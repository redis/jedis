package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


class PoolInfo {
  public String host;
  public JedisPool pool;

  public PoolInfo(String host, JedisPool pool) {
    this.host = host;
    this.pool = pool;
  }
}

public class JedisSentinelSlavePool implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(JedisSentinelSlavePool.class);

  private final JedisClientConfig sentinelClientConfig;

  private final JedisClientConfig clientConfig;

  private final GenericObjectPoolConfig<Jedis> poolConfig;

  protected final Collection<SlaveListener> slaveListeners = new ArrayList<>();

  private final List<PoolInfo> slavePools = new ArrayList<>();

  private int poolIndex;

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels, String password) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels, String password, String sentinelPassword) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT,
        password, Protocol.DEFAULT_DATABASE, null, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, sentinelPassword, null);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password) {
    this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int timeout) {
    this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final String password) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password,
                                final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, null, password, database);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String user,
                                final String password, final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, user, password, database);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password,
                                final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, clientName);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String user,
                                final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, user, password, database, clientName);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
                                final String password, final int database) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, null);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
                                final String user, final String password, final int database) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, user, password, database, null);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
                                final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, clientName);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
                                final String user, final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, user, password, database, clientName,
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null, null, null);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
                                final String user, final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName,
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null, null, null);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
                                final String password, final int database, final String clientName,
                                final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelPassword,
                                final String sentinelClientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, clientName,
        sentinelConnectionTimeout, sentinelSoTimeout, null, sentinelPassword, sentinelClientName);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
                                final String user, final String password, final int database, final String clientName,
                                final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelUser,
                                final String sentinelPassword, final String sentinelClientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, 0, user, password, database, clientName,
        sentinelConnectionTimeout, sentinelSoTimeout, sentinelUser, sentinelPassword, sentinelClientName);
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig,
                                final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
                                final String user, final String password, final int database, final String clientName,
                                final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelUser,
                                final String sentinelPassword, final String sentinelClientName) {
    this(masterName, parseHostAndPorts(sentinels), poolConfig,
        DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
            .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout)
            .user(user).password(password).database(database).clientName(clientName).build(),
        DefaultJedisClientConfig.builder().connectionTimeoutMillis(sentinelConnectionTimeout)
            .socketTimeoutMillis(sentinelSoTimeout).user(sentinelUser).password(sentinelPassword)
            .clientName(sentinelClientName).build()
    );
  }

  public JedisSentinelSlavePool(String masterName, Set<HostAndPort> sentinels,
                                final GenericObjectPoolConfig<Jedis> poolConfig, final JedisClientConfig clientConfig,
                                final JedisClientConfig sentinelClientConfig) {
    this.poolConfig = poolConfig;
    this.clientConfig = clientConfig;
    this.sentinelClientConfig = sentinelClientConfig;

    initSentinels(sentinels, masterName);
  }

  private static Set<HostAndPort> parseHostAndPorts(Set<String> strings) {
    return strings.stream().map(HostAndPort::from).collect(Collectors.toSet());
  }

  public void destroy() {
    for (SlaveListener m : slaveListeners) {
      m.shutdown();
    }

    for (PoolInfo poolInfo : slavePools) {
      poolInfo.pool.destroy();
    }
  }

  private List<HostAndPort> initSentinels(Set<HostAndPort> sentinels, final String masterName) {

    boolean sentinelAvailable = false;

    LOG.info("Trying to find master from available Sentinels...");

    List<HostAndPort> slaves = new ArrayList<>();

    for (HostAndPort sentinel : sentinels) {

      LOG.debug("Connecting to Sentinel {}", sentinel);

      try (Jedis jedis = new Jedis(sentinel, sentinelClientConfig)) {
        List<Map<String, String>> slaveInfos = jedis.sentinelSlaves(masterName);

        // connected to sentinel...
        sentinelAvailable = true;

        if (slaveInfos == null || slaveInfos.isEmpty()) {
          LOG.warn("Can not get slave addr, master name: {}. Sentinel: {}", masterName, sentinel);
          continue;
        }

        slaveInfos.forEach(slaveInfo -> {
          String ip = slaveInfo.get("ip");
          int port = Integer.parseInt(slaveInfo.get("port"));
          HostAndPort slave = new HostAndPort(ip, port);
          addSlave(slave);
          slaves.add(slave);
        });

        LOG.debug("Found Redis slaves at {}", slaveInfos);
        break;
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        LOG.warn(
          "Cannot get master address from sentinel running @ {}. Reason: {}. Trying next one.", sentinel, e);
      }
    }

    if (slaves.isEmpty()) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not monitored
        throw new JedisException("Can connect to sentinel, but " + masterName
            + " seems to be not monitored...");
      } else {
        throw new JedisConnectionException("All sentinels down, cannot determine where is "
            + masterName + " master is running...");
      }
    }

    LOG.info("Redis slaves running at {}, starting Sentinel listeners...", slaves);

    for (HostAndPort sentinel : sentinels) {
      SlaveListener slaveListener = new SlaveListener(masterName, sentinel.getHost(), sentinel.getPort());
      // whether SlaveListener threads are alive or not, process can be stopped
      slaveListener.setDaemon(true);
      slaveListeners.add(slaveListener);
      slaveListener.start();
    }

    return slaves;
  }

  private void addSlave(HostAndPort slave) {
    String newSlaveHost = slave.toString();
    synchronized (this.slavePools) {
      for (int i = 0; i < this.slavePools.size(); i++) {
        PoolInfo poolInfo = this.slavePools.get(i);
        if (poolInfo.host.equals(newSlaveHost)) {
          return;
        }
      }
      slavePools.add(new PoolInfo(newSlaveHost, new JedisPool(this.poolConfig, slave, this.clientConfig)));
    }
  }

  private void removeSlave(HostAndPort slave) {
    String newSlaveHost = slave.toString();
    synchronized (this.slavePools) {
      for (int i = 0; i < this.slavePools.size(); i++) {
        PoolInfo poolInfo = this.slavePools.get(i);
        if (poolInfo.host.equals(newSlaveHost)) {
          PoolInfo removed = slavePools.remove(i);
          removed.pool.destroy();
          return;
        }
      }
    }
  }

    public Jedis getResource() {
        int startIdx;
        synchronized (slavePools) {
            poolIndex++;
            if (poolIndex >= slavePools.size()) {
                poolIndex = 0;
            }
            startIdx = poolIndex;
        }
        return _getResource(startIdx, 0);
    }

    public Jedis _getResource(int idx, int cnt) {
        PoolInfo poolInfo;
        synchronized (slavePools) {
            if (cnt >= slavePools.size()) {
                throw new RuntimeException("can not get Jedis Object, all slave is invalid");
            }
            poolInfo = slavePools.get(idx % slavePools.size());
        }
        try {
            Jedis jedis = poolInfo.pool.getResource();
            return jedis;
        } catch (Exception e) {
            LOG.error("get connection fail:", e);
            return _getResource(idx+1, cnt + 1);
        }
    }

  @Override
  public void close() {
    destroy();
  }

  protected class SlaveListener extends Thread {

    protected String masterName;
    protected String host;
    protected int port;
    protected long subscribeRetryWaitTimeMillis = 5000;
    protected volatile Jedis j;
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected SlaveListener() {
    }

    public SlaveListener(String masterName, String host, int port) {
      super(String.format("MasterListener-%s-[%s:%d]", masterName, host, port));
      this.masterName = masterName;
      this.host = host;
      this.port = port;
    }

    public SlaveListener(String masterName, String host, int port,
                         long subscribeRetryWaitTimeMillis) {
      this(masterName, host, port);
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }

    @Override
    public void run() {

      running.set(true);

      while (running.get()) {

        try {
          // double check that it is not being shutdown
          if (!running.get()) {
            break;
          }

          final HostAndPort hostPort = new HostAndPort(host, port);
          j = new Jedis(hostPort, sentinelClientConfig);

          j.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              LOG.info("Sentinel: {}, channel: {}, published: {}.", hostPort, channel, message);
              String[] switchMasterMsg = message.split(" ");
              String slaveIp;
              int slavePort;
              switch (channel) {
                case "+sdown":
                  if (switchMasterMsg[0].equals("master")) {
                    return;
                  }
                  if (!masterName.equals(switchMasterMsg[5])) {
                    return;
                  }
                  slaveIp = switchMasterMsg[2];
                  slavePort = Integer.parseInt(switchMasterMsg[3]);
                  removeSlave(new HostAndPort(slaveIp, slavePort));
                  break;
                case "-sdown":
                  if (!masterName.equals(switchMasterMsg[5])) {
                    return;
                  }
                  slaveIp = switchMasterMsg[2];
                  slavePort = Integer.parseInt(switchMasterMsg[3]);
                  addSlave(new HostAndPort(slaveIp, slavePort));
                  break;
                case "+slave":
                  if (!masterName.equals(switchMasterMsg[5])) {
                    return;
                  }
                  slaveIp = switchMasterMsg[2];
                  slavePort = Integer.parseInt(switchMasterMsg[3]);
                  addSlave(new HostAndPort(slaveIp, slavePort));

                  String masterIp = switchMasterMsg[6];
                  int masterPort = Integer.parseInt(switchMasterMsg[7]);
                  removeSlave(new HostAndPort(masterIp, masterPort));
                  break;
              }
            }
          }, "+sdown", "-sdown", "+slave", "-slave");

        } catch (JedisException e) {

          if (running.get()) {
            LOG.error("Lost connection to Sentinel at {}:{}. Sleeping 5000ms and retrying.", host,
              port, e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException e1) {
              LOG.error("Sleep interrupted: ", e1);
            }
          } else {
            LOG.debug("Unsubscribing from Sentinel at {}:{}", host, port);
          }
        } finally {
          if (j != null) {
            j.close();
          }
        }
      }
    }

    public void shutdown() {
      try {
        LOG.debug("Shutting down listener on {}:{}", host, port);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (j != null) {
          j.close();
        }
      } catch (RuntimeException e) {
        LOG.error("Caught exception while shutting down: ", e);
      }
    }
  }
}
