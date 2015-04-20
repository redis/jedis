package redis.clients.jedis;

import java.util.List;

public interface ClusterPipeline {
  Response<String> clusterNodes();

  Response<String> clusterMeet(final String ip, final int port);

  Response<String> clusterAddSlots(final int... slots);

  Response<String> clusterDelSlots(final int... slots);

  Response<String> clusterInfo();

  Response<List<String>> clusterGetKeysInSlot(final int slot, final int count);

  Response<String> clusterSetSlotNode(final int slot, final String nodeId);

  Response<String> clusterSetSlotMigrating(final int slot, final String nodeId);

  Response<String> clusterSetSlotImporting(final int slot, final String nodeId);
}
