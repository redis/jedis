package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JedisSentinelSlavePool extends Pool<Jedis> {

  protected Logger logger = LoggerFactory.getLogger(JedisSentinelSlavePool.class.getName());

  protected GenericObjectPoolConfig<Jedis> poolConfig;
  protected final Collection<MasterListener> masterListeners = new ArrayList<>();
  private final JedisSentinelSlaveFactory factory;
  private volatile HostAndPort currentHostMaster;
  private volatile List<HostAndPort> currentSlaves;
  private final Object initPoolLock = new Object();
  private final JedisClientConfig sentinelClientConfig;
  private final Set<HostAndPort> sentinels;

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels, final GenericObjectPoolConfig<Jedis> poolConfig, final int timeout, final int soTimeout, final String password, final int database, final String sentinelPassword) {
    this(masterName, sentinels, poolConfig, timeout, soTimeout, password, database, null, DefaultJedisClientConfig.builder().connectionTimeoutMillis(timeout).socketTimeoutMillis(timeout).password(sentinelPassword).build(), new JedisSentinelSlaveFactory(timeout, soTimeout, password, database, null, false, null, null, null, masterName));
  }

  public JedisSentinelSlavePool(String masterName, Set<String> sentinels, final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout, final String password, final int database, final String clientName, final JedisClientConfig sentinelClientConfig, JedisSentinelSlaveFactory factory) {

    super(poolConfig, factory);
    this.sentinelClientConfig = sentinelClientConfig;
    this.poolConfig = poolConfig;
    this.factory = factory;
    this.sentinels = parseHostAndPorts(sentinels);
    HostAndPort master = initsentinels(this.sentinels, masterName);
    initPool(master);
  }

  private static Set<HostAndPort> parseHostAndPorts(Set<String> strings) {
    return strings.stream().map(HostAndPort::from).collect(Collectors.toSet());
  }

  private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
    String host = getMasterAddrByNameResult.get(0);
    int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

    return new HostAndPort(host, port);
  }

  @Override
  public void destroy() {
    for (MasterListener m : masterListeners) {
      m.shutdown();
    }
    super.destroy();
  }

  @Override
  public Jedis getResource() {
    while (true) {
      Jedis jedis = super.getResource();
      jedis.setDataSource(this);

      final List<HostAndPort> slaves = currentSlaves;
      final HostAndPort connection = jedis.getConnection().getHostAndPort();

      if (slaves.isEmpty()) {
        logger.debug("slave node is empty! host:{},port:{}", connection.getHost(), connection.getPort());
        returnBrokenResource(jedis);
      }
      if (slaves.contains(connection)) {
        return jedis;
      } else {
        returnBrokenResource(jedis);
      }
    }
  }

  private void initPool(HostAndPort master) {
    synchronized (initPoolLock) {
      if (!master.equals(currentHostMaster)) {
        currentHostMaster = master;
        // update newest slaves
        factory.setSlavesHostAndPort(currentSlaves);
        // although we clear the pool, we still have to check the returned object in getResource,
        // this call only clears idle instances, not borrowed instances
        super.clear();

        logger.info("Created JedisSentinelPool to master at {}", master);
      }
    }
  }

  public void setCurrentSlaves(List<HostAndPort> currentSlaves) {
    if (currentSlaves == null || currentSlaves.size() == 0) {
      return;
    }
    this.currentSlaves = currentSlaves;
  }

  public List<HostAndPort> getCurrentSlaves() {
    return currentSlaves;
  }

  private HostAndPort initsentinels(Set<HostAndPort> sentinels, final String masterName) {

    HostAndPort master = null;
    boolean sentinelAvailable = false;

    logger.info("Trying to find a valid sentinel from available Sentinels " + masterName);

    for (HostAndPort hap : sentinels) {

      logger.info("Connecting to Sentinel " + hap + ",masterName = " + masterName);

      try (Jedis jedis = new Jedis(hap, sentinelClientConfig)) {
        sentinelAvailable = true;

        List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);
        if (masterAddr == null || masterAddr.size() != 2) {
          logger.warn("Can not get master addr from sentinel, master name: " + masterName + ". Sentinel: " + hap + ".");
          continue;
        }

        master = toHostAndPort(masterAddr);

        //init currentSlaves
        List<Map<String, String>> slaves = jedis.sentinelReplicas(masterName);
        logger.info(masterName + " sentinelSlaves:" + slaves);
        if (slaves == null || slaves.size() == 0) {
          continue;
        }
        // filter status is down
        this.setCurrentSlaves(slaves.stream().filter(this::checkNodeStatus)
                .map(slave -> HostAndPort.from(slave.get("ip") + ":" + slave.get("port")))
                .collect(Collectors.toList()));
        logger.info(masterName + " set currentSlaves " + currentSlaves);

        logger.debug("Found Redis master at {}", master);

        break;
      } catch (JedisException e) {
        logger.warn("Cannot get master address from sentinel running @ " + hap + ". Reason: " + e + ". Trying next one.");
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not monitored
        throw new JedisException("Can connect to sentinel, but " + masterName + " seems to be not monitored...");
      } else {
        throw new JedisConnectionException("All sentinels down, cannot determine where is " + masterName + " master is running...");
      }
    }

    logger.info("Redis master running at {}, starting Sentinel listeners...", master);
    if (masterListeners.size() == 0) {
      for (HostAndPort hap : sentinels) {
        MasterListener masterListener = new MasterListener(masterName, hap.getHost(), hap.getPort());
        // whether MasterListener threads are alive or not, process can be stopped
        masterListener.setDaemon(true);
        masterListeners.add(masterListener);
        masterListener.start();
      }
    }

    return master;
  }

  private boolean checkNodeStatus(Map<String, String> slave) {
    return !slave.get("flags").contains("s_down") && "ok".equals(slave.get("master-link-status"));
  }

  @Override
  public void returnResource(final Jedis resource) {
    if (resource != null) {
      try {
        resource.resetState();
        super.returnResource(resource);
      } catch (RuntimeException e) {
        returnBrokenResource(resource);
        logger.debug("Resource is returned to the pool as broken", e);
      }

    }
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

    public MasterListener(String masterName, String host, int port, long subscribeRetryWaitTimeMillis) {
      this(masterName, host, port);
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }

    @Override
    public void run() {

      running.set(true);

      while (running.get()) {
        final HostAndPort hostPort = new HostAndPort(host, port);
        j = new Jedis(hostPort, sentinelClientConfig);

        try {
          // double check that it is not being shutdown
          if (!running.get()) {
            break;
          }

          j.subscribe(new SentinelSlaveChangePubSub(), "+switch-master", "+slave", "+sdown", "+odown", "+reboot");

        } catch (JedisConnectionException e) {

          if (running.get()) {
            logger.error("Lost connection to Sentinel at " + host + ":" + port + ". Sleeping 5000ms and retrying.", e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException e1) {
              logger.info("Sleep interrupted: ", e1);
            }
          } else {
            logger.info("Unsubscribing from Sentinel at " + host + ":" + port);
          }
        } finally {
          j.close();
        }
      }
    }

    public void shutdown() {
      try {
        logger.info("Shutting down listener on " + host + ":" + port);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (j != null) {
          j.disconnect();
        }
      } catch (Exception e) {
        logger.error("Caught exception while shutting down: ", e);
      }
    }

    private class SentinelSlaveChangePubSub extends JedisPubSub {
      @Override
      public void onMessage(String channel, String message) {
        if (masterName == null) {
          logger.error("Master Name is null!");
          throw new InvalidParameterException("Master Name is null!");
        }
        logger.info("Get message on chanel[" + channel + "], published [" + message + "]" + ". current sentinel " + host + ":" + port);

        String[] msg = message.split(" ");
        List<String> msgList = Arrays.asList(msg);
        if (msgList.isEmpty()) {
          return;
        }
        boolean needResetPool = masterName.equalsIgnoreCase(msgList.get(0));
        int tmpIndex = msgList.indexOf("@") + 1;
        if (tmpIndex > 0 && masterName.equalsIgnoreCase(msgList.get(tmpIndex))) { //message from other channels
          needResetPool = true;
        }
        if (needResetPool) {
          // sleep 1s Ensure sentinel status is updated
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            logger.error("initSentinels error");
          }
          HostAndPort master = initsentinels(sentinels, masterName);
          initPool(master);
        } else {
          logger.info("message is not for master " + masterName);
        }
      }
    }
  }
}