package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.Protocol.Command.SET;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.util.Hashing;

public class ShardingTest {

  private static final HostAndPort redis1 = HostAndPorts.getRedisServers().get(0);
  private static final HostAndPort redis2 = HostAndPorts.getRedisServers().get(1);

  private JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password("foobared").build();

  @Before
  public void setUp() {
    try (Jedis j = new Jedis(redis1, clientConfig)) {
      j.flushAll();
    }
    try (Jedis j = new Jedis(redis2, clientConfig)) {
      j.flushAll();
    }
  }

  @Test
  public void trySharding() {
    List<HostAndPort> shards = new ArrayList<>();
    shards.add(redis1);
    shards.add(redis2);
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig)) {
      for (int i = 0; i < 1000; i++) {
        jedis.set("key" + i, Integer.toString(i));
      }
    }

    long totalDbSize = 0;
    try (Jedis j = new Jedis(redis1)) {
      j.auth("foobared");
      long dbSize = j.dbSize();
      assertTrue(dbSize > 400);
      totalDbSize += dbSize;
    }
    try (Jedis j = new Jedis(redis2)) {
      j.auth("foobared");
      long dbSize = j.dbSize();
      assertTrue(dbSize > 400);
      totalDbSize += dbSize;
    }
    assertEquals(1000, totalDbSize);
  }

  @Test
  public void tryShardingWithMurmur() {
    List<HostAndPort> shards = new ArrayList<>();
    shards.add(redis1);
    shards.add(redis2);
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig, Hashing.MURMUR_HASH)) {
      for (int i = 0; i < 1000; i++) {
        jedis.set("key" + i, Integer.toString(i));
      }
    }

    long totalDbSize = 0;
    try (Jedis j = new Jedis(redis1)) {
      j.auth("foobared");
      long dbSize = j.dbSize();
      assertTrue(dbSize > 400);
      totalDbSize += dbSize;
    }
    try (Jedis j = new Jedis(redis2)) {
      j.auth("foobared");
      long dbSize = j.dbSize();
      assertTrue(dbSize > 400);
      totalDbSize += dbSize;
    }
    assertEquals(1000, totalDbSize);
  }

  @Test
  public void tryShardingWithMD5() {
    List<HostAndPort> shards = new ArrayList<>();
    shards.add(redis1);
    shards.add(redis2);
    try (JedisSharding jedis = new JedisSharding(shards, clientConfig, Hashing.MD5)) {
      for (int i = 0; i < 1000; i++) {
        jedis.set("key" + i, Integer.toString(i));
      }
    }

    long totalDbSize = 0;
    try (Jedis j = new Jedis(redis1)) {
      j.auth("foobared");
      long dbSize = j.dbSize();
      assertTrue(dbSize > 400);
      totalDbSize += dbSize;
    }
    try (Jedis j = new Jedis(redis2)) {
      j.auth("foobared");
      long dbSize = j.dbSize();
      totalDbSize += dbSize;
    }
    assertEquals(1000, totalDbSize);
  }

  @Test
  public void checkKeyTags() {
    assertNotNull(((ShardedCommandArguments) new ShardedCommandArguments(Hashing.MURMUR_HASH, SET).key("bar")).getKeyHash());
    assertNotNull(((ShardedCommandArguments) new ShardedCommandArguments(Hashing.MD5, SET).key("bar")).getKeyHash());
    assertEquals(((ShardedCommandArguments) new ShardedCommandArguments(Hashing.MURMUR_HASH,
        JedisSharding.DEFAULT_KEY_TAG_PATTERN, SET).key("bar")).getKeyHash(),
        ((ShardedCommandArguments) new ShardedCommandArguments(Hashing.MURMUR_HASH,
            JedisSharding.DEFAULT_KEY_TAG_PATTERN, SET).key("foo{bar}")).getKeyHash());
    assertEquals(((ShardedCommandArguments) new ShardedCommandArguments(Hashing.MD5, JedisSharding.DEFAULT_KEY_TAG_PATTERN, SET).key("bar")).getKeyHash(),
        ((ShardedCommandArguments) new ShardedCommandArguments(Hashing.MD5, JedisSharding.DEFAULT_KEY_TAG_PATTERN, SET).key("foo{bar}")).getKeyHash());
  }

  @Test
  public void checkCloseable() {
    List<HostAndPort> shards = new ArrayList<>();
    shards.add(redis1);
    shards.add(redis2);

    JedisSharding jedis = new JedisSharding(shards, clientConfig);
    jedis.set("closeable", "true");
    assertEquals("true", jedis.get("closeable"));
    jedis.close();
    try {
      jedis.get("closeable");
      fail();
    } catch (Exception ex) {
    }
  }

  @Test
  public void testGeneralCommand() {
    List<HostAndPort> shards = new ArrayList<>();
    shards.add(redis1);
    shards.add(redis2);

    try (JedisSharding jedis = new JedisSharding(shards, clientConfig)) {
      jedis.sendCommand("command", SET, "command", "general");
      assertEquals("general", jedis.get("command"));
    }
  }
}