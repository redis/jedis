package redis.clients.jedis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

import static redis.clients.jedis.RedisClusterClient.INIT_NO_ERROR_PROPERTY;

@Internal
public class JedisClusterInfoCache {

  private static final Logger logger = LoggerFactory.getLogger(JedisClusterInfoCache.class);

  private final Map<String, ConnectionPool> nodes = new HashMap<>();
  private final Map<String, ConnectionPool> primaryNodes = new HashMap<>();
  private final ConnectionPool[] nodeBySlot = new ConnectionPool[Protocol.CLUSTER_HASHSLOTS];
  private final HostAndPort[] addressBySlot = new HostAndPort[Protocol.CLUSTER_HASHSLOTS];
  private final List<ConnectionPool>[] replicaNodesBySlot;

  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();
  private final ReentrantLock rediscoverLock = new ReentrantLock();

  /**
   * Guards slot-delta application. A full refresh holds it for its whole query→apply span (taken
   * after, and released before, {@link #rediscoverLock} — only the refresh ever holds both, in that
   * fixed order), so deltas can never interleave with — and always order after — a refresh, while a
   * running drain never makes a concurrent {@link #renewClusterSlots} attempt skip itself.
   */
  private final ReentrantLock slotDeltaLock = new ReentrantLock();

  private final ConcurrentLinkedQueue<List<SlotMigration>> pendingSlotDelta = new ConcurrentLinkedQueue<>();

  /** Deltas enqueued but not yet fully applied; drives {@link #hasPendingSlotDeltas()}. */
  private final AtomicInteger inFlightSlotDeltas = new AtomicInteger();

  private final GenericObjectPoolConfig<Connection> poolConfig;
  private final JedisClientConfig clientConfig;
  private final Cache clientSideCache;
  private final Set<HostAndPort> startNodes;
  /** The client-wide SMIGRATING/SMIGRATED coordinator; non-null iff cluster maintenance is on. */
  private final ClusterMaintenanceCoordinator maintenanceCoordinator;

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

