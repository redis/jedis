package redis.clients.jedis;

import java.util.List;

import redis.clients.jedis.JedisCluster.Reset;

public interface ClusterCommands {
  String clusterNodes();

  String clusterMeet(final String ip, final int port);

  String clusterAddSlots(final int... slots);

  String clusterDelSlots(final int... slots);

  String clusterInfo();

  List<String> clusterGetKeysInSlot(final int slot, final int count);

  String clusterSetSlotNode(final int slot, final String nodeId);

  String clusterSetSlotMigrating(final int slot, final String nodeId);

  String clusterSetSlotImporting(final int slot, final String nodeId);

  String clusterSetSlotStable(final int slot);

  String clusterForget(final String nodeId);

  String clusterFlushSlots();

  Long clusterKeySlot(final String key);

  Long clusterCountKeysInSlot(final int slot);

  String clusterSaveConfig();

  String clusterReplicate(final String nodeId);

  List<String> clusterSlaves(final String nodeId);

  String clusterFailover();

  List<Object> clusterSlots();

  String clusterReset(Reset resetType);
}
