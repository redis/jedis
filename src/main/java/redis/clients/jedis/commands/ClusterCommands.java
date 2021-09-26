package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.ClusterResetType;
import redis.clients.jedis.args.ClusterFailoverOption;

public interface ClusterCommands {

  String readonly();

  String readwrite();

  String clusterNodes();

  String clusterReplicas(String nodeId);

  String clusterMeet(String ip, int port);

  String clusterAddSlots(int... slots);

  String clusterDelSlots(int... slots);

  String clusterInfo();

  List<String> clusterGetKeysInSlot(int slot, int count);

  String clusterSetSlotNode(int slot, String nodeId);

  String clusterSetSlotMigrating(int slot, String nodeId);

  String clusterSetSlotImporting(int slot, String nodeId);

  String clusterSetSlotStable(int slot);

  String clusterForget(String nodeId);

  String clusterFlushSlots();

  long clusterKeySlot(String key);

  long clusterCountKeysInSlot(int slot);

  String clusterSaveConfig();

  String clusterReplicate(String nodeId);

  /**
   * {@code CLUSTER SLAVES} command is deprecated since Redis 5.
   * @deprecated Use {@link ClusterCommands#clusterReplicas(java.lang.String)}.
   */
  @Deprecated
  List<String> clusterSlaves(String nodeId);

  default String clusterFailover() {
    return clusterFailover(null);
  }

  String clusterFailover(ClusterFailoverOption failoverOption);

  List<Object> clusterSlots();

  /**
   * {@code resetType} can be null for default behavior.
   * @param resetType
   * @return OK
   */
  String clusterReset(ClusterResetType resetType);

  String clusterMyId();
}
