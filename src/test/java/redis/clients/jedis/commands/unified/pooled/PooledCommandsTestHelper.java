package redis.clients.jedis.commands.unified.pooled;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.HostAndPorts;

public class PooledCommandsTestHelper {

  private static final HostAndPort nodeInfo = HostAndPorts.getRedisServers().get(0);

  private static Jedis node;

  static JedisPooled getPooled() throws InterruptedException {

    node = new Jedis(nodeInfo);
    node.auth("foobared");
    node.flushAll();

    //return new JedisPooled(nodeInfo.getHost(), nodeInfo.getPort(), null, "foobared");
    return new JedisPooled(nodeInfo, DefaultJedisClientConfig.builder().resp3().password("foobared").build());
  }

  static void clearData() {
    node.flushDB();
  }
}
