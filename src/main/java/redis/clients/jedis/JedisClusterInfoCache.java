package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public class JedisClusterInfoCache {
  private final Map<String, JedisPool> nodes = new ConcurrentHashMap<String, JedisPool>();
  private final Map<Integer, JedisPool> slots = new ConcurrentHashMap<Integer, JedisPool>();

  private volatile boolean rediscovering;
  private final GenericObjectPoolConfig poolConfig;

  private int connectionTimeout;
  private int soTimeout;
  private String password;
  private String clientName;

  private boolean ssl;
  private SSLSocketFactory sslSocketFactory;
  private SSLParameters sslParameters;
  private HostnameVerifier hostnameVerifier;
  private JedisClusterHostAndPortMap hostAndPortMap;

  private static final int MASTER_NODE_INDEX = 2;

  public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig, int timeout) {
    this(poolConfig, timeout, timeout, null, null);
  }

  public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig,
      final int connectionTimeout, final int soTimeout, final String password, final String clientName) {
    this(poolConfig, connectionTimeout, soTimeout, password, clientName, false, null, null, null, null);
  }

  public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig,
      final int connectionTimeout, final int soTimeout, final String password, final String clientName,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, 
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.clientName = clientName;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
    this.hostAndPortMap = hostAndPortMap;
  }

  public void discoverClusterNodesAndSlots(Jedis jedis) {

    reset();
    List<Object> slots = jedis.clusterSlots();

    for (Object slotInfoObj : slots) {
      List<Object> slotInfo = (List<Object>) slotInfoObj;

      if (slotInfo.size() <= MASTER_NODE_INDEX) {
        continue;
      }

      List<Integer> slotNums = getAssignedSlotArray(slotInfo);

      // hostInfos
      int size = slotInfo.size();
      for (int i = MASTER_NODE_INDEX; i < size; i++) {
        List<Object> hostInfos = (List<Object>) slotInfo.get(i);
        if (hostInfos.size() <= 0) {
          continue;
        }

        HostAndPort targetNode = generateHostAndPort(hostInfos);
        setupNodeIfNotExist(targetNode);
        if (i == MASTER_NODE_INDEX) {
          assignSlotsToNode(slotNums, targetNode);
        }
      }
    }

  }

  private HostAndPort generateHostAndPort(List<Object> hostInfos) {
    String host = SafeEncoder.encode((byte[]) hostInfos.get(0));
    int port = ((Long) hostInfos.get(1)).intValue();
    if (ssl && hostAndPortMap != null) {
      HostAndPort hostAndPort = hostAndPortMap.getSSLHostAndPort(host, port);
      if (hostAndPort != null) {
        return hostAndPort;
      }
    }
    return new HostAndPort(host, port);
  }

  public JedisPool setupNodeIfNotExist(HostAndPort node) {
    String nodeKey = getNodeKey(node);
    JedisPool existingPool = nodes.get(nodeKey);
    if (existingPool != null) return existingPool;

    JedisPool nodePool = new JedisPool(poolConfig, node.getHost(), node.getPort(),
            connectionTimeout, soTimeout, password, 0, clientName,
            ssl, sslSocketFactory, sslParameters, hostnameVerifier);
    nodes.put(nodeKey, nodePool);
    return nodePool;
  }

  public void assignSlotToNode(int slot, HostAndPort targetNode) {
    JedisPool targetPool = setupNodeIfNotExist(targetNode);
    slots.put(slot, targetPool);
  }

  public void assignSlotsToNode(List<Integer> targetSlots, HostAndPort targetNode) {
    JedisPool targetPool = setupNodeIfNotExist(targetNode);
    for (Integer slot : targetSlots) {
      slots.put(slot, targetPool);
    }
  }

  public JedisPool getNode(String nodeKey) {
    return nodes.get(nodeKey);
  }

  public JedisPool getSlotPool(int slot) {
    return slots.get(slot);
  }

  public Map<String, JedisPool> getNodes() {
    return new HashMap<String, JedisPool>(nodes);
  }

  public List<JedisPool> getShuffledNodesPool() {
    List<JedisPool> pools = new ArrayList<JedisPool>(nodes.values());
    Collections.shuffle(pools);
    return pools;
  }

  /**
   * Clear discovered nodes collections and gently release allocated resources
   */
  public void reset() {
    for (JedisPool pool : nodes.values()) {
      try {
        if (pool != null) {
          pool.destroy();
        }
      } catch (Exception e) {
        // pass
      }
    }
    nodes.clear();
    slots.clear();
  }

  public static String getNodeKey(HostAndPort hnp) {
    return hnp.getHost() + ":" + hnp.getPort();
  }

  public static String getNodeKey(Client client) {
    return client.getHost() + ":" + client.getPort();
  }

  public static String getNodeKey(Jedis jedis) {
    return getNodeKey(jedis.getClient());
  }

  private List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
    List<Integer> slotNums = new ArrayList<Integer>();
    for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1))
        .intValue(); slot++) {
      slotNums.add(slot);
    }
    return slotNums;
  }
}
