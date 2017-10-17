package redis.clients.jedis.commands;

import redis.clients.jedis.Response;

import java.util.List;

public interface ClusterPipeline {
  Response<String> clusterNodes();

  Response<String> clusterMeet(String ip, int port);

  Response<String> clusterAddSlots(int... slots);

  Response<String> clusterDelSlots(int... slots);

  Response<String> clusterInfo();

  Response<List<String>> clusterGetKeysInSlot(int slot, int count);

  Response<String> clusterSetSlotNode(int slot, String nodeId);

  Response<String> clusterSetSlotMigrating(int slot, String nodeId);

  Response<String> clusterSetSlotImporting(int slot, String nodeId);
}
