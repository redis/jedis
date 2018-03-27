package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractJedisClusterInfoCache {
  public abstract JedisPool setupNodeIfNotExist(HostAndPort hostAndPort);

  public abstract Map<String, JedisPool> getNodes();

  public abstract Map<String, JedisPool> getNodes(ReadFrom readFrom);

  public abstract void discoverClusterNodesAndSlots(Jedis jedis);

  public abstract void renewClusterSlots(Jedis jedis);

  public abstract void reset();

  public abstract List<JedisPool> getShuffledNodesPool(ReadFrom readFrom);

  public abstract JedisPool getSlotPool(int slot, ReadFrom readFrom);

  public static String getNodeKey(HostAndPort hnp) {
    return hnp.getHost() + ":" + hnp.getPort();
  }

  public static String getNodeKey(Client client) {
    return client.getHost() + ":" + client.getPort();
  }

  public static String getNodeKey(Jedis jedis) {
    return getNodeKey(jedis.getClient());
  }

  protected HostAndPort generateHostAndPort(List<Object> hostInfos) {
    return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)),
        ((Long) hostInfos.get(1)).intValue());
  }

  protected List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
    List<Integer> slotNums = new ArrayList<Integer>();
    for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1))
        .intValue(); slot++) {
      slotNums.add(slot);
    }
    return slotNums;
  }
}
