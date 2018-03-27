package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JedisSentinelSlaveInfoCache extends JedisSentinelSlaveInfoCacheAbstract {
  private final Map<String, JedisPool> slaveNodes = new HashMap<>();
  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();
  private final GenericObjectPoolConfig poolConfig;
  protected Logger log = LoggerFactory.getLogger(getClass().getName());

  private int connectionTimeout;
  private int soTimeout;
  private int database;
  private String password;
  private String clientName;
  private final String masterName;

  // todo configurable slave pool config
  public JedisSentinelSlaveInfoCache(final GenericObjectPoolConfig poolConfig, int timeout,
      String masterName) {
    this(poolConfig, timeout, timeout, null, null, masterName, 0);
  }

  public JedisSentinelSlaveInfoCache(final GenericObjectPoolConfig poolConfig,
      final int connectionTimeout, final int soTimeout, final String password,
      final String clientName, final String masterName, final int database) {
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.clientName = clientName;
    this.masterName = masterName;
    this.database = database;
  }

  @Override
  public List<JedisPool> getSlaves() {
    r.lock();
    try {
      return new ArrayList<>(slaveNodes.values());
    } finally {
      r.unlock();
    }
  }

  @Override
  public void discoverSlaves(Jedis jedis) {
    w.lock();

    try {
      List<HostAndPort> hostAndPorts = toHostAndPorts(jedis.sentinelSlaves(masterName));

      for (HostAndPort hnp : hostAndPorts) {
        setupSlaveIfNotExists(hnp);
      }
    } finally {
      w.unlock();
    }
  }

  @Override
  public void removeSlave(HostAndPort hnp) {
    w.lock();
    try {
      JedisPool toRemove = slaveNodes.remove(hnp.getHost() + ":" + hnp.getPort());
      try {
        if (toRemove != null) {
          toRemove.destroy();
        }
      } catch (Exception e) {
        // pass
      }

    } finally {
      w.unlock();
    }
  }

  @Override
  public void reset() {
    w.lock();
    try {
      List<JedisPool> pools = getSlaves();
      pools.addAll(slaveNodes.values());
      for (JedisPool pool : pools) {
        try {
          if (pool != null) {
            pool.destroy();
          }
        } catch (Exception e) {
          // pass
        }
      }
      slaveNodes.clear();
    } finally {
      w.unlock();
    }
  }

  @Override
  public List<JedisPool> getShuffledSlavesPool(ReadFrom readFrom) {
    List<JedisPool> slaves = getSlaves();
    Collections.shuffle(getSlaves());

    return slaves;
  }

  public JedisPool setupSlaveIfNotExists(HostAndPort hnp) {
    w.lock();
    try {
      log.debug("Try to add slave {}:{} to sentinel slave info", hnp.getHost(), hnp.getPort());
      JedisPool existingPool = slaveNodes.get(hnp.getHost() + ":" + hnp.getPort());
      if (existingPool != null) {
        return existingPool;
      }

      JedisPool nodePool = new JedisPool(poolConfig, hnp.getHost(), hnp.getPort(),
          connectionTimeout, soTimeout, password, database, clientName, false, null, null, null,
          false);
      slaveNodes.put(hnp.getHost() + ":" + hnp.getPort(), nodePool);

      return nodePool;
    } finally {
      w.unlock();
    }
  }

  private List<HostAndPort> toHostAndPorts(List<Map<String, String>> getSlavesResult) {
    log.debug("Discovered slaves: " + getSlavesResult);
    List<HostAndPort> slaves = new ArrayList<>();

    for (Map<String, String> slaveEntry : getSlavesResult) {
      String host = slaveEntry.get("ip");
      int port = Integer.parseInt(slaveEntry.get("port"));

      slaves.add(new HostAndPort(host, port));
    }

    return slaves;
  }
}
