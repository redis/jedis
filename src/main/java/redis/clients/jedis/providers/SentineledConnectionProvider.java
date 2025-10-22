package redis.clients.jedis.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.ReadFrom;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.ReadOnlyCommands;
import redis.clients.jedis.util.Pool;

public class SentineledConnectionProvider implements ConnectionProvider {
  class PoolInfo {
    public String host;
    public ConnectionPool pool;

    public PoolInfo(String host, ConnectionPool pool) {
      this.host = host;
      this.pool = pool;
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(SentineledConnectionProvider.class);

  protected static final long DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS = 5000;

  private volatile HostAndPort currentMaster;

  private volatile ConnectionPool pool;

  private final String masterName;

  private final JedisClientConfig masterClientConfig;

  private final Cache clientSideCache;

  private final GenericObjectPoolConfig<Connection> masterPoolConfig;

  protected final Collection<SentinelListener> sentinelListeners = new ArrayList<>();

  private final JedisClientConfig sentinelClientConfig;

  private final long subscribeRetryWaitTimeMillis;

  private final ReadFrom readFrom;

  private ReadOnlyCommands.ReadOnlyPredicate READ_ONLY_COMMANDS;

  private final Lock initPoolLock = new ReentrantLock(true);

  private final List<PoolInfo> slavePools = new ArrayList<>();

  private final Lock slavePoolsLock = new ReentrantLock(true);

  private int poolIndex;

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, null, null, sentinels, sentinelClientConfig);
  }

