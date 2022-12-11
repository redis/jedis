package redis.clients.jedis.providers;

import redis.clients.jedis.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class SentineledConnectionProvider implements ConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SentineledConnectionProvider.class);

  private final Object initPoolLock = new Object();

  private volatile HostAndPort currentMaster;

  private final GenericObjectPoolConfig<Connection> masterPoolConfig;

  private final JedisClientConfig masterClientConfig;

  private volatile ConnectionPool pool;

  private final JedisClientConfig sentinelClientConfig;

  protected final Collection<MasterListener> masterListeners = new ArrayList<>();

  public SentineledConnectionProvider(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig) {
    this(masterName, sentinels, masterClientConfig, sentinelClientConfig, null);
  }

  public SentineledConnectionProvider(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masterClientConfig, final JedisClientConfig sentinelClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig) {

    this.masterClientConfig = masterClientConfig;
    this.sentinelClientConfig = sentinelClientConfig;
    this.masterPoolConfig = poolConfig;

    HostAndPort master = initSentinels(sentinels, masterName);
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
    masterListeners.forEach(MasterListener::shutdown);

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

  private HostAndPort initSentinels(Set<HostAndPort> sentinels, final String masterName) {

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

      MasterListener masterListener = new MasterListener(masterName, sentinel.getHost(), sentinel.getPort());
      // whether MasterListener threads are alive or not, process can be stopped
      masterListener.setDaemon(true);
      masterListeners.add(masterListener);
      masterListener.start();
    }

    return master;
  }

  private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
    String host = getMasterAddrByNameResult.get(0);
    int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

    return new HostAndPort(host, port);
  }

  protected class MasterListener extends Thread {

    protected String masterName;
    protected String host;
    protected int port;
    protected long subscribeRetryWaitTimeMillis = 5000;
    protected volatile Jedis j;
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected MasterListener() {
    }

    public MasterListener(String masterName, String host, int port) {
      super(String.format("MasterListener-%s-[%s:%d]", masterName, host, port));
      this.masterName = masterName;
      this.host = host;
      this.port = port;
    }

    public MasterListener(String masterName, String host, int port,
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

          // code for active refresh
          List<String> masterAddr = j.sentinelGetMasterAddrByName(masterName);
          if (masterAddr == null || masterAddr.size() != 2) {
            LOG.warn("Can not get master addr, master name: {}. Sentinel: {}.", masterName,
                hostPort);
          } else {
            initMaster(toHostAndPort(masterAddr));
          }

          j.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              LOG.debug("Sentinel {} published: {}.", hostPort, message);

              String[] switchMasterMsg = message.split(" ");

              if (switchMasterMsg.length > 3) {

                if (masterName.equals(switchMasterMsg[0])) {
                  initMaster(toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4])));
                } else {
                  LOG.debug(
                    "Ignoring message on +switch-master for master name {}, our master name is {}",
                    switchMasterMsg[0], masterName);
                }

              } else {
                LOG.error("Invalid message received on Sentinel {} on channel +switch-master: {}",
                    hostPort, message);
              }
            }
          }, "+switch-master");

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

    // must not throw exception
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
