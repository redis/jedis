package redis.clients.jedis.prefix;

import io.redis.test.annotations.ConditionalOnEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.TestEnvUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class PrefixedKeysTest<T extends UnifiedJedis> {

    @RegisterExtension
    public static EnvCondition envCondition = new EnvCondition();

    abstract T nonPrefixingJedis();

    T prefixingJedis() {
        T jedis = nonPrefixingJedis();
        jedis.setKeyArgumentPreProcessor(new PrefixedKeyArgumentPreProcessor("test-prefix:"));
        return jedis;
    }

    @AfterEach
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
    @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
