package redis.clients.jedis.resps;

import java.util.List;
import java.util.Map;

/**
 * This class holds information about a shard of the cluster with command {@code CLUSTER SHARDS}.
 * They can be accessed via getters. There is also {@link ClusterShardInfo#getClusterShardInfo()}
 * method that returns a generic {@link Map} in case more info are returned from the server.
 */
public class ClusterShardInfo {

  public static final String SLOTS = "slots";
  public static final String NODES = "nodes";

  private final List<List<Long>> slots;
  private final List<ClusterShardNodeInfo> nodes;

  private final Map<String, Object> clusterShardInfo;

  /**
   * @param map contains key-value pairs with cluster shard info
   */
  @SuppressWarnings("unchecked")
  public ClusterShardInfo(Map<String, Object> map) {
    slots = (List<List<Long>>) map.get(SLOTS);
    nodes = (List<ClusterShardNodeInfo>) map.get(NODES);

    clusterShardInfo = map;
  }

  public List<List<Long>> getSlots() {
    return slots;
  }

  public List<ClusterShardNodeInfo> getNodes() {
    return nodes;
  }

  public Map<String, Object> getClusterShardInfo() {
    return clusterShardInfo;
  }

}
