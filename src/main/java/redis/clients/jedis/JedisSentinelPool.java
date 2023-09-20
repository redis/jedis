package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

public class JedisSentinelPool extends Pool<Jedis> {

  private static final Logger LOG = LoggerFactory.getLogger(JedisSentinelPool.class);

  private final JedisFactory factory;

  private final JedisClientConfig sentinelClientConfig;

  protected final Collection<SentinelMasterListener> masterListeners = new ArrayList<>();

  private volatile HostAndPort currentHostMaster;

  private final Object initPoolLock = new Object();

  public JedisSentinelPool(String masterName, Set<HostAndPort> sentinels,
      final JedisClientConfig masteClientConfig, final JedisClientConfig sentinelClientConfig) {
    this(masterName, sentinels, new JedisFactory(masteClientConfig), sentinelClientConfig);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, String password) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, String password, String sentinelPassword) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT,
        password, Protocol.DEFAULT_DATABASE, null, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, sentinelPassword, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password) {
    this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int timeout) {
    this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final String password) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password,
      final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, null, password, database);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String user,
      final String password, final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, user, password, database);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password,
      final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, clientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String user,
      final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, user, password, database, clientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String user, final String password, final int database) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, user, password, database, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, clientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String user, final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, user, password, database, clientName,
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null, null, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName,
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null, null, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName,
      final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelPassword,
      final String sentinelClientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, clientName,
        sentinelConnectionTimeout, sentinelSoTimeout, null, sentinelPassword, sentinelClientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String user, final String password, final int database, final String clientName,
      final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelUser,
      final String sentinelPassword, final String sentinelClientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, 0, user, password, database, clientName,
        sentinelConnectionTimeout, sentinelSoTimeout, sentinelUser, sentinelPassword, sentinelClientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
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

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final JedisFactory factory) {
    this(masterName, parseHostAndPorts(sentinels), poolConfig, factory,
        DefaultJedisClientConfig.builder().build());
  }

  public JedisSentinelPool(String masterName, Set<HostAndPort> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final JedisClientConfig masteClientConfig,
      final JedisClientConfig sentinelClientConfig) {
    this(masterName, sentinels, poolConfig, new JedisFactory(masteClientConfig), sentinelClientConfig);
  }

  public JedisSentinelPool(String masterName, Set<HostAndPort> sentinels,
      final JedisFactory factory, final JedisClientConfig sentinelClientConfig) {
    super(factory);

    this.factory = factory;
    this.sentinelClientConfig = sentinelClientConfig;

    HostAndPort master = initSentinels(sentinels, masterName);
    initMaster(master);
    initMasterListeners(sentinels, masterName);
  }

  public JedisSentinelPool(String masterName, Set<HostAndPort> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final JedisFactory factory,
      final JedisClientConfig sentinelClientConfig) {
    super(poolConfig, factory);

    this.factory = factory;
    this.sentinelClientConfig = sentinelClientConfig;

    HostAndPort master = initSentinels(sentinels, masterName);
    initMaster(master);
    initMasterListeners(sentinels, masterName, poolConfig);
  }

  private void initMasterListeners(Set<HostAndPort> sentinels, String masterName) {
    initMasterListeners(sentinels, masterName, null);
  }

  private void initMasterListeners(Set<HostAndPort> sentinels, String masterName,
      GenericObjectPoolConfig<Jedis> poolConfig) {

    LOG.info("Starting Sentinel listeners for {}...", masterName);
    SentinelPoolConfig jedisSentinelPoolConfig = null;
    if (poolConfig instanceof SentinelPoolConfig) {
      jedisSentinelPoolConfig = ((SentinelPoolConfig) poolConfig);
    } else {
      jedisSentinelPoolConfig = new SentinelPoolConfig();
    }

    for (HostAndPort sentinel : sentinels) {
      if (jedisSentinelPoolConfig.isEnableActiveDetectListener()) {
        masterListeners.add(
          new SentinelMasterActiveDetectListener(currentHostMaster, sentinel, sentinelClientConfig,
              masterName, jedisSentinelPoolConfig.getActiveDetectIntervalTimeMillis()) {
            @Override
            public void onChange(HostAndPort hostAndPort) {
              initMaster(hostAndPort);
            }
          });
      }

      if (jedisSentinelPoolConfig.isEnableDefaultSubscribeListener()) {
        masterListeners.add(new SentinelMasterSubscribeListener(masterName, sentinel,
            sentinelClientConfig, jedisSentinelPoolConfig.getSubscribeRetryWaitTimeMillis()) {
          @Override
          public void onChange(HostAndPort hostAndPort) {
            initMaster(hostAndPort);
          }
        });
      }
    }
    masterListeners.forEach(SentinelMasterListener::start);
  }

  private static Set<HostAndPort> parseHostAndPorts(Set<String> strings) {
    return strings.stream().map(HostAndPort::from).collect(Collectors.toSet());
  }

  @Override
  public void destroy() {
    masterListeners.forEach(SentinelMasterListener::shutdown);
    super.destroy();
  }

  public HostAndPort getCurrentHostMaster() {
    return currentHostMaster;
  }

  private void initMaster(HostAndPort master) {
    synchronized (initPoolLock) {
      if (!master.equals(currentHostMaster)) {
        currentHostMaster = master;
        factory.setHostAndPort(currentHostMaster);
        // although we clear the pool, we still have to check the returned object in getResource,
        // this call only clears idle instances, not borrowed instances
        super.clear();

        LOG.info("Created JedisSentinelPool to master at {}", master);
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

    LOG.info("Redis master running at {}", master);

    return master;
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
      final HostAndPort connection = jedis.getClient().getHostAndPort();

      if (master.equals(connection)) {
        // connected to the correct master
        return jedis;
      } else {
        returnBrokenResource(jedis);
      }
    }
  }

  @Override
  public void returnResource(final Jedis resource) {
    if (resource != null) {
      try {
        resource.resetState();
        super.returnResource(resource);
      } catch (RuntimeException e) {
        returnBrokenResource(resource);
        LOG.debug("Resource is returned to the pool as broken", e);
      }
    }
  }
}