  @Experimental
  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Cache clientSideCache, Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, clientSideCache, null, sentinels, sentinelClientConfig);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig,
        DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
                                      final GenericObjectPoolConfig<Connection> poolConfig,
                                      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig, ReadFrom readFrom) {
    this(masterName, masterClientConfig, null, poolConfig, sentinels, sentinelClientConfig,
            DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS, readFrom, ReadOnlyCommands.asPredicate());
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
                                      final GenericObjectPoolConfig<Connection> poolConfig,
                                      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig, ReadFrom readFrom,
                                      ReadOnlyCommands.ReadOnlyPredicate readOnlyPredicate) {
    this(masterName, masterClientConfig, null, poolConfig, sentinels, sentinelClientConfig,
            DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS, readFrom, readOnlyPredicate);
  }

  public SentineledConnectionProvider(String masterName, JedisClientConfig clientConfig, Cache cache, GenericObjectPoolConfig<Connection> poolConfig, Set<HostAndPort> sentinels, JedisClientConfig sentinelClientConfig, ReadFrom readFrom, ReadOnlyCommands.ReadOnlyPredicate readOnlyPredicate) {
    this(masterName, clientConfig, cache, poolConfig, sentinels, sentinelClientConfig,
            DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS, readFrom, readOnlyPredicate);
  }

  @Experimental
  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Cache clientSideCache, final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, clientSideCache, poolConfig, sentinels, sentinelClientConfig,
        DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS, ReadFrom.UPSTREAM, ReadOnlyCommands.asPredicate());
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig,
      final long subscribeRetryWaitTimeMillis) {
    this(masterName, masterClientConfig, null, poolConfig, sentinels, sentinelClientConfig, subscribeRetryWaitTimeMillis, ReadFrom.UPSTREAM, ReadOnlyCommands.asPredicate());
  }

  @Experimental
  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Cache clientSideCache, final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig,
      final long subscribeRetryWaitTimeMillis, ReadFrom readFrom, ReadOnlyCommands.ReadOnlyPredicate readOnlyPredicate) {

    this.masterName = masterName;
    this.masterClientConfig = masterClientConfig;
    this.clientSideCache = clientSideCache;
    this.masterPoolConfig = poolConfig;

    this.sentinelClientConfig = sentinelClientConfig;
    this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    this.readFrom = readFrom;
    this.READ_ONLY_COMMANDS = readOnlyPredicate;

    HostAndPort master = initSentinels(sentinels);
    initMaster(master);
  }

  private Connection getSlaveResource() {
    int startIdx;
    slavePoolsLock.lock();
    try {
      poolIndex++;
      if (poolIndex >= slavePools.size()) {
        poolIndex = 0;
      }
      startIdx = poolIndex;
    } finally {
      slavePoolsLock.unlock();
    }
    return _getSlaveResource(startIdx, 0);
  }

  private Connection _getSlaveResource(int idx, int cnt) {
    PoolInfo poolInfo;
    slavePoolsLock.lock();
    try {
      if (cnt >= slavePools.size()) {
        return null;
      }
      poolInfo = slavePools.get(idx % slavePools.size());
    } finally {
      slavePoolsLock.unlock();
    }

    try {
      Connection jedis = poolInfo.pool.getResource();
      return jedis;
    } catch (Exception e) {
      LOG.error("get connection fail:", e);
      return _getSlaveResource(idx + 1, cnt + 1);
    }
  }

  @Override
  public Connection getConnection() {
    return pool.getResource();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    boolean isReadCommand = READ_ONLY_COMMANDS.isReadOnly(args);
    if (!isReadCommand) {
      return pool.getResource();
    }

    Connection conn;
    switch (readFrom) {
      case REPLICA:
        conn = getSlaveResource();
        if (conn == null) {
          throw new JedisException("all replica is invalid");
        }
        return conn;
      case UPSTREAM_PREFERRED:
        try {
          conn = pool.getResource();
          if (conn != null) {
            return conn;
          }
        } catch (Exception e) {
          LOG.error("get master connection error", e);
        }

        conn = getSlaveResource();
        if (conn == null) {
          throw new JedisException("all redis instance is invalid");
        }
        return conn;
      case REPLICA_PREFERRED:
        conn = getSlaveResource();
        if (conn != null) {
          return conn;
        }
        return pool.getResource();
      default:
        return pool.getResource();
    }
  }

  @Override
  public Map<?, Pool<Connection>> getConnectionMap() {
    return Collections.singletonMap(currentMaster, pool);
  }

  @Override
  public Map<?, Pool<Connection>> getPrimaryNodesConnectionMap() {
    return Collections.singletonMap(currentMaster, pool);
  }

  @Override
  public void close() {
    sentinelListeners.forEach(SentinelListener::shutdown);

    pool.close();

    for (PoolInfo slavePool : slavePools) {
      slavePool.pool.close();
    }
  }

  public HostAndPort getCurrentMaster() {
    return currentMaster;
  }

  private void initMaster(HostAndPort master) {
    initPoolLock.lock();

    try {
      if (!master.equals(currentMaster)) {
        currentMaster = master;

        ConnectionPool newPool = createNodePool(currentMaster);

        ConnectionPool existingPool = pool;
        pool = newPool;
        LOG.info("Created connection pool to master at {}.", master);
        if (clientSideCache != null) {
          clientSideCache.flush();
        }

        if (existingPool != null) {
          // although we clear the pool, we still have to check the returned object in getResource,
          // this call only clears idle instances, not borrowed instances
          // existingPool.clear(); // necessary??
          existingPool.close();
        }
      }
    } finally {
      initPoolLock.unlock();
    }
  }

  private ConnectionPool createNodePool(HostAndPort master) {
    if (masterPoolConfig == null) {
      if (clientSideCache == null) {
        return new ConnectionPool(master, masterClientConfig);
      } else {
        return new ConnectionPool(master, masterClientConfig, clientSideCache);
      }
    } else {
      if (clientSideCache == null) {
        return new ConnectionPool(master, masterClientConfig, masterPoolConfig);
      } else {
        return new ConnectionPool(master, masterClientConfig, clientSideCache, masterPoolConfig);
      }
    }
  }

  private void initSlaves(List<HostAndPort> slaves) {
    List<PoolInfo> removedSlavePools = new ArrayList<>();
    slavePoolsLock.lock();
    try {
      for (int i = slavePools.size()-1; i >= 0; i--) {
        PoolInfo poolInfo = slavePools.get(i);
        boolean found = false;
        for (HostAndPort slave : slaves) {
          String host = slave.toString();
          if (poolInfo.host.equals(host)) {
            found = true;
            break;
          }
        }
        if (!found) {
          removedSlavePools.add(slavePools.remove(i));
        }
      }

      for (HostAndPort slave : slaves) {
        addSlave(slave);
      }
    } finally {
      slavePoolsLock.unlock();
      if (!removedSlavePools.isEmpty() && clientSideCache != null) {
        clientSideCache.flush();
      }

      for (PoolInfo removedSlavePool : removedSlavePools) {
        removedSlavePool.pool.destroy();
      }
    }
  }

  private static boolean isHealthy(String flags) {
    for (String flag : flags.split(",")) {
      switch (flag.trim()) {
        case "s_down":
        case "o_down":
        case "disconnected":
          return false;
      }
    }
    return true;
  }

  private void addSlave(HostAndPort slave) {
    String newSlaveHost = slave.toString();
    slavePoolsLock.lock();
    try {
      for (int i = 0; i < this.slavePools.size(); i++) {
        PoolInfo poolInfo = this.slavePools.get(i);
        if (poolInfo.host.equals(newSlaveHost)) {
          return;
        }
      }
      slavePools.add(new PoolInfo(newSlaveHost, createNodePool(slave)));
    } finally {
      slavePoolsLock.unlock();
    }
  }

  private void removeSlave(HostAndPort slave) {
    String newSlaveHost = slave.toString();
    PoolInfo removed = null;
    slavePoolsLock.lock();
    try {
      for (int i = 0; i < this.slavePools.size(); i++) {
        PoolInfo poolInfo = this.slavePools.get(i);
        if (poolInfo.host.equals(newSlaveHost)) {
          removed = slavePools.remove(i);
          break;
        }
      }
    } finally {
      slavePoolsLock.unlock();
    }
    if (removed != null) {
      removed.pool.destroy();
    }
  }

  private HostAndPort initSentinels(Set<HostAndPort> sentinels) {

    HostAndPort master = null;
    boolean sentinelAvailable = false;

    LOG.debug("Trying to find master from available sentinels...");

    for (HostAndPort sentinel : sentinels) {

      LOG.debug("Connecting to Sentinel {}...", sentinel);

      try (Jedis jedis = new Jedis(sentinel, sentinelClientConfig)) {

        List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);

        // connected to sentinel...
        sentinelAvailable = true;

        if (masterAddr == null || masterAddr.size() != 2) {
          LOG.warn("Sentinel {} is not monitoring master {}.", sentinel, masterName);
          continue;
        }

        master = toHostAndPort(masterAddr);
        LOG.debug("Redis master reported at {}.", master);
        break;
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        LOG.warn("Could not get master address from {}.", sentinel, e);
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not monitored
        throw new JedisException(
            "Can connect to sentinel, but " + masterName + " seems to be not monitored.");
      } else {
        throw new JedisConnectionException(
            "All sentinels down, cannot determine where " + masterName + " is running.");
      }
    }

    LOG.info("Redis master running at {}. Starting sentinel listeners...", master);

    for (HostAndPort sentinel : sentinels) {

      SentinelListener listener = new SentinelListener(sentinel);
      // whether SentinelListener threads are alive or not, process can be stopped
      listener.setDaemon(true);
      sentinelListeners.add(listener);
      listener.start();
    }

    return master;
  }

  /**
   * Must be of size 2.
   */
  private static HostAndPort toHostAndPort(List<String> masterAddr) {
    return toHostAndPort(masterAddr.get(0), masterAddr.get(1));
  }

  private static HostAndPort toHostAndPort(String hostStr, String portStr) {
    return new HostAndPort(hostStr, Integer.parseInt(portStr));
  }

  protected class SentinelListener extends Thread {

    protected final HostAndPort node;
    protected volatile Jedis sentinelJedis;
    protected AtomicBoolean running = new AtomicBoolean(false);

    public SentinelListener(HostAndPort node) {
      super(String.format("%s-SentinelListener-[%s]", masterName, node.toString()));
      this.node = node;
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

          sentinelJedis = new Jedis(node, sentinelClientConfig);

          List<Map<String, String>> slaveInfos = sentinelJedis.sentinelSlaves(masterName);

          List<HostAndPort> slaves = new ArrayList<>();

          for (int i = 0; i < slaveInfos.size(); i++) {
            Map<String, String> slaveInfo = slaveInfos.get(i);
            String flags = slaveInfo.get("flags");
            if (flags == null || !isHealthy(flags)) {
              continue;
            }
            String ip = slaveInfo.get("ip");
            int port = Integer.parseInt(slaveInfo.get("port"));
            HostAndPort slave = new HostAndPort(ip, port);
            slaves.add(slave);
          }

          initSlaves(slaves);

          // code for active refresh
          List<String> masterAddr = sentinelJedis.sentinelGetMasterAddrByName(masterName);
          if (masterAddr == null || masterAddr.size() != 2) {
            LOG.warn("Can not get master {} address. Sentinel: {}.", masterName, node);
          } else {
            initMaster(toHostAndPort(masterAddr));
          }

          sentinelJedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              LOG.debug("Sentinel {} with channel {} published: {}.", node, channel, message);

              String[] switchMsg = message.split(" ");
              String slaveIp;
              int slavePort;
              switch (channel) {
                case "+switch-master":
                  if (switchMsg.length > 3) {
                    if (masterName.equals(switchMsg[0])) {
                      initMaster(toHostAndPort(switchMsg[3], switchMsg[4]));
                    } else {
                      LOG.debug(
                              "Ignoring message on +switch-master for master {}. Our master is {}.",
                              switchMsg[0], masterName);
                    }
                  } else {
                    LOG.error("Invalid message received on sentinel {} on channel +switch-master: {}.",
                            node, message);
                  }
                  break;
                case "+sdown":
                  if (switchMsg.length < 6) {
                    return;
                  }
                  if (switchMsg[0].equals("master")) {
                    return;
                  }
                  if (!masterName.equals(switchMsg[5])) {
                    return;
                  }
                  slaveIp = switchMsg[2];
                  slavePort = Integer.parseInt(switchMsg[3]);
                  removeSlave(new HostAndPort(slaveIp, slavePort));
                  break;
                case "-sdown":
                  if (switchMsg.length < 6) {
                    return;
                  }
                  if (!masterName.equals(switchMsg[5])) {
                    return;
                  }
                  slaveIp = switchMsg[2];
                  slavePort = Integer.parseInt(switchMsg[3]);
                  addSlave(new HostAndPort(slaveIp, slavePort));
                  break;
                case "+slave":
                  if (switchMsg.length < 8) {
                    return;
                  }
                  if (!masterName.equals(switchMsg[5])) {
                    return;
                  }
                  slaveIp = switchMsg[2];
                  slavePort = Integer.parseInt(switchMsg[3]);
                  addSlave(new HostAndPort(slaveIp, slavePort));

                  String masterIp = switchMsg[6];
                  int masterPort = Integer.parseInt(switchMsg[7]);
                  removeSlave(new HostAndPort(masterIp, masterPort));
                  break;
              }
            }
          }, "+switch-master", "+sdown", "-sdown", "+slave");

        } catch (JedisException e) {

          if (running.get()) {
            LOG.error("Lost connection to sentinel {}. Sleeping {}ms and retrying.", node,
                subscribeRetryWaitTimeMillis, e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException se) {
              LOG.error("Sleep interrupted.", se);
            }
          } else {
            LOG.debug("Unsubscribing from sentinel {}.", node);
          }
        } finally {
          IOUtils.closeQuietly(sentinelJedis);
        }
      }
    }

    // must not throw exception
    public void shutdown() {
      try {
        LOG.debug("Shutting down listener on {}.", node);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (sentinelJedis != null) {
          sentinelJedis.close();
        }
      } catch (RuntimeException e) {
        LOG.error("Error while shutting down.", e);
      }
    }
  }
}
