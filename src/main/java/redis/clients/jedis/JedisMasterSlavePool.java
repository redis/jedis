package redis.clients.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisException;

public class JedisMasterSlavePool implements Closeable {
  // extends JedisPoolAbstract
  private static Logger log = Logger.getLogger(JedisMasterSlavePool.class.getSimpleName());
  private static final int MAX_RETRIES_GETTING_RESOURCE = 5;

  protected GenericObjectPoolConfig poolConfig;
  protected GenericObjectPoolConfig poolConfigSlaves;
  protected int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  protected int soTimeout = Protocol.DEFAULT_TIMEOUT;
  protected String password;
  protected int database = Protocol.DEFAULT_DATABASE;
  protected String clientName;
  protected MasterListener masterListeners = null;

  private volatile JedisPool masterPool;
  private volatile List<JedisPool> slavesPools = new ArrayList<JedisPool>();

  private volatile JedisFactory factory;
  private volatile HostAndPort currentHostMaster;

  private static final String ROLE_KEY = "role:";

  protected boolean useSlaves = false;
  protected boolean singleNode = false;

  private enum Role {
    master, slave
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig) {
    this(nodes, poolConfig, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisMasterSlavePool(String[] nodes) {
    this(nodes, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisMasterSlavePool(String[] nodes, String password) {
    this(nodes, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig, int timeout,
      final String password) {
    this(nodes, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final int timeout) {
    this(nodes, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final String password) {
    this(nodes, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig, int timeout,
      final String password, final int database) {
    this(nodes, poolConfig, timeout, timeout, password, database);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig, int timeout,
      final String password, final int database, final String clientName) {
    this(nodes, poolConfig, poolConfig, timeout, timeout, password, database, clientName);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final int timeout,
      final int soTimeout, final String password, final int database) {
    this(nodes, poolConfig, poolConfig, timeout, soTimeout, password, database, null);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig,
      final GenericObjectPoolConfig poolConfigSlaves, final int timeout, final int soTimeout, final String password,
      final int database) {
    this(nodes, poolConfig, poolConfigSlaves, timeout, soTimeout, password, database, null);
  }

  public JedisMasterSlavePool(String[] nodes, final GenericObjectPoolConfig poolConfig,
      final GenericObjectPoolConfig poolConfigSlaves, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName) {
    this.poolConfig = poolConfig;
    this.poolConfigSlaves = poolConfigSlaves;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.database = database;
    this.clientName = clientName;

    initElasticache(nodes);
  }

  @Override
  public void close() throws IOException {
    if (masterListeners != null)
      masterListeners.shutdown();
    masterPool.destroy();
    for (JedisPool slave : slavesPools) {
      slave.destroy();
    }
  }

  public HostAndPort getCurrentHostMaster() {
    return currentHostMaster;
  }

  private void initPool(HostAndPort master, List<HostAndPort> slaves) {
    currentHostMaster = master;
    if (factory == null) {
      if (masterPool != null)
        masterPool.close();
      masterPool = new JedisPool(poolConfig, master.getHost(), master.getPort(), connectionTimeout, soTimeout,
          password, database, clientName, false, null, null, null);
      for (JedisPool slavePool : slavesPools) {
        slavePool.close();
      }
      slavesPools.clear();
      for (HostAndPort slave : slaves) {
        slavesPools.add(new JedisPool(poolConfig, slave.getHost(), slave.getPort(), connectionTimeout, soTimeout,
            password, database, clientName, false, null, null, null));
      }
    }
    log.fine("Created JedisPool to master at " + master);
  }

  private void initElasticache(String[] nodes) {
    log.fine("Trying to find master from available nodes...");
    HostAndPort master = null;
    List<HostAndPort> slaves = new ArrayList<HostAndPort>();
    if (nodes.length == 1) {
      singleNode = true;
      master = toHostAndPort(Arrays.asList(nodes[0].split(":")));
    } else {
      for (String node : nodes) {
        final HostAndPort hap = toHostAndPort(Arrays.asList(node.split(":")));
        log.fine("Connecting to Redis " + hap);
        Jedis jedis = null;
        try {
          jedis = new Jedis(hap.getHost(), hap.getPort());
          if (master == null) {
            Role role = determineRole(jedis.info("replication"));
            if (Role.master.equals(role)) {
              log.fine("Found Redis master at " + hap.toString());
              master = hap;
            }
          } else {
            useSlaves = true;
            slaves.add(hap);
          }
        } catch (JedisException e) {
          log.warning("Cannot connect to elasticache node running @ " + node + ". Reason: " + e + ". Trying next one.");
          if (jedis != null)
            jedis.close();
        }
      }
    }
    initPool(master, slaves);
    if (currentHostMaster == null) {
      throw new JedisException("Master not found in the list of provided nodes");
    }
    log.fine("Redis master running at " + currentHostMaster + ", starting Master listeners...");
    if (!singleNode) {
      Set<HostAndPort> nodesHP = new HashSet<HostAndPort>();
      for (String node : nodes) {
        nodesHP.add(toHostAndPort(Arrays.asList(node.split(":"))));
      }
      masterListeners = new MasterListener(nodesHP);
      masterListeners.run();
    }
  }

  private Role determineRole(String data) {
    for (String s : data.split("\\r\\n")) {
      if (s.startsWith(ROLE_KEY)) {
        return Role.valueOf(s.substring(ROLE_KEY.length()));
      }
    }
    throw new JedisException("Cannot determine node role from provided 'INFO replication' data" + data);
  }

  private HostAndPort toHostAndPort(List<String> node) {
    String host = node.get(0);
    int port = Integer.parseInt(node.get(1));

    return new HostAndPort(host, port);
  }

  public Jedis getResource() {
    int retries = 0;
    while (retries++ < MAX_RETRIES_GETTING_RESOURCE) {
      Jedis jedis = masterPool.getResource();
      jedis.setDataSource(masterPool);

      // get a reference because it can change concurrently
      final HostAndPort master = currentHostMaster;
      final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient().getPort());

      if (master.equals(connection)) {
        // connected to the correct master
        return jedis;
      }
    }
    throw new JedisException("Failed to get a resource from the master");
  }

  /**
   * Return a slave redis, may return the master if called during the election of a new master
   * 
   * @return
   */
  public Jedis getReadOnlyResource() {
    if (useSlaves) {
      int retries = 0;
      while (retries++ < MAX_RETRIES_GETTING_RESOURCE) {
        // RoundRobin to get Slave resource
        JedisPool slaveRoudRobin = getSlaveRoudRobin(slavesPools);
        Jedis jedis = slaveRoudRobin.getResource();
        jedis.setDataSource(slaveRoudRobin);

        // get a reference because it can change concurrently
        final HostAndPort master = currentHostMaster;
        final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient().getPort());

        if (!master.equals(connection)) {
          // connected to the correct master
          return jedis;
        }
      }
      throw new JedisException("Failed to get a resource from the slaves");
    } else {
      return getResource();
    }
  }

  private final AtomicInteger index = new AtomicInteger(-1);

  public JedisPool getSlaveRoudRobin(List<JedisPool> slaves) {
    int ind = Math.abs(index.incrementAndGet() % slaves.size());
    return slaves.get(ind);
  }

  protected class MasterListener extends Thread {

    protected String masterName = "";
    protected Set<HostAndPort> nodes;
    protected long waitBetweenScan = 1000;
    protected volatile Jedis j;
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected MasterListener() {
    }

    public MasterListener(Set<HostAndPort> nodes) {
      this.nodes = nodes;
    }

    public MasterListener(Set<HostAndPort> nodes, long subscribeRetryWaitTimeMillis) {
      this(nodes);
      this.waitBetweenScan = subscribeRetryWaitTimeMillis;
    }

    public void run() {
      running.set(true);
      while (running.get()) {
        HostAndPort master = null;
        List<HostAndPort> slavesPools = new ArrayList<HostAndPort>();
        log.fine("Scanning for master");
        for (HostAndPort node : nodes) {
          log.finer("Connecting to Redis " + node);
          Jedis jedis = null;
          try {
            jedis = new Jedis(node.getHost(), node.getPort());
            Role role = determineRole(jedis.info("replication"));
            if (Role.master.equals(role)) {
              log.fine("Found Redis master at " + node);
              if (masterName.equals(node)) {
                break;
              } else {
                master = node;
                masterName = node.toString();
              }
            } else {
              slavesPools.add(node);
            }
          } catch (JedisException e) {
            log.warning("Cannot connect to elasticache node running @ " + node + ". Reason: " + e
                + ". Trying next one.");
            if (jedis != null)
              jedis.close();
          }
        }
        // A new master was found
        if (master != null)
          initPool(master, slavesPools);
        try {
          Thread.sleep(waitBetweenScan);
        } catch (InterruptedException e1) {
          log.log(Level.SEVERE, "Sleep interrupted: ", e1);
        }

      }
    }

    public void shutdown() {
      try {
        log.fine("Shutting down elasticache master listener thread");
        running.set(false);
      } catch (Exception e) {
        log.log(Level.SEVERE, "Caught exception while shutting down: ", e);
      }
    }
  }

}