package redis.clients.jedis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

import static redis.clients.jedis.JedisCluster.INIT_NO_ERROR_PROPERTY;

@Internal
public class JedisClusterInfoCache {

  private static final Logger logger = LoggerFactory.getLogger(JedisClusterInfoCache.class);

  private final Map<String, ConnectionPool> nodes = new HashMap<>();
  private final ConnectionPool[] slots = new ConnectionPool[Protocol.CLUSTER_HASHSLOTS];
  private final HostAndPort[] slotNodes = new HostAndPort[Protocol.CLUSTER_HASHSLOTS];

  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();
  private final Lock rediscoverLock = new ReentrantLock();

  private final GenericObjectPoolConfig<Connection> poolConfig;
  private final JedisClientConfig clientConfig;
  private final ClientSideCache clientSideCache;
  private final Set<HostAndPort> startNodes;

  private static final int MASTER_NODE_INDEX = 2;

  /**
   * The single thread executor for the topology refresh task.
   */
  private ScheduledExecutorService topologyRefreshExecutor = null;

  class TopologyRefreshTask implements Runnable {
    @Override
    public void run() {
      logger.debug("Cluster topology refresh run, old nodes: {}", nodes.keySet());
      renewClusterSlots(null);
      logger.debug("Cluster topology refresh run, new nodes: {}", nodes.keySet());
    }
  }

