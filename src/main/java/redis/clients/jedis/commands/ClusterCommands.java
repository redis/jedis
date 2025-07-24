package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.args.ClusterResetType;
import redis.clients.jedis.args.ClusterFailoverOption;
import redis.clients.jedis.resps.ClusterShardInfo;

public interface ClusterCommands {

  String asking();

  String readonly();

  String readwrite();

  String clusterNodes();

  String clusterMeet(String ip, int port);

  String clusterAddSlots(int... slots);

  String clusterDelSlots(int... slots);

  String clusterInfo();

  List<String> clusterGetKeysInSlot(int slot, int count);

  List<byte[]> clusterGetKeysInSlotBinary(int slot, int count);

  String clusterSetSlotNode(int slot, String nodeId);

  String clusterSetSlotMigrating(int slot, String nodeId);

  String clusterSetSlotImporting(int slot, String nodeId);

  String clusterSetSlotStable(int slot);

  String clusterForget(String nodeId);

  String clusterFlushSlots();

  long clusterKeySlot(String key);

  long clusterCountFailureReports(String nodeId);

  long clusterCountKeysInSlot(int slot);

  String clusterSaveConfig();

  /**
   * Set a specific config epoch in a fresh node. It only works when the nodes' table
   * of the node is empty or when the node current config epoch is zero.
   * @param configEpoch
   * @return OK
   */
  String clusterSetConfigEpoch(long configEpoch);

  /**
   * Advance the cluster config epoch.
   * @return BUMPED if the epoch was incremented, or STILL if the node already has the
   * greatest config epoch in the cluster.
   */
  String clusterBumpEpoch();

  String clusterReplicate(String nodeId);

  List<String> clusterReplicas(String nodeId);

  String clusterFailover();

  String clusterFailover(ClusterFailoverOption failoverOption);

  /**
   * {@code CLUSTER SHARDS} returns details about the shards of the cluster.
   * This command replaces the {@code CLUSTER SLOTS} command from Redis 7,
   * by providing a more efficient and extensible representation of the cluster.
   *
   * @return a list of shards, with each shard containing two objects, 'slots' and 'nodes'.
   * @see <a href="https://redis.io/commands/cluster-shards/">CLUSTER SHARDS</a>
   */
  List<ClusterShardInfo> clusterShards();

  String clusterReset();

  /**
   * {@code resetType} can be null for default behavior.
   *
   * @param resetType
   * @return OK
   */
  String clusterReset(ClusterResetType resetType);

  String clusterMyId();

  String clusterMyShardId();

  /**
   * return the information of all such peer links as an array, where each array element is a map that contains
   * attributes and their values for an individual link.
   *
   * @return the information of all such peer links as an array
   * @see <a href="https://redis.io/commands/cluster-links" >CLUSTET LINKS</a>
   */
  List<Map<String, Object>> clusterLinks();

  /**
   * Takes a list of slot ranges (specified by start and end slots) to assign to the node
   *
   * @param ranges slots range
   * @return OK if the command was successful. Otherwise, an error is returned.
   */
  String clusterAddSlotsRange(int... ranges);

  /**
   * Takes a list of slot ranges (specified by start and end slots) to remove to the node.
   *
   * @param ranges slots range
   * @return OK if the command was successful. Otherwise, an error is returned.
   */
  String clusterDelSlotsRange(int... ranges);
}
