package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

public abstract class JedisSentinelSlaveInfoCacheAbstract {

  public abstract List<JedisPool> getSlaves();

  public abstract void discoverSlaves(Jedis jedis);

  public abstract void removeSlave(HostAndPort hnp);

  public abstract void reset();

  public abstract List<JedisPool> getShuffledSlavesPool(ReadFrom readFrom);

  public static String getSlaveKey(HostAndPort hnp) {
    return hnp.getHost() + ":" + hnp.getPort();
  }

  public static String getSlaveKey(Client client) {
    return client.getHost() + ":" + client.getPort();
  }

  public static String getSlaveKey(Jedis jedis) {
    return getSlaveKey(jedis.getClient());
  }

  protected HostAndPort generateHostAndPort(List<Object> hostInfos) {
    return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)),
        ((Long) hostInfos.get(1)).intValue());
  }
}