  public JedisClusterInfoCache(final JedisClientConfig clientConfig,
      final Set<HostAndPort> startNodes) {
    this(clientConfig, null, null, startNodes);
  }

  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, Cache clientSideCache,
      final Set<HostAndPort> startNodes) {
    this(clientConfig, clientSideCache, null, startNodes);
  }

  public JedisClusterInfoCache(final JedisClientConfig clientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes) {
    this(clientConfig, null, poolConfig, startNodes);
  }

  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, Cache clientSideCache,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes) {
    this(clientConfig, clientSideCache, poolConfig, startNodes, null);
  }

  public JedisClusterInfoCache(final JedisClientConfig clientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes,
      final Duration topologyRefreshPeriod) {
    this(clientConfig, null, poolConfig, startNodes, topologyRefreshPeriod);
  }

  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, Cache clientSideCache,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes,
      final Duration topologyRefreshPeriod) {
    this(clientConfig, clientSideCache, poolConfig, startNodes, topologyRefreshPeriod, null);
  }

  /**
   * Creates the cache with cluster maintenance notifications configured for every node pool's
   * connections; a {@code null} or DISABLED config turns the feature off.
   * @since 8.1
   */
  @Experimental
  public JedisClusterInfoCache(final JedisClientConfig clientConfig, Cache clientSideCache,
      final GenericObjectPoolConfig<Connection> poolConfig, final Set<HostAndPort> startNodes,
      final Duration topologyRefreshPeriod, final MaintenanceNotificationsConfig maintConfig) {
    this.poolConfig = poolConfig;
    this.clientConfig = clientConfig;
    this.clientSideCache = clientSideCache;
    this.startNodes = startNodes;
    this.maintenanceCoordinator = maintConfig != null && maintConfig.isEnabledOrAuto()
        ? new ClusterMaintenanceCoordinator(this, maintConfig)
        : null;
    if (clientConfig.getAuthXManager() != null) {
      clientConfig.getAuthXManager().start();
    }
    if (topologyRefreshPeriod != null) {
      logger.info("Cluster topology refresh start, period: {}, startNodes: {}",
        topologyRefreshPeriod, startNodes);
      topologyRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
      topologyRefreshExecutor.scheduleWithFixedDelay(new TopologyRefreshTask(),
        topologyRefreshPeriod.toMillis(), topologyRefreshPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (clientConfig.isReadOnlyForRedisClusterReplicas()) {
      replicaNodesBySlot = new ArrayList[Protocol.CLUSTER_HASHSLOTS];
    } else {
      replicaNodesBySlot = null;
    }
  }

  /**
   * Check whether the number and order of slots in the cluster topology are equal to
   * CLUSTER_HASHSLOTS
   * @param slotsInfo the cluster topology
   * @return if slots is ok, return true, elese return false.
   */
  private boolean checkClusterSlotSequence(List<Object> slotsInfo) {
    List<Integer> slots = new ArrayList<>();
    for (Object slotInfoObj : slotsInfo) {
      List<Object> slotInfo = (List<Object>) slotInfoObj;
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
            primaryNodes.put(getNodeKey(targetNode), getNode(targetNode));
            assignSlotsToNode(slotNums, targetNode);
          } else if (clientConfig.isReadOnlyForRedisClusterReplicas()) {
            assignSlotsToReplicaNode(slotNums, targetNode);
          }
        }
      }
    } finally {
      w.unlock();
    }
  }

  public void renewClusterSlots(Connection jedis) {
    // If rediscovering is already in process - no need to start one more same rediscovering, just
    // return
    if (rediscoverLock.tryLock()) {
      try {
        // Exclude delta application for the whole query->apply span (blocking is fine: drains are
        // short and memory-only). Deltas queued meanwhile apply in the finally below, ordered
        // after the refreshed topology.
        slotDeltaLock.lock();
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
        // release before draining: a delta that lost its tryLock to this refresh is waiting in the
        // queue for exactly this pass
        slotDeltaLock.unlock();
        try {
          if (!pendingSlotDelta.isEmpty()) {
            drainSlotDeltas();
          }
        } catch (RuntimeException e) {
          // never mask an in-flight discovery exception; queued deltas would re-drain on the next
          // delta or refresh
          logger.warn("Applying queued slot deltas after refresh failed", e);
        } finally {
          rediscoverLock.unlock();
        }
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
      resetSlots();
      primaryNodes.clear();
      if (clientSideCache != null) {
        clientSideCache.flush();
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
            primaryNodes.put(getNodeKey(targetNode), getNode(targetNode));
            assignSlotsToNode(slotNums, targetNode);
          } else if (clientConfig.isReadOnlyForRedisClusterReplicas()) {
            assignSlotsToReplicaNode(slotNums, targetNode);
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
    GenericObjectPoolConfig<Connection> cfg = poolConfig != null ? poolConfig
        : new GenericObjectPoolConfig<>();
    // cluster-maintenance-aware pool: handshake visitor + coordinator listener per connection
    ClusterMaintenanceController controller = maintenanceCoordinator == null ? null
        : new ClusterMaintenanceController(maintenanceCoordinator);
    return new ConnectionPool(node, clientConfig, clientSideCache, cfg, controller);
  }

  public void assignSlotToNode(int slot, HostAndPort targetNode) {
    w.lock();
    try {
      ConnectionPool targetPool = setupNodeIfNotExist(targetNode);
      nodeBySlot[slot] = targetPool;
      addressBySlot[slot] = targetNode;
    } finally {
      w.unlock();
    }
  }

  public void assignSlotsToNode(List<Integer> targetSlots, HostAndPort targetNode) {
    w.lock();
    try {
      ConnectionPool targetPool = setupNodeIfNotExist(targetNode);
      for (Integer slot : targetSlots) {
        nodeBySlot[slot] = targetPool;
        addressBySlot[slot] = targetNode;
      }
    } finally {
      w.unlock();
    }
  }

  /**
   * Applies one SMIGRATED delta: reassigns its slots and drops the pools of its sources that are
   * left without any slot. Never blocks behind a running refresh — the delta is queued and the
   * refresh applies it, ordered after the fresh topology, before releasing {@link #slotDeltaLock}
   * to callers.
   */
  void applySlotMigration(List<SlotMigration> migrations) {
    // incremented before the enqueue and decremented only after application completes, so
    // hasPendingSlotDeltas() covers the delta's whole enqueue->applied lifecycle
    inFlightSlotDeltas.incrementAndGet();
    pendingSlotDelta.add(migrations);
    drainSlotDeltas();
  }

  boolean hasPendingSlotDeltas() {
    return inFlightSlotDeltas.get() > 0;
  }

  /**
   * Applies every queued delta under {@link #slotDeltaLock}, then drops the sources those deltas
   * emptied. A failed tryLock leaves the queue to the holder: a refresh drains after releasing,
   * another drain re-checks the queue after releasing, so nothing enqueued is ever skipped.
   */
  private void drainSlotDeltas() {
    do {
      if (!slotDeltaLock.tryLock()) {
        return;
      }
      try {
        List<SlotMigration> applied = new ArrayList<>();
        List<SlotMigration> delta;
        while ((delta = pendingSlotDelta.poll()) != null) {
          try {
            processSlotDelta(delta);
            applied.addAll(delta);
          } finally {
            inFlightSlotDeltas.decrementAndGet();
          }
        }
        removeDepartedSources(applied);
      } finally {
        slotDeltaLock.unlock();
      }
    } while (!pendingSlotDelta.isEmpty());
  }

  private void processSlotDelta(List<SlotMigration> migrations) {
    w.lock();
    try {
      for (SlotMigration migration : migrations) {
        ConnectionPool destPool = setupNodeIfNotExist(migration.dest);
        primaryNodes.put(getNodeKey(migration.dest), destPool);
        migration.slots.forEachSlot(slot -> {
          nodeBySlot[slot] = destPool;
          addressBySlot[slot] = migration.dest;
        });
      }
      if (clientSideCache != null) {
        clientSideCache.flush();
      }
      primaryNodes.keySet().retainAll(getOwnerKeys());
    } finally {
      w.unlock();
    }
  }

  /**
   * Forgets and destroys the source nodes of the applied deltas that neither own a primary slot nor
   * serve a replica slot anymore. Only those sources are candidates: any other node left without
   * slots is the refresh's to sweep.
   */
  private void removeDepartedSources(List<SlotMigration> migrations) {
    if (migrations.isEmpty()) {
      return;
    }
    List<ConnectionPool> removedPools = new ArrayList<>();
    Set<HostAndPort> removedAddresses = new HashSet<>();
    w.lock();
    try {
      Set<String> ownerKeys = getOwnerKeys();
      Set<ConnectionPool> serving = replicaPoolsInUse();
      for (SlotMigration migration : migrations) {
        String sourceKey = getNodeKey(migration.src);
        ConnectionPool sourcePool = nodes.get(sourceKey);
        if (sourcePool == null || ownerKeys.contains(sourceKey) || serving.contains(sourcePool)) {
          continue;
        }
        nodes.remove(sourceKey);
        removedPools.add(sourcePool);
        removedAddresses.add(migration.src);
      }
    } finally {
      w.unlock();
    }
    for (ConnectionPool pool : removedPools) {
      try {
        pool.destroy();
      } catch (RuntimeException e) {
        logger.debug("Destroying the pool of a departed node failed", e);
      }
    }
    if (!removedAddresses.isEmpty()) {
      logger.debug("Removed cluster nodes departed by slot migration: {}", removedAddresses);
    }
  }

  /** Keys of the nodes owning at least one primary slot; call under {@link #rwl}. */
  private Set<String> getOwnerKeys() {
    Set<String> keys = new HashSet<>();
    HostAndPort previousOwner = null;
    for (HostAndPort owner : addressBySlot) {
      if (owner != null && owner != previousOwner) { // ranges repeat the same reference
        keys.add(getNodeKey(owner));
        previousOwner = owner;
      }
    }
    return keys;
  }

  /** Pools serving at least one replica slot (identity set); call under {@link #rwl}. */
  private Set<ConnectionPool> replicaPoolsInUse() {
    Set<ConnectionPool> pools = Collections.newSetFromMap(new IdentityHashMap<>());
    if (replicaNodesBySlot != null) {
      for (List<ConnectionPool> slotReplicas : replicaNodesBySlot) {
        if (slotReplicas != null) {
          pools.addAll(slotReplicas);
        }
      }
    }
    return pools;
  }

  public void assignSlotsToReplicaNode(List<Integer> targetSlots, HostAndPort targetNode) {
    w.lock();
    try {
      ConnectionPool targetPool = setupNodeIfNotExist(targetNode);
      for (Integer slot : targetSlots) {
        if (replicaNodesBySlot[slot] == null) {
          replicaNodesBySlot[slot] = new ArrayList<>();
        }
        replicaNodesBySlot[slot].add(targetPool);
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
      return nodeBySlot[slot];
    } finally {
      r.unlock();
    }
  }

  public HostAndPort getSlotNode(int slot) {
    r.lock();
    try {
      return addressBySlot[slot];
    } finally {
      r.unlock();
    }
  }

  public List<ConnectionPool> getSlotReplicaPools(int slot) {
    r.lock();
    try {
      return replicaNodesBySlot[slot];
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

  public Map<String, ConnectionPool> getPrimaryNodes() {
    r.lock();
    try {
      return new HashMap<>(primaryNodes);
    } finally {
      r.unlock();
    }
  }

  public List<ConnectionPool> getShuffledPrimaryNodesPool() {
    r.lock();
    try {
      List<ConnectionPool> pools = new ArrayList<>(primaryNodes.values());
      Collections.shuffle(pools);
      return pools;
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
      resetNodes();
      resetSlots();
    } finally {
      w.unlock();
    }
  }

  private void resetSlots() {
    Arrays.fill(nodeBySlot, null);
    Arrays.fill(addressBySlot, null);
    resetReplicaSlots();
  }

  private void resetReplicaSlots() {
    if (replicaNodesBySlot == null) {
      return;
    }

    Arrays.stream(replicaNodesBySlot).filter(Objects::nonNull).forEach(List::clear);
    Arrays.fill(replicaNodesBySlot, null);
  }

  private void resetNodes() {
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
    primaryNodes.clear();
  }

  public void close() {
    reset();
    if (topologyRefreshExecutor != null) {
      logger.info("Cluster topology refresh shutdown, startNodes: {}", startNodes);
      topologyRefreshExecutor.shutdownNow();
    }
  }

  public static String getNodeKey(HostAndPort hnp) {
    return hnp.toString();
  }

  @SuppressWarnings("unchecked")
  private List<Object> executeClusterSlots(Connection jedis) {
    CommandArguments clusterSlotsCmd = new CommandArguments(Protocol.Command.CLUSTER).add("SLOTS");
    return (List<Object>) jedis.executeCommand(clusterSlotsCmd);
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
