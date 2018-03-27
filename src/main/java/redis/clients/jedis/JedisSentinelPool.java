package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class JedisSentinelPool extends JedisSentinelPoolAbstract {

  protected GenericObjectPoolConfig poolConfig;

  // Pools für Slaves anlegen & auf Sentinel +odown hören
  protected int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  protected int soTimeout = Protocol.DEFAULT_TIMEOUT;

  protected String password;

  protected int database = Protocol.DEFAULT_DATABASE;

  protected String clientName;
  protected String masterName;

  protected Set<MasterListener> masterListeners = new HashSet<MasterListener>();

  protected Logger log = LoggerFactory.getLogger(getClass().getName());

  private volatile JedisFactory factory;
  private volatile HostAndPort currentHostMaster;

  protected ReadFrom readFrom = ReadFrom.MASTER;
  protected JedisSentinelSlaveInfoCache sentinelSlaveInfoCache;

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE, ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels) {
    this(masterName, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE, ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, final ReadFrom readFrom) {
    this(masterName, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, String password) {
    this(masterName, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password,
        ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, String password,
      final ReadFrom readFrom) {
    this(masterName, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password,
        readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password) {
    this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE,
        ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout) {
    this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE,
        ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout, final ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final String password) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password, ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final String password, final ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database, final ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, clientName,
        ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database, final String clientName, final ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, clientName,
        readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout, final int soTimeout,
      final String password, final int database) {
    this(masterName, sentinels, poolConfig, timeout, soTimeout, password, database, null,
        ReadFrom.MASTER);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout, final int soTimeout,
      final String password, final int database, final ReadFrom readFrom) {
    this(masterName, sentinels, poolConfig, timeout, soTimeout, password, database, null, readFrom);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName, final ReadFrom readFrom) {
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
    this.masterName = masterName;
    this.readFrom = readFrom;

    if (!readFrom.equals(ReadFrom.MASTER)) {
      // instantiate sentinel slave caching/discovering
      sentinelSlaveInfoCache = new JedisSentinelSlaveInfoCache(poolConfig, connectionTimeout,
          soTimeout, password, clientName, masterName, database);
    }

    HostAndPort master = initSentinels(sentinels, masterName);
    initPool(master);
  }

  @Override
  public void destroy() {
    for (MasterListener m : masterListeners) {
      m.shutdown();
    }

    super.destroy();
  }

  public HostAndPort getCurrentHostMaster() {
    return currentHostMaster;
  }

  private void initPool(HostAndPort master) {
    if (!master.equals(currentHostMaster)) {
      currentHostMaster = master;
      if (factory == null) {
        factory = new JedisFactory(master.getHost(), master.getPort(), connectionTimeout,
            soTimeout, password, database, clientName);
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

  private HostAndPort initSentinels(Set<String> sentinels, final String masterName) {

    HostAndPort master = null;
    boolean sentinelAvailable = false;

    log.info("Trying to find master from available Sentinels...");

    for (String sentinel : sentinels) {
      final HostAndPort hap = HostAndPort.parseString(sentinel);

      log.debug("Connecting to Sentinel {}", hap);

      Jedis jedis = null;
      try {
        jedis = new Jedis(hap);

        List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);

        // connected to sentinel...
        sentinelAvailable = true;

        if (masterAddr == null || masterAddr.size() != 2) {
          log.warn("Can not get master addr, master name: {}. Sentinel: {}", masterName, hap);
          continue;
        }

        // initially sync sentinel slave info, when read only slaves are active
        if (sentinelSlaveInfoCache != null) sentinelSlaveInfoCache.discoverSlaves(jedis);

        master = toHostAndPort(masterAddr);
        log.debug("Found Redis master at {}", master);
        break;
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        log.warn(
          "Cannot get master address from sentinel running @ {}. Reason: {}. Trying next one.",
          hap, e.toString());
      } finally {
        if (jedis != null) {
          jedis.close();
        }
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not
        // monitored
        throw new JedisException("Can connect to sentinel, but " + masterName
            + " seems to be not monitored...");
      } else {
        throw new JedisConnectionException("All sentinels down, cannot determine where is "
            + masterName + " master is running...");
      }
    }

    log.info("Redis master running at " + master + ", starting Sentinel listeners...");

    for (String sentinel : sentinels) {
      final HostAndPort hap = HostAndPort.parseString(sentinel);
      MasterListener masterListener = new MasterListener(masterName, hap.getHost(), hap.getPort());
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

  @Override
  public Jedis getResource() {
    while (true) {
      Jedis jedis = super.getResource();
      jedis.setDataSource(this);

      // get a reference because it can change concurrently
      final HostAndPort master = currentHostMaster;
      final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient()
          .getPort());

      if (master.equals(connection)) {
        // connected to the correct master
        return jedis;
      } else {
        returnBrokenResource(jedis);
      }
    }
  }

  @Override
  protected void returnBrokenResource(final Jedis resource) {
    if (resource != null) {
      returnBrokenResourceObject(resource);
    }
  }

  @Override
  protected void returnResource(final Jedis resource) {
    if (resource != null) {
      resource.resetState();
      returnResourceObject(resource);
    }
  }

  @Override
  public Jedis getResourceReadOnly() {
    if (readFrom.equals(ReadFrom.MASTER)) return getResource();

    // let the dice decide, whether to use the master or any available slave
    if (readFrom.equals(ReadFrom.BOTH) && ThreadLocalRandom.current().nextBoolean()) return getResource();

    List<JedisPool> availablePools = sentinelSlaveInfoCache.getShuffledSlavesPool(readFrom);

    log.debug("Available pools " + availablePools.size());
    if (availablePools.size() == 0) return getResource();

      try {
          return availablePools.get(0).getResource();
      } catch (JedisConnectionException ignored) {
      }

      // just return a resource of the master pool, when we can't connect to the slave
      return getResource();
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

        j = new Jedis(host, port);

        try {
          // double check that it is not being shutdown
          if (!running.get()) {
            break;
          }

          /*
           * Added code for active refresh
           */
          List<String> masterAddr = j.sentinelGetMasterAddrByName(masterName);
          if (masterAddr == null || masterAddr.size() != 2) {
            log.warn("Can not get master addr, master name: {}. Sentinel: {}：{}.", masterName,
              host, port);
          } else {
            initPool(toHostAndPort(masterAddr));
          }

          j.subscribe(
            new JedisPubSub() {
              @Override
              public void onMessage(String channel, String message) {
                log.debug("Sentinel {}:{} published: {}.", host, port, message);

                switch (channel) {
                case "+switch-master": {
                  String[] switchMasterMsg = message.split(" ");
                  if (switchMasterMsg.length > 3) {

                    if (masterName.equals(switchMasterMsg[0])) {
                      initPool(toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4])));
                    } else {
                      log.debug(
                        "Ignoring message on +switch-master for master name {}, our master name is {}",
                        switchMasterMsg[0], masterName);
                    }

                  } else {
                    log.error(
                      "Invalid message received on Sentinel {}:{} on channel +switch-master: {}",
                      host, port, message);
                  }
                }
                  break;
                case "+slave": {
                  // we don't care about slaves, if no slave connection are to maintain
                  if (sentinelSlaveInfoCache == null) return;
                  String[] newSlaveMsg = message.split(" ");

                  if (newSlaveMsg.length >= 8) {

                    if (masterName.equals(newSlaveMsg[5])) {
                      sentinelSlaveInfoCache.setupSlaveIfNotExists(toHostAndPort(Arrays.asList(
                        newSlaveMsg[2], newSlaveMsg[3])));

                    } else {
                      log.debug(
                        "Ignoring message on +slave for master name {}, our master name is {}",
                        newSlaveMsg[5], masterName);
                    }

                  } else {
                    log.error("Invalid message received on Sentinel {}:{} on channel +slave {}",
                      host, port, message);
                  }

                }
                  break;
                case "+odown":
                case "-odown": {
                  // we don't care about slaves, if no slave connection are to maintain
                  if (sentinelSlaveInfoCache == null) return;
                  String[] odownMessage = message.split(" ");

                  if (!odownMessage[0].equals("slave")) return;

                  if (odownMessage.length >= 8) {

                    if (masterName.equals(odownMessage[5])) {
                      if (channel.startsWith("+")) sentinelSlaveInfoCache
                          .removeSlave(toHostAndPort(Arrays
                              .asList(odownMessage[2], odownMessage[3])));
                      else sentinelSlaveInfoCache.setupSlaveIfNotExists(toHostAndPort(Arrays
                          .asList(odownMessage[2], odownMessage[3])));

                    } else {
                      log.debug("Ignoring message on {} for master name {}, our master name is {}",
                        channel, odownMessage[5], masterName);
                    }

                  } else {
                    log.error("Invalid message received on Sentinel {}:{} on channel {}: {}", host,
                      channel, port, message);
                  }

                }
                  break;
                }
              }
            }, readFrom.equals(ReadFrom.MASTER) ? "+switch-master" : "+switch-master", "+slave",
            "+odown", "-odown");

        } catch (JedisException e) {

          if (running.get()) {
            log.error("Lost connection to Sentinel at {}:{}. Sleeping 5000ms and retrying.", host,
              port, e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException e1) {
              log.error("Sleep interrupted: ", e1);
            }
          } else {
            log.debug("Unsubscribing from Sentinel at {}:{}", host, port);
          }
        } finally {
          j.close();
        }
      }
    }

    public void shutdown() {
      try {
        log.debug("Shutting down listener on {}:{}", host, port);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (j != null) {
          j.disconnect();
        }
      } catch (Exception e) {
        log.error("Caught exception while shutting down: ", e);
      }
    }
  }
}