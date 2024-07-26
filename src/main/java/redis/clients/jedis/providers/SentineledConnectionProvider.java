package redis.clients.jedis.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.IOUtils;

public class SentineledConnectionProvider implements ConnectionProvider {

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

  private final Lock initPoolLock = new ReentrantLock(true);

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

  @Experimental
  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Cache clientSideCache, final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, clientSideCache, poolConfig, sentinels, sentinelClientConfig,
        DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig,
      final long subscribeRetryWaitTimeMillis) {
    this(masterName, masterClientConfig, null, poolConfig, sentinels, sentinelClientConfig, subscribeRetryWaitTimeMillis);
  }

  @Experimental
  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Cache clientSideCache, final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig,
      final long subscribeRetryWaitTimeMillis) {

    this.masterName = masterName;
    this.masterClientConfig = masterClientConfig;
    this.clientSideCache = clientSideCache;
    this.masterPoolConfig = poolConfig;

    this.sentinelClientConfig = sentinelClientConfig;
    this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;

    HostAndPort master = initSentinels(sentinels);
    initMaster(master);
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
              LOG.debug("Sentinel {} published: {}.", node, message);

              String[] switchMasterMsg = message.split(" ");

              if (switchMasterMsg.length > 3) {

                if (masterName.equals(switchMasterMsg[0])) {
                  initMaster(toHostAndPort(switchMasterMsg[3], switchMasterMsg[4]));
                } else {
                  LOG.debug(
                      "Ignoring message on +switch-master for master {}. Our master is {}.",
                      switchMasterMsg[0], masterName);
                }

              } else {
                LOG.error("Invalid message received on sentinel {} on channel +switch-master: {}.",
                    node, message);
              }
            }
          }, "+switch-master");

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
