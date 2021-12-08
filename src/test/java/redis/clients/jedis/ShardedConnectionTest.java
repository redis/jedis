package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

public class ShardedConnectionTest {

  private static final HostAndPort redis1 = HostAndPorts.getRedisServers().get(0);
  private static final HostAndPort redis2 = HostAndPorts.getRedisServers().get(1);

  private List<HostAndPort> shards;
  private JedisClientConfig clientConfig;

  @Before
  public void startUp() {
    shards = new ArrayList<>();
    shards.add(redis1);
    shards.add(redis2);

    clientConfig = DefaultJedisClientConfig.builder().password("foobared").build();

    for (HostAndPort shard : shards) {
      try (Jedis j = new Jedis(shard, clientConfig)) {
        j.flushAll();
      }
    }
  }

  @Test
  public void checkConnections() {
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void checkPoolWhenJedisIsBroken() {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig, poolConfig)) {
      jedis.sendCommand(Protocol.Command.QUIT);
      jedis.incr("foo");
    } catch (JedisConnectionException jce) {
    }
  }

  @Test
  public void checkPoolTestOnBorrowWhenJedisIsBroken() {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    poolConfig.setTestOnBorrow(true);
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig, poolConfig)) {
      jedis.sendCommand(Protocol.Command.QUIT);
      jedis.incr("foo");
    }
  }

  @Test
  public void checkPoolTestOnReturnWhenJedisIsBroken() {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    poolConfig.setTestOnReturn(true);
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig, poolConfig)) {
      jedis.sendCommand(Protocol.Command.QUIT);
      jedis.incr("foo");
    }
  }

  @Test
  public void checkFailedJedisServer() {
    try (JedisSharding jedis = new JedisSharding(shards)) {
      try {
        jedis.incr("foo");
        fail("Should get NOAUTH error.");
      } catch (JedisDataException jde) {
        assertEquals("NOAUTH Authentication required.", jde.getMessage());
      }
    }
  }

  @Test
  public void checkResourceIsCloseable() throws URISyntaxException {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    poolConfig.setBlockWhenExhausted(false);

    try (JedisSharding jedis = new JedisSharding(shards, clientConfig, poolConfig)) {
      jedis.set("hello", "jedis");
    }
  }
}