  public JedisClusterInfoCache(final JedisClientConfig clientConfig, final Set<HostAndPort> startNodes) {
    this(clientConfig, null, null, startNodes);
  }

  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      final Set<HostAndPort> startNodes) {
    this(clientConfig, clientSideCache, null, startNodes);
  }

  public JedisClusterInfoCache(final JedisClientConfig clientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes) {
    this(clientConfig, null, poolConfig, startNodes);
  }

  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes) {
    this(clientConfig, clientSideCache, poolConfig, startNodes, null);
  }

  public JedisClusterInfoCache(final JedisClientConfig clientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes,
      final Duration topologyRefreshPeriod) {
    this(clientConfig, null, poolConfig, startNodes, topologyRefreshPeriod);
  }

  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, ClientSideCache clientSideCache,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes,
      final Duration topologyRefreshPeriod) {
    this.poolConfig = poolConfig;
    this.clientConfig = clientConfig;
    this.clientSideCache = clientSideCache;
    this.startNodes = startNodes;
    if (topologyRefreshPeriod != null) {
      logger.info("Cluster topology refresh start, period: {}, startNodes: {}", topologyRefreshPeriod, startNodes);
      topologyRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
      topologyRefreshExecutor.scheduleWithFixedDelay(new TopologyRefreshTask(), topologyRefreshPeriod.toMillis(),
          topologyRefreshPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Check whether the number and order of slots in the cluster topology are equal to CLUSTER_HASHSLOTS
   * @param slotsInfo the cluster topology
   * @return if slots is ok, return true, elese return false.
   */
  private boolean checkClusterSlotSequence(List<Object> slotsInfo) {
    List<Integer> slots = new ArrayList<>();
    for (Object slotInfoObj : slotsInfo) {
      List<Object> slotInfo = (List<Object>)slotInfoObj;
      slots.addAll(getAssignedSlotArray(slotInfo));
    }
    Collections.sort(slots);
    if (slots.size() != Protocol.CLUSTER_HASHSLOTS) {
      return false;
    }
    for (int i = 0; i < Protocol.CLUSTER_HASHSLOTS; ++i) {
      if (i != slots.get(i)) {
        return false;
      }
    }
    return true;
  }

  public void discoverClusterNodesAndSlots(Connection jedis) {
    List<Object> slotsInfo = executeClusterSlots(jedis);
    if (System.getProperty(INIT_NO_ERROR_PROPERTY) == null) {
      if (slotsInfo.isEmpty()) {
        throw new JedisClusterOperationException("Cluster slots list is empty.");
      }
      if (!checkClusterSlotSequence(slotsInfo)) {
        throw new JedisClusterOperationException("Cluster slots have holes.");
      }
    }
    w.lock();
    try {
      reset();
      for (Object slotInfoObj : slotsInfo) {
        List<Object> slotInfo = (List<Object>) slotInfoObj;

        if (slotInfo.size() <= MASTER_NODE_INDEX) {
          continue;
        }

        List<Integer> slotNums = getAssignedSlotArray(slotInfo);

        // hostInfos
        int size = slotInfo.size();
        for (int i = MASTER_NODE_INDEX; i < size; i++) {
          List<Object> hostInfos = (List<Object>) slotInfo.get(i);
          if (hostInfos.isEmpty()) {
            continue;
          }

          HostAndPort targetNode = generateHostAndPort(hostInfos);
          setupNodeIfNotExist(targetNode);
          if (i == MASTER_NODE_INDEX) {
            assignSlotsToNode(slotNums, targetNode);
          }
        }
      }
    } finally {
      w.unlock();
    }
  }

  public void renewClusterSlots(Connection jedis) {
    // If rediscovering is already in process - no need to start one more same rediscovering, just return
    if (rediscoverLock.tryLock()) {
      try {
        // First, if jedis is available, use jedis renew.
        if (jedis != null) {
          try {
            discoverClusterSlots(jedis);
            return;
          } catch (JedisException e) {
            // try nodes from all pools
          }
        }

        // Then, we use startNodes to try, as long as startNodes is available,
        // whether it is vip, domain, or physical ip, it will succeed.
        if (startNodes != null) {
          for (HostAndPort hostAndPort : startNodes) {
            try (Connection j = new Connection(hostAndPort, clientConfig)) {
              discoverClusterSlots(j);
              return;
            } catch (JedisException e) {
              // try next nodes
            }
          }
        }

        // Finally, we go back to the ShuffledNodesPool and try the remaining physical nodes.
        for (ConnectionPool jp : getShuffledNodesPool()) {
          try (Connection j = jp.getResource()) {
            // If already tried in startNodes, skip this node.
            if (startNodes != null && startNodes.contains(j.getHostAndPort())) {
              continue;
            }
            discoverClusterSlots(j);
            return;
          } catch (JedisException e) {
            // try next nodes
          }
        }

      } finally {
        rediscoverLock.unlock();
      }
    }
  }

  private void discoverClusterSlots(Connection jedis) {
    List<Object> slotsInfo = executeClusterSlots(jedis);
    if (System.getProperty(INIT_NO_ERROR_PROPERTY) == null) {
      if (slotsInfo.isEmpty()) {
        throw new JedisClusterOperationException("Cluster slots list is empty.");
      }
      if (!checkClusterSlotSequence(slotsInfo)) {
        throw new JedisClusterOperationException("Cluster slots have holes.");
      }
    }
    w.lock();
    try {
      Arrays.fill(slots, null);
      Arrays.fill(slotNodes, null);
      if (clientSideCache != null) {
        clientSideCache.clear();
      }
      Set<String> hostAndPortKeys = new HashSet<>();

      for (Object slotInfoObj : slotsInfo) {
        List<Object> slotInfo = (List<Object>) slotInfoObj;

        if (slotInfo.size() <= MASTER_NODE_INDEX) {
          continue;
        }

        List<Integer> slotNums = getAssignedSlotArray(slotInfo);

        int size = slotInfo.size();
        for (int i = MASTER_NODE_INDEX; i < size; i++) {
          List<Object> hostInfos = (List<Object>) slotInfo.get(i);
          if (hostInfos.isEmpty()) {
            continue;
          }

          HostAndPort targetNode = generateHostAndPort(hostInfos);
          hostAndPortKeys.add(getNodeKey(targetNode));
          setupNodeIfNotExist(targetNode);
          if (i == MASTER_NODE_INDEX) {
            assignSlotsToNode(slotNums, targetNode);
          }
        }
      }

      // Remove dead nodes according to the latest query
      Iterator<Entry<String, ConnectionPool>> entryIt = nodes.entrySet().iterator();
      while (entryIt.hasNext()) {
        Entry<String, ConnectionPool> entry = entryIt.next();
        if (!hostAndPortKeys.contains(entry.getKey())) {
          ConnectionPool pool = entry.getValue();
          try {
            if (pool != null) {
              pool.destroy();
            }
          } catch (Exception e) {
            // pass, may be this node dead
          }
          entryIt.remove();
        }
      }
    } finally {
      w.unlock();
    }
  }

  private HostAndPort generateHostAndPort(List<Object> hostInfos) {
    String host = SafeEncoder.encode((byte[]) hostInfos.get(0));
    int port = ((Long) hostInfos.get(1)).intValue();
    return new HostAndPort(host, port);
  }

  public ConnectionPool setupNodeIfNotExist(final HostAndPort node) {
    w.lock();
    try {
      String nodeKey = getNodeKey(node);
      ConnectionPool existingPool = nodes.get(nodeKey);
      if (existingPool != null) return existingPool;

      ConnectionPool nodePool = createNodePool(node);
      nodes.put(nodeKey, nodePool);
      return nodePool;
    } finally {
      w.unlock();
    }
  }

  private ConnectionPool createNodePool(HostAndPort node) {
    if (poolConfig == null) {
      if (clientSideCache == null) {
        return new ConnectionPool(node, clientConfig);
      } else {
        return new ConnectionPool(node, clientConfig, clientSideCache);
      }
    } else {
      if (clientSideCache == null) {
        return new ConnectionPool(node, clientConfig, poolConfig);
      } else {
        return new ConnectionPool(node, clientConfig, clientSideCache, poolConfig);
      }
    }
  }

  public void assignSlotToNode(int slot, HostAndPort targetNode) {
    w.lock();
    try {
      ConnectionPool targetPool = setupNodeIfNotExist(targetNode);
      slots[slot] = targetPool;
      slotNodes[slot] = targetNode;
    } finally {
      w.unlock();
    }
  }

  public void assignSlotsToNode(List<Integer> targetSlots, HostAndPort targetNode) {
    w.lock();
    try {
      ConnectionPool targetPool = setupNodeIfNotExist(targetNode);
      for (Integer slot : targetSlots) {
        slots[slot] = targetPool;
        slotNodes[slot] = targetNode;
      }
    } finally {
      w.unlock();
    }
  }

  public ConnectionPool getNode(String nodeKey) {
    r.lock();
    try {
      return nodes.get(nodeKey);
    } finally {
      r.unlock();
    }
  }

  public ConnectionPool getNode(HostAndPort node) {
    return getNode(getNodeKey(node));
  }

  public ConnectionPool getSlotPool(int slot) {
    r.lock();
    try {
      return slots[slot];
    } finally {
      r.unlock();
    }
  }

  public HostAndPort getSlotNode(int slot) {
    r.lock();
    try {
      return slotNodes[slot];
    } finally {
      r.unlock();
    }
  }

  public Map<String, ConnectionPool> getNodes() {
    r.lock();
    try {
      return new HashMap<>(nodes);
    } finally {
      r.unlock();
    }
  }

  public List<ConnectionPool> getShuffledNodesPool() {
    r.lock();
    try {
      List<ConnectionPool> pools = new ArrayList<>(nodes.values());
      Collections.shuffle(pools);
      return pools;
    } finally {
      r.unlock();
    }
  }

  /**
   * Clear discovered nodes collections and gently release allocated resources
   */
  public void reset() {
    w.lock();
    try {
      for (ConnectionPool pool : nodes.values()) {
        try {
          if (pool != null) {
            pool.destroy();
          }
        } catch (RuntimeException e) {
          // pass
        }
      }
      nodes.clear();
      Arrays.fill(slots, null);
      Arrays.fill(slotNodes, null);
    } finally {
      w.unlock();
    }
  }

  public void close() {
    reset();
    if (topologyRefreshExecutor != null) {
      logger.info("Cluster topology refresh shutdown, startNodes: {}", startNodes);
      topologyRefreshExecutor.shutdownNow();
    }
  }

  public static String getNodeKey(HostAndPort hnp) {
    //return hnp.getHost() + ":" + hnp.getPort();
    return hnp.toString();
  }

  private List<Object> executeClusterSlots(Connection jedis) {
    jedis.sendCommand(Protocol.Command.CLUSTER, "SLOTS");
    return jedis.getObjectMultiBulkReply();
  }

  private List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
    List<Integer> slotNums = new ArrayList<>();
    for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1))
        .intValue(); slot++) {
      slotNums.add(slot);
    }
    return slotNums;
  }
}
