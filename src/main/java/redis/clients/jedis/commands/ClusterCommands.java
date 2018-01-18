package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.ClusterReset;

public interface ClusterCommands {
  String clusterNodes();

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

  Long clusterKeySlot(String key);

  Long clusterCountKeysInSlot(int slot);

  String clusterSaveConfig();

  String clusterReplicate(String nodeId);

  List<String> clusterSlaves(String nodeId);

  String clusterFailover();

  List<Object> clusterSlots();

  String clusterReset(ClusterReset resetType);

  String readonly();
}
