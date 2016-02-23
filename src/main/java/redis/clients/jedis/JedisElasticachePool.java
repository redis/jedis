package redis.clients.jedis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisException;

public class JedisElasticachePool extends JedisPoolAbstract {

  protected GenericObjectPoolConfig poolConfig;

  protected int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  protected int soTimeout = Protocol.DEFAULT_TIMEOUT;

  protected String password;

  protected int database = Protocol.DEFAULT_DATABASE;

  protected String clientName;

  protected MasterListener masterListeners = null;

  private static Logger log = Logger.getLogger(JedisElasticachePool.class.getSimpleName());

  private volatile JedisFactory factory;
  private volatile HostAndPort currentHostMaster;

  private static final String ROLE_KEY = "role:";

  private enum Role {
    master, slave
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig) {
    this(nodes, poolConfig, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisElasticachePool(String[] nodes) {
    this(nodes, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisElasticachePool(String[] nodes, String password) {
    this(nodes, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, int timeout,
      final String password) {
    this(nodes, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final int timeout) {
    this(nodes, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final String password) {
    this(nodes, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, int timeout,
      final String password, final int database) {
    this(nodes, poolConfig, timeout, timeout, password, database);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, int timeout,
      final String password, final int database, final String clientName) {
    this(nodes, poolConfig, timeout, timeout, password, database, clientName);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final int timeout,
      final int soTimeout, final String password, final int database) {
    this(nodes, poolConfig, timeout, soTimeout, password, database, null);
  }

  public JedisElasticachePool(String[] nodes, final GenericObjectPoolConfig poolConfig, final int connectionTimeout,
      final int soTimeout, final String password, final int database, final String clientName) {
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.database = database;
    this.clientName = clientName;

    initElasticache(nodes);
  }

  public void destroy() {
    masterListeners.shutdown();
    super.destroy();
  }

  public HostAndPort getCurrentHostMaster() {
    return currentHostMaster;
  }

  private void initPool(HostAndPort master) {
    if (!master.equals(currentHostMaster)) {
      currentHostMaster = master;
      if (factory == null) {
        factory = new JedisFactory(master.getHost(), master.getPort(), connectionTimeout, soTimeout, password,
            database, clientName, false, null, null, null);
        initPool(poolConfig, factory);
      } else {
        factory.setHostAndPort(currentHostMaster);
        // although we clear the pool, we still have to check the
        // returned object
        // in getResource, this call only clears idle instances, not
        // borrowed instances
        internalPool.clear();
      }

      log.info("Created JedisPool to master at " + master);
    }
  }

  private void initElasticache(String[] nodes) {

    log.info("Trying to find master from available nodes...");

    for (String node : nodes) {
      final HostAndPort hap = toHostAndPort(Arrays.asList(node.split(":")));
      log.fine("Connecting to Redis " + hap);
      try (Jedis jedis = new Jedis(hap.getHost(), hap.getPort())) {
        Role role = determineRole(jedis.info("replication"));
        if (Role.master.equals(role)) {
          log.fine("Found Redis master at " + hap.toString());
          initPool(hap);
          break;
        }
      } catch (JedisException e) {
        log.warning("Cannot connect to elasticache node running @ " + node + ". Reason: " + e + ". Trying next one.");
      }
    }
    if (currentHostMaster == null) {
      throw new JedisException("Master not found in the list of provided nodes");
    }
    log.info("Redis master running at " + currentHostMaster + ", starting Master listeners...");
    Set<HostAndPort> nodesHP = Arrays.asList(nodes).stream().map(node -> toHostAndPort(Arrays.asList(node.split(":"))))
        .collect(Collectors.toSet());
    masterListeners = new MasterListener(nodesHP);
    masterListeners.run();
  }

  private Role determineRole(String data) {
    for (String s : data.split("\\r\\n")) {
      if (s.startsWith(ROLE_KEY)) {
        return Role.valueOf(s.substring(ROLE_KEY.length()));
      }
    }
    throw new JedisException("Cannot determine node role from provided 'INFO replication' data" + data);
  }

  private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
    String host = getMasterAddrByNameResult.get(0);
    int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

    return new HostAndPort(host, port);
  }

  @Override
  public Jedis getResource() {
    while (true) {
      Jedis jedis = super.getResource();
      jedis.setDataSource(this);

      // get a reference because it can change concurrently
      final HostAndPort master = currentHostMaster;
      final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient().getPort());

      if (master.equals(connection)) {
        // connected to the correct master
        return jedis;
      } else {
        returnBrokenResource(jedis);
      }
    }
  }
  
  /**
   * Return a slave redis, may return the master if called during the election of a new master
   * @return 
   */
  public Jedis getReadResource() {
    return null;
//    while (true) {
//      Jedis jedis = getNextSlavePool().getResource();
//      jedis.setDataSource(this);
//      
//      // get a reference because it can change concurrently
//      final HostAndPort master = currentHostMaster;
//      final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient().getPort());
//      
//      if (!master.equals(connection)) {
//        // connected to the correct master
//        return jedis;
//      } else {
//        returnBrokenResource(jedis);
//      }
//    }
  }

  protected void returnBrokenResource(final Jedis resource) {
    if (resource != null) {
      returnBrokenResourceObject(resource);
    }
  }

  protected void returnResource(final Jedis resource) {
    if (resource != null) {
      resource.resetState();
      returnResourceObject(resource);
    }
  }

  protected class MasterListener extends Thread {

    protected String masterName = "";
    protected Set<HostAndPort> nodes;
    protected long subscribeRetryWaitTimeMillis = 1000;
    protected volatile Jedis j;
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected MasterListener() {
    }

    public MasterListener(Set<HostAndPort> nodes) {
      this.nodes = nodes;
    }

    public MasterListener(Set<HostAndPort> nodes, long subscribeRetryWaitTimeMillis) {
      this(nodes);
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }

    public void run() {
      running.set(true);
      while (running.get()) {
        log.info("Scanning for master");
        for (HostAndPort node : nodes) {
          log.info("Connecting to Redis " + node);
          try (Jedis jedis = new Jedis(node.getHost(), node.getPort())) {
            Role role = determineRole(jedis.info("replication"));
            if (Role.master.equals(role)) {
              log.info("Found Redis master at " + node);
              if (masterName.equals(node)) {
                break;
              } else {
                initPool(node);
                masterName = node.toString();
                //FIXME
//                initSlavePools(nodes);
                break;
              }
            }
          } catch (JedisException e) {
            log.warning("Cannot connect to elasticache node running @ " + node + ". Reason: " + e
                + ". Trying next one.");
          }
        }
        try {
          Thread.sleep(subscribeRetryWaitTimeMillis);
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