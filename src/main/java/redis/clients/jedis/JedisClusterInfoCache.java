package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public class JedisClusterInfoCache {
  private final Map<String, JedisPool> masterNodes = new HashMap<String, JedisPool>();
  private final Map<Integer, JedisPool> masterSlots = new HashMap<Integer, JedisPool>();

  private final Map<String, JedisPool> slaveNodes = new HashMap<String, JedisPool>();
  private final Map<Integer, List<JedisPool>> slaveSlots = new HashMap<Integer, List<JedisPool>>();

  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();
  private volatile boolean rediscovering;
  private final GenericObjectPoolConfig poolConfig;

  private int connectionTimeout;
  private int soTimeout;
  private String password;
  private String clientName;

  private static final int MASTER_NODE_INDEX = 2;

  public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig, int timeout) {
    this(poolConfig, timeout, timeout, null, null);
  }

  public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig,
      final int connectionTimeout, final int soTimeout, final String password, final String clientName) {
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.clientName = clientName;
  }


  public void renewClusterSlots(Jedis jedis) {
    //If rediscovering is already in process - no need to start one more same rediscovering, just return
    if (!rediscovering) {
      try {
        w.lock();
        rediscovering = true;

        if (jedis != null) {
          try {
            discoverClusterNodesAndSlots(jedis);
            return;
          } catch (JedisException e) {
            //try nodes from all pools
          }
        }
        List<JedisPool> pools = getShuffledNodesPool(ReadFrom.BOTH);
        for (JedisPool jp : pools) {
          Jedis j = null;
          try {
            j = jp.getResource();
            discoverClusterNodesAndSlots(j);
            return;
          } catch (JedisConnectionException e) {
            // try next nodes
          } finally {
            if (j != null) {
              j.close();
            }
          }
        }
      } finally {
        rediscovering = false;
        w.unlock();
      }
    }
  }

  public void discoverClusterNodesAndSlots(Jedis jedis) {
    w.lock();

    try {
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
          if (hostInfos.isEmpty()) {
            continue;
          }

          HostAndPort targetNode = generateHostAndPort(hostInfos);
          setupNodeIfNotExist(targetNode, i == MASTER_NODE_INDEX);
          assignSlotsToNode(slotNums, targetNode, i == MASTER_NODE_INDEX);
        }
      }
    } finally {
      w.unlock();
    }
  }

  public JedisPool setupNodeIfNotExist(HostAndPort node, boolean isMaster) {
    w.lock();
    try {
      Map<String, JedisPool> targetNodesMap = isMaster ? masterNodes : slaveNodes;

      String nodeKey = getNodeKey(node);
      JedisPool existingPool = targetNodesMap.get(nodeKey);
      if (existingPool != null) {
        return existingPool;
      }

      JedisPool nodePool = new JedisPool(poolConfig, node.getHost(), node.getPort(),
              connectionTimeout, soTimeout, password, 0, clientName, false, null, null, null, !isMaster);
      targetNodesMap.put(nodeKey, nodePool);
      // Maybe we should destory the pool in the other map, should we?
      return nodePool;
    } finally {
      w.unlock();
    }
  }

  public void assignSlotsToNode(List<Integer> targetSlots, HostAndPort targetNode, boolean isMaster) {
    w.lock();
    try {
      JedisPool targetPool = setupNodeIfNotExist(targetNode, isMaster);
      for (Integer slot : targetSlots) {
        if (isMaster) {
          masterSlots.put(slot, targetPool);
        } else {
          List<JedisPool> pools = slaveSlots.get(slot);
          if (pools == null) {
            pools = new ArrayList<JedisPool>();
            slaveSlots.put(slot, pools);
          }
          if (!pools.contains(targetPool)) {
            pools.add(targetPool);
          }
        }
      }
    } finally {
      w.unlock();
    }
  }

  public JedisPool getNode(String nodeKey) {
    r.lock();
    try {
      return masterNodes.containsKey(nodeKey) ? masterNodes.get(nodeKey) : slaveNodes.get(nodeKey);
    } finally {
      r.unlock();
    }
  }

  public JedisPool getSlotPool(int slot) {
    return getSlotPool(slot, ReadFrom.MASTER);
  }

  public JedisPool getSlotPool(int slot, ReadFrom readFrom) {
    r.lock();
    try {
      // read from Master && has NO slave nodes
      if (ReadFrom.MASTER == readFrom || slaveSlots.get(slot) == null || slaveSlots.get(slot).isEmpty()) {
        return masterSlots.get(slot);
      }

      if (ReadFrom.SLAVE == readFrom) {
        if (slaveSlots.get(slot).size() == 1) {
          return slaveSlots.get(slot).get(0);
        } else if (slaveSlots.get(slot).size() > 1) {
          ArrayList<JedisPool> pools = new ArrayList<JedisPool>(slaveSlots.get(slot));
          Collections.shuffle(pools);
          return pools.get(0);
        }
      }

      if (ReadFrom.BOTH == readFrom) {
        ArrayList<JedisPool> pools = new ArrayList<JedisPool>(slaveSlots.get(slot));
        pools.add(masterSlots.get(slot));
        Collections.shuffle(pools);
        return pools.get(0);
      }

      // DEFAULT, this line should not be reached.
      return masterSlots.get(slot);
    } finally {
      r.unlock();
    }
  }

  public Map<String, JedisPool> getMasterNodes() {
    r.lock();
    try {
      return new HashMap<String, JedisPool>(masterNodes);
    } finally {
      r.unlock();
    }
  }

  public Map<String, JedisPool> getAllNodes() {
    r.lock();
    try {
      Map<String, JedisPool> nodes = new HashMap<String, JedisPool>(slaveNodes);
      nodes.putAll(masterNodes);
      return nodes;
    } finally {
      r.unlock();
    }
  }

  public List<JedisPool> getShuffledNodesPool(ReadFrom readFrom) {
    r.lock();
    try {
      Collection<JedisPool> targetList;
      if (ReadFrom.MASTER == readFrom) {
        targetList = masterNodes.values();
      } else if (ReadFrom.SLAVE == readFrom) {
        targetList = slaveNodes.values();
      } else {
        targetList = new ArrayList<JedisPool>(masterNodes.size() + slaveNodes.size());
        targetList.addAll(masterNodes.values());
        targetList.addAll(slaveNodes.values());
      }

      List<JedisPool> pools = new ArrayList<JedisPool>(targetList);
      Collections.shuffle(pools);
      return pools;
    } finally {
      r.unlock();
    }
  }

  /**
   * Clear discovered masterNodes collections and gently release allocated resources
   */
  public void reset() {
    w.lock();
    try {
      List<JedisPool> pools = new ArrayList<JedisPool>(masterNodes.size() + slaveNodes.size());
      pools.addAll(masterNodes.values());
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
      masterNodes.clear();
      masterSlots.clear();
      slaveNodes.clear();
      slaveSlots.clear();
    } finally {
      w.unlock();
    }
  }

  public static String getNodeKey(HostAndPort hnp) {
    return hnp.getHost() + ":" + hnp.getPort();
  }

  public static String getNodeKey(Client client) {
    return client.getHost() + ":" + client.getPort();
  }

  private HostAndPort generateHostAndPort(List<Object> hostInfos) {
    return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)),
            ((Long) hostInfos.get(1)).intValue());
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
