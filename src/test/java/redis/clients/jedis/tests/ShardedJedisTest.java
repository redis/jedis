package redis.clients.jedis.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.Hashing;
import redis.clients.util.ShardInfo;

public class ShardedJedisTest extends Assert {
	private static HostAndPort redis1 = HostAndPortUtil.getRedisServers().get(0); 
	private static HostAndPort redis2 = HostAndPortUtil.getRedisServers().get(1); 
	
    @Test
    public void checkSharding() throws IOException {
	List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
	shards.add(new JedisShardInfo(redis1.host, redis1.port));
	shards.add(new JedisShardInfo(redis2.host, redis2.port));
	ShardedJedis jedis = new ShardedJedis(shards);
	ShardInfo s1 = jedis.getShardInfo("a");
	ShardInfo s2 = jedis.getShardInfo("b");
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

}