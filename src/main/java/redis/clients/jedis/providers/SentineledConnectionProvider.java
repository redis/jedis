package redis.clients.jedis.providers;

import redis.clients.jedis.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class SentineledConnectionProvider implements ConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SentineledConnectionProvider.class);

  private final Object initPoolLock = new Object();

  private final String masterName;

  private volatile HostAndPort currentMaster;

  private final GenericObjectPoolConfig<Connection> masterPoolConfig;

  private final JedisClientConfig masterClientConfig;

  private volatile ConnectionPool pool;

  private final long subscribeRetryWaitTimeMillis;

  private final SentinelSubscriber sentinelSubscriber;

  private final JedisClientConfig sentinelClientConfig;

  protected final Collection<SentinelListener> sentinelListeners = new ArrayList<>();

  public SentineledConnectionProvider(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig) {
    this(masterName, sentinels, masterClientConfig, sentinelClientConfig, null);
  }

  public SentineledConnectionProvider(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig) {
    this(masterName, sentinels, masterClientConfig, sentinelClientConfig, poolConfig,
        DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS, DEFAULT_SENTINEL_SUBSCRIBER);
  }

  public SentineledConnectionProvider(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig, long subscribeRetryWaitTimeMillis,
      SentinelSubscriber sentinelSubscriber) {

    this.masterName = masterName;
    this.masterClientConfig = masterClientConfig;
    this.sentinelClientConfig = sentinelClientConfig;
    this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    this.sentinelSubscriber = sentinelSubscriber;
    this.masterPoolConfig = poolConfig;

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

  private final Consumer<HostAndPort> initMaster = (node) -> initMaster(node);

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

    LOG.info("Trying to find master from available Sentinels...");

    for (HostAndPort sentinel : sentinels) {

      LOG.debug("Connecting to Sentinel {}", sentinel);

      try (Jedis jedis = new Jedis(sentinel, sentinelClientConfig)) {

        List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);

        // connected to sentinel...
        sentinelAvailable = true;

        if (masterAddr == null || masterAddr.size() != 2) {
          LOG.warn("Can not get master addr, master name: {}. Sentinel: {}", masterName, sentinel);
          continue;
        }

        master = toHostAndPort(masterAddr);
        LOG.debug("Found Redis master at {}", master);
        break;
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        LOG.warn(
          "Cannot get master address from sentinel running @ {}. Reason: {}. Trying next one.", sentinel, e);
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not monitored
        throw new JedisException("Can connect to sentinel, but " + masterName
            + " seems to be not monitored...");
      } else {
        throw new JedisConnectionException("All sentinels down, cannot determine where is "
            + masterName + " master is running...");
      }
    }

    LOG.info("Redis master running at {}, starting Sentinel listeners...", master);

    for (HostAndPort sentinel : sentinels) {

      SentinelListener masterListener = new SentinelListener(sentinel);
      // whether MasterListener threads are alive or not, process can be stopped
      masterListener.setDaemon(true);
      sentinelListeners.add(masterListener);
      masterListener.start();
    }

    return master;
  }

  private static HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
    String host = getMasterAddrByNameResult.get(0);
    int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

    return new HostAndPort(host, port);
  }

  public interface SentinelSubscriber {

    JedisPubSub createSubscriber(HostAndPort node, String masterName, Consumer<HostAndPort> initMaster);

    String[] getPubSubChannels();
  }

  private static final SentinelSubscriber DEFAULT_SENTINEL_SUBSCRIBER = new DefaultSentinelSubscriber();

  public static class DefaultSentinelSubscriber implements SentinelSubscriber {

    public DefaultSentinelSubscriber() {
    }

    @Override
    public JedisPubSub createSubscriber(HostAndPort node, String masterName, Consumer<HostAndPort> initMaster) {
      return new JedisPubSub() {
        @Override
        public void onMessage(String channel, String message) {
          LOG.debug("Sentinel {} published: {}.", node, message);

          String[] switchMasterMsg = message.split(" ");

          if (switchMasterMsg.length > 3) {

            if (masterName.equals(switchMasterMsg[0])) {
              initMaster.accept(toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4])));
            } else {
              LOG.debug(
                  "Ignoring message on +switch-master for master name {}, our master name is {}",
                  switchMasterMsg[0], masterName);
            }

          } else {
            LOG.error("Invalid message received on Sentinel {} on channel +switch-master: {}",
                node, message);
          }
        }
      };
    }

    @Override
    public String[] getPubSubChannels() {
      return new String[]{"+switch-master"};
    }
  }

  protected static final long DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS = 5000;

  protected class SentinelListener extends Thread {

    protected final HostAndPort node;
    protected volatile Jedis sentinelJedis;
    protected AtomicBoolean running = new AtomicBoolean(false);

    public SentinelListener(HostAndPort node) {
      super(String.format("SentinelListener-%s-[%s]", masterName, node.toString()));
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
            LOG.warn("Can not get master addr, master name: {}. Sentinel: {}.", masterName, node);
          } else {
            initMaster.accept(toHostAndPort(masterAddr));
          }

          sentinelJedis.subscribe(sentinelSubscriber.createSubscriber(node, masterName, initMaster),
              sentinelSubscriber.getPubSubChannels());

        } catch (JedisException e) {

          if (running.get()) {
            LOG.error("Lost connection to Sentinel at {}. Sleeping {}ms and retrying.",
                node, subscribeRetryWaitTimeMillis, e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException se) {
              LOG.error("Sleep interrupted.", se);
            }
          } else {
            LOG.debug("Unsubscribing from Sentinel at {}.", node);
          }
        } finally {
          if (sentinelJedis != null) {
            sentinelJedis.close();
          }
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
