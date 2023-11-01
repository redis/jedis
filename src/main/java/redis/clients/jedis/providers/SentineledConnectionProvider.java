package redis.clients.jedis.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.SentinelPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.sentinel.listenner.SentinelActiveDetectListener;
import redis.clients.jedis.sentinel.listenner.SentinelListener;
import redis.clients.jedis.sentinel.listenner.SentinelSubscribeListener;

public class SentineledConnectionProvider implements ConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SentineledConnectionProvider.class);

  protected static final long DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS = 5000;

  private volatile HostAndPort currentMaster;

  private volatile ConnectionPool pool;

  private final String masterName;

  private final JedisClientConfig masterClientConfig;

  private final GenericObjectPoolConfig<Connection> masterPoolConfig;

  private final Collection<SentinelListener> sentinelListeners = new ArrayList<>();

  private final JedisClientConfig sentinelClientConfig;

  private final Object initPoolLock = new Object();

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, /*poolConfig*/ null, sentinels, sentinelClientConfig);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig,
        DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig,
      final long subscribeRetryWaitTimeMillis) {

    this.masterName = masterName;
    this.masterClientConfig = masterClientConfig;
    this.masterPoolConfig = poolConfig;

    this.sentinelClientConfig = sentinelClientConfig;

    HostAndPort master = initSentinels(sentinels);
    initMaster(master);
    initMasterListeners(sentinels, masterName, poolConfig, subscribeRetryWaitTimeMillis);
  }

  private void initMasterListeners(Set<HostAndPort> sentinels, String masterName,
      GenericObjectPoolConfig poolConfig, long subscribeRetryWaitTimeMillis) {

    LOG.info("Init master node listener {}", masterName);
    SentinelPoolConfig jedisSentinelPoolConfig = null;
    if (poolConfig instanceof SentinelPoolConfig) {
      jedisSentinelPoolConfig = ((SentinelPoolConfig) poolConfig);
      /***
       * if SentinelPoolConfig is set to used , the subscribe Retry Wait time will use
       * SentinelPoolConfig(subscribeRetryWaitTimeMillis) instead of param
       * subscribeRetryWaitTimeMillis in this method
       */
    } else {
      jedisSentinelPoolConfig = new SentinelPoolConfig();
      jedisSentinelPoolConfig.setSubscribeRetryWaitTimeMillis(subscribeRetryWaitTimeMillis);
    }

    for (HostAndPort sentinel : sentinels) {
      if (jedisSentinelPoolConfig.isEnableActiveDetectListener()) {
        sentinelListeners.add(
          new SentinelActiveDetectListener(currentMaster, sentinel, sentinelClientConfig,
              masterName, jedisSentinelPoolConfig.getActiveDetectIntervalTimeMillis()) {
            @Override
            public void onChange(HostAndPort hostAndPort) {
              initMaster(hostAndPort);
            }
          });
      }

      if (jedisSentinelPoolConfig.isEnableDefaultSubscribeListener()) {
        sentinelListeners.add(new SentinelSubscribeListener(masterName, sentinel,
            sentinelClientConfig, jedisSentinelPoolConfig.getSubscribeRetryWaitTimeMillis()) {
          @Override
          public void onChange(HostAndPort hostAndPort) {
            initMaster(hostAndPort);
          }
        });
      }
    }

    sentinelListeners.forEach(SentinelListener::start);
  }

  @Override
  public Connection getConnection() {
    return pool.getResource();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    return pool.getResource();
  }

  @Override
  public void close() {
    sentinelListeners.forEach(SentinelListener::shutdown);
    pool.close();
  }

  public HostAndPort getCurrentMaster() {
    return currentMaster;
  }

  private void initMaster(HostAndPort master) {
    synchronized (initPoolLock) {
      if (!master.equals(currentMaster)) {
        currentMaster = master;

        ConnectionPool newPool = masterPoolConfig != null
            ? new ConnectionPool(currentMaster, masterClientConfig, masterPoolConfig)
            : new ConnectionPool(currentMaster, masterClientConfig);

        ConnectionPool existingPool = pool;
        pool = newPool;
        LOG.info("Created connection pool to master at {}.", master);

        if (existingPool != null) {
          // although we clear the pool, we still have to check the returned object in getResource,
          // this call only clears idle instances, not borrowed instances
          // existingPool.clear(); // necessary??
          existingPool.close();
        }
      }
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

    LOG.info("Redis master running at {}.", master);

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
}
