package redis.clients.jedis.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.Hashing;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Sharded;

public class ShardedJedisTest extends Assert {
    private static HostAndPort redis1 = HostAndPortUtil.getRedisServers()
            .get(0);
    private static HostAndPort redis2 = HostAndPortUtil.getRedisServers()
            .get(1);

    @Test
    public void checkSharding() throws IOException {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.host, redis1.port));
        shards.add(new JedisShardInfo(redis2.host, redis2.port));
        ShardedJedis jedis = new ShardedJedis(shards);
        JedisShardInfo s1 = jedis.getShardInfo("a1");
        JedisShardInfo s2 = jedis.getShardInfo("b2");
        assertNotSame(s1, s2);
    }

    @Test
    public void trySharding() throws IOException {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo(redis1.host, redis1.port);
        si.setPassword("foobared");
        shards.add(si);
        si = new JedisShardInfo(redis2.host, redis2.port);
        si.setPassword("foobared");
        shards.add(si);
        ShardedJedis jedis = new ShardedJedis(shards);
        jedis.set("a", "bar");
        JedisShardInfo s1 = jedis.getShardInfo("a");
        jedis.set("b", "bar1");
        JedisShardInfo s2 = jedis.getShardInfo("b");
        jedis.disconnect();

        Jedis j = new Jedis(s1.getHost(), s1.getPort());
        j.auth("foobared");
        assertEquals("bar", j.get("a"));
        j.disconnect();

        j = new Jedis(s2.getHost(), s2.getPort());
        j.auth("foobared");
        assertEquals("bar1", j.get("b"));
        j.disconnect();
    }

    @Test
    public void tryShardingWithMurmure() throws IOException {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo(redis1.host, redis1.port);
        si.setPassword("foobared");
        shards.add(si);
        si = new JedisShardInfo(redis2.host, redis2.port);
        si.setPassword("foobared");
        shards.add(si);
        ShardedJedis jedis = new ShardedJedis(shards, Hashing.MURMUR_HASH);
        jedis.set("a", "bar");
        JedisShardInfo s1 = jedis.getShardInfo("a");
        jedis.set("b", "bar1");
        JedisShardInfo s2 = jedis.getShardInfo("b");
        jedis.disconnect();

        Jedis j = new Jedis(s1.getHost(), s1.getPort());
        j.auth("foobared");
        assertEquals("bar", j.get("a"));
        j.disconnect();

        j = new Jedis(s2.getHost(), s2.getPort());
        j.auth("foobared");
        assertEquals("bar1", j.get("b"));
        j.disconnect();
    }

    @Test
    public void checkKeyTags() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.host, redis1.port));
        shards.add(new JedisShardInfo(redis2.host, redis2.port));
        ShardedJedis jedis = new ShardedJedis(shards,
                ShardedJedis.DEFAULT_KEY_TAG_PATTERN);

        assertEquals(jedis.getKeyTag("foo"), "foo");
        assertEquals(jedis.getKeyTag("foo{bar}"), "bar");
        assertEquals(jedis.getKeyTag("foo{bar}}"), "bar"); // default pattern is
        // non greedy
        assertEquals(jedis.getKeyTag("{bar}foo"), "bar"); // Key tag may appear
        // anywhere
        assertEquals(jedis.getKeyTag("f{bar}oo"), "bar"); // Key tag may appear
        // anywhere

        JedisShardInfo s1 = jedis.getShardInfo("abc{bar}");
        JedisShardInfo s2 = jedis.getShardInfo("foo{bar}");
        assertSame(s1, s2);

        JedisShardInfo s3 = jedis.getShardInfo("a112");
        JedisShardInfo s4 = jedis.getShardInfo("b112");
        assertNotSame(s3, s4);

        ShardedJedis jedis2 = new ShardedJedis(shards);

        assertEquals(jedis2.getKeyTag("foo"), "foo");
        assertNotSame(jedis2.getKeyTag("foo{bar}"), "bar");

        JedisShardInfo s5 = jedis2.getShardInfo("foo{bar}");
        JedisShardInfo s6 = jedis2.getShardInfo("abc{bar}");
        assertNotSame(s5, s6);
    }

    @Test
    public void shardedPipeline() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.host, redis1.port));
        shards.add(new JedisShardInfo(redis2.host, redis2.port));
        shards.get(0).setPassword("foobared");
        shards.get(1).setPassword("foobared");
        ShardedJedis jedis = new ShardedJedis(shards);

        jedis.set("a112", "a");
        jedis.set("b112", "b");

        assertNotSame(jedis.getShard("a112"), jedis.getShard("b112"));

        List<Object> results = jedis.pipelined(new ShardedJedisPipeline() {
            public void execute() {
                get("a112");
                get("b112");
            }
        });

        List<Object> expected = new ArrayList<Object>(2);
        expected.add(SafeEncoder.encode("a"));
        expected.add(SafeEncoder.encode("b"));

        assertEquals(2, results.size());
        assertArrayEquals(SafeEncoder.encode("a"), (byte[]) results.get(0));
        assertArrayEquals(SafeEncoder.encode("b"), (byte[]) results.get(1));
    }

    @Test
    public void testMD5Sharding() throws Exception {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2));
        Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(
                shards, Hashing.MD5);
        int shard_6379 = 0;
        int shard_6380 = 0;
        int shard_6381 = 0;
        for (int i = 0; i < 1000; i++) {
            JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer
                    .toString(i));
            switch (jedisShardInfo.getPort()) {
            case 6379:
                shard_6379++;
                break;
            case 6380:
                shard_6380++;
                break;
            case 6381:
                shard_6381++;
                break;
            default:
                fail("Attempting to use a non-defined shard!!:"
                        + jedisShardInfo);
                break;
            }
        }
        assertTrue(shard_6379 > 300 && shard_6379 < 400);
        assertTrue(shard_6380 > 300 && shard_6380 < 400);
        assertTrue(shard_6381 > 300 && shard_6381 < 400);
    }

    @Test
    public void testMurmurSharding() throws Exception {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2));
        Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(
                shards, Hashing.MURMUR_HASH);
        int shard_6379 = 0;
        int shard_6380 = 0;
        int shard_6381 = 0;
        for (int i = 0; i < 1000; i++) {
            JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer
                    .toString(i));
            switch (jedisShardInfo.getPort()) {
            case 6379:
                shard_6379++;
                break;
            case 6380:
                shard_6380++;
                break;
            case 6381:
                shard_6381++;
                break;
            default:
                fail("Attempting to use a non-defined shard!!:"
                        + jedisShardInfo);
                break;
            }
        }
        assertTrue(shard_6379 > 300 && shard_6379 < 400);
        assertTrue(shard_6380 > 300 && shard_6380 < 400);
        assertTrue(shard_6381 > 300 && shard_6381 < 400);
    }
}