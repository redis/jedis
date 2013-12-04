package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.Tuple;
import redis.clients.util.Hashing;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Sharded;

public class ShardedJedisTest extends Assert {
    private static HostAndPort redis1 = HostAndPortUtil.getRedisServers()
            .get(0);
    private static HostAndPort redis2 = HostAndPortUtil.getRedisServers()
            .get(1);

    private List<String> getKeysDifferentShard(ShardedJedis jedis) {
        List<String> ret = new ArrayList<String>();
        JedisShardInfo first = jedis.getShardInfo("a0");
        ret.add("a0");
        for (int i = 1; i < 100; ++i) {
            JedisShardInfo actual = jedis.getShardInfo("a" + i);
            if (actual != first) {
                ret.add("a" + i);
                break;

            }

        }
        return ret;
    }
    
    @After
    public void tearDown() {
    	try {
	    	Jedis jedis = new Jedis(redis1.getHost(), redis1.getPort());
	    	jedis.flushDB();
	    	jedis.disconnect();
	    	
	    	jedis = new Jedis(redis2.getHost(), redis2.getPort());
	    	jedis.flushDB();
	    	jedis.disconnect();
    	} catch (Exception e) {
    		// pass
    	}
    }
    
    @Test
    public void checkSharding() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
        shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
        ShardedJedis jedis = new ShardedJedis(shards);
        List<String> keys = getKeysDifferentShard(jedis);
        JedisShardInfo s1 = jedis.getShardInfo(keys.get(0));
        JedisShardInfo s2 = jedis.getShardInfo(keys.get(1));
        assertNotSame(s1, s2);
    }

    @Test
    public void trySharding() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo(redis1.getHost(), redis1.getPort());
        si.setPassword("foobared");
        shards.add(si);
        si = new JedisShardInfo(redis2.getHost(), redis2.getPort());
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
    public void tryShardingWithMurmure() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo(redis1.getHost(), redis1.getPort());
        si.setPassword("foobared");
        shards.add(si);
        si = new JedisShardInfo(redis2.getHost(), redis2.getPort());
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
        shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
        shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
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

        List<String> keys = getKeysDifferentShard(jedis);
        JedisShardInfo s3 = jedis.getShardInfo(keys.get(0));
        JedisShardInfo s4 = jedis.getShardInfo(keys.get(1));
        assertNotSame(s3, s4);

        ShardedJedis jedis2 = new ShardedJedis(shards);

        assertEquals(jedis2.getKeyTag("foo"), "foo");
        assertNotSame(jedis2.getKeyTag("foo{bar}"), "bar");

        JedisShardInfo s5 = jedis2.getShardInfo(keys.get(0) + "{bar}");
        JedisShardInfo s6 = jedis2.getShardInfo(keys.get(1) + "{bar}");
        assertNotSame(s5, s6);
    }

    @Test
    public void shardedPipeline() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
        shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
        shards.get(0).setPassword("foobared");
        shards.get(1).setPassword("foobared");
        ShardedJedis jedis = new ShardedJedis(shards);

        final List<String> keys = getKeysDifferentShard(jedis);
        jedis.set(keys.get(0), "a");
        jedis.set(keys.get(1), "b");

        assertNotSame(jedis.getShard(keys.get(0)), jedis.getShard(keys.get(1)));

        List<Object> results = jedis.pipelined(new ShardedJedisPipeline() {
            public void execute() {
                get(keys.get(0));
                get(keys.get(1));
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
    public void testMD5Sharding() {
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
    public void testMurmurSharding() {
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

    @Test
    public void testMasterSlaveShardingConsistency() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2));
        Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(
                shards, Hashing.MURMUR_HASH);

        List<JedisShardInfo> otherShards = new ArrayList<JedisShardInfo>(3);
        otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT));
        otherShards.add(new JedisShardInfo("otherhost",
                Protocol.DEFAULT_PORT + 1));
        otherShards.add(new JedisShardInfo("otherhost",
                Protocol.DEFAULT_PORT + 2));
        Sharded<Jedis, JedisShardInfo> sharded2 = new Sharded<Jedis, JedisShardInfo>(
                otherShards, Hashing.MURMUR_HASH);

        for (int i = 0; i < 1000; i++) {
            JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer
                    .toString(i));
            JedisShardInfo jedisShardInfo2 = sharded2.getShardInfo(Integer
                    .toString(i));
            assertEquals(shards.indexOf(jedisShardInfo), otherShards
                    .indexOf(jedisShardInfo2));
        }

    }

    @Test
    public void testMasterSlaveShardingConsistencyWithShardNaming() {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT,
                "HOST1:1234"));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1,
                "HOST2:1234"));
        shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2,
                "HOST3:1234"));
        Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(
                shards, Hashing.MURMUR_HASH);

        List<JedisShardInfo> otherShards = new ArrayList<JedisShardInfo>(3);
        otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT,
                "HOST2:1234"));
        otherShards.add(new JedisShardInfo("otherhost",
                Protocol.DEFAULT_PORT + 1, "HOST3:1234"));
        otherShards.add(new JedisShardInfo("otherhost",
                Protocol.DEFAULT_PORT + 2, "HOST1:1234"));
        Sharded<Jedis, JedisShardInfo> sharded2 = new Sharded<Jedis, JedisShardInfo>(
                otherShards, Hashing.MURMUR_HASH);

        for (int i = 0; i < 1000; i++) {
            JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer
                    .toString(i));
            JedisShardInfo jedisShardInfo2 = sharded2.getShardInfo(Integer
                    .toString(i));
            assertEquals(jedisShardInfo.getName(), jedisShardInfo2.getName());
        }
    }
    
    @Test
    public void testZScanWithParams() {
    	List<JedisShardInfo> shards = setupBasicShard();
    	
        ShardedJedis jedis = null;
        try {
        	jedis = new ShardedJedis(shards);
        	
	        // since shard algorithm is based on key, the key must exists only one shard
	        String key = "test_zscan";
	        
	        jedis.zadd(key, 1.0, "a");
	        jedis.zadd(key, 1.5, "ab");
	        jedis.zadd(key, 2.0, "c");
	        
	        ScanParams params = new ScanParams();
	        params.match("a*");
	        ScanResult<Tuple> result = jedis.zscan(key, 0, params);
	        List<Tuple> keyValues = result.getResult();
	        assertTrue(keyValues.size() > 0);
	        for (Tuple keyValue : keyValues)
	        	assertNotEquals(keyValue.getElement(), "c");
        } finally {
        	if (null != jedis)
        		jedis.disconnect();
        }
    }
    
    @Test
    public void testSScanWithParams() {
    	List<JedisShardInfo> shards = setupBasicShard();
    	
    	ShardedJedis jedis = null;
    	try {
    		jedis = new ShardedJedis(shards);
        	
	        // since shard algorithm is based on key, the key must exists only one shard
	        String key = "test_sscan";
	        
	        jedis.sadd(key, "a");
	        jedis.sadd(key, "ab");
	        jedis.sadd(key, "c");
	        
	        ScanParams params = new ScanParams();
	        params.match("a*");
	        ScanResult<String> result = jedis.sscan(key, 0, params);
	        List<String> values = result.getResult();
	        assertTrue(values.size() > 0);
	        for (String value : values)
	        	assertNotEquals(value, "c");
    	} finally {
    		if (null != jedis)
        		jedis.disconnect();
    	}
    }
    
    @Test
    public void testHScanWithParams() {
    	List<JedisShardInfo> shards = setupBasicShard();
    	
    	ShardedJedis jedis = null;
    	try {
    		jedis = new ShardedJedis(shards);
        	
	        // since shard algorithm is based on key, the key must exists only one shard
	        String key = "test_hscan";
	        
	        jedis.hset(key, "a", "b");
	        jedis.hset(key, "ab", "bc");
	        jedis.hset(key, "c", "d");
	        
	        ScanParams params = new ScanParams();
	        params.match("a*");
	        ScanResult<Entry<String,String>> result = jedis.hscan(key, 0, params);
	        List<Entry<String,String>> entries = result.getResult();
	        assertTrue(entries.size() > 0);
	        for (Entry<String, String> entry : entries) {
	        	assertNotEquals(entry.getKey(), "c");
	        }
    	} finally {
    		if (null != jedis)
        		jedis.disconnect();
    	}
    }
    
    private List<JedisShardInfo> setupBasicShard() {
    	List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    	
    	JedisShardInfo shard1 = new JedisShardInfo(redis1.getHost(), redis1.getPort());
    	shard1.setPassword("foobared");
        JedisShardInfo shard2 = new JedisShardInfo(redis2.getHost(), redis2.getPort());
        shard2.setPassword("foobared");
        
        shards.add(shard1);
        shards.add(shard2);
        
        return shards;
    }
    
}