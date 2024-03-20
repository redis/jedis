package redis.clients.jedis.prefix;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor;
import redis.clients.jedis.util.SafeEncoder;

public abstract class PrefixedKeysTest<T extends UnifiedJedis> {

    abstract T nonPrefixingJedis();

    T prefixingJedis() {
        T jedis = nonPrefixingJedis();
        jedis.setKeyArgumentPreProcessor(new PrefixedKeyArgumentPreProcessor("test-prefix:"));
        return jedis;
    }

    @After
    public void cleanUp() {
        try (UnifiedJedis jedis = prefixingJedis()) {
            jedis.flushAll();
        }
    }

    @Test
    public void prefixesKeys() {
        try (UnifiedJedis jedis = prefixingJedis()) {
            jedis.set("foo1", "bar1");
            jedis.set(SafeEncoder.encode("foo2"), SafeEncoder.encode("bar2"));
            AbstractPipeline pipeline = jedis.pipelined();
            pipeline.incr("foo3");
            pipeline.zadd("foo4", 1234, "bar4");
            pipeline.sync();
        }

        try (UnifiedJedis jedis = nonPrefixingJedis()) {
            assertEquals("bar1", jedis.get("test-prefix:foo1"));
            assertEquals("bar2", jedis.get("test-prefix:foo2"));
            assertEquals("1", jedis.get("test-prefix:foo3"));
            assertEquals(new Tuple("bar4", 1234d), jedis.zpopmax("test-prefix:foo4"));
        }
    }

    @Test
    public void prefixesKeysInTransaction() {
        try (UnifiedJedis jedis = prefixingJedis()) {
            AbstractTransaction transaction = jedis.multi();
            transaction.set("foo1", "bar1-from-transaction");
            transaction.hset("foo2", "bar2-key", "bar2-value");
            transaction.exec();
        }

        try (UnifiedJedis jedis = nonPrefixingJedis()) {
            assertEquals("bar1-from-transaction", jedis.get("test-prefix:foo1"));
            assertEquals("bar2-value", jedis.hget("test-prefix:foo2", "bar2-key"));
        }
    }
}
