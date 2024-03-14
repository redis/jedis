package redis.clients.jedis;

import org.junit.Test;
import redis.clients.jedis.resps.Tuple;

import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertEquals;

public abstract class PrefixedKeysTest {

    abstract UnifiedJedis prefixingJedis();

    abstract UnifiedJedis nonPrefixingJedis();

    @Test
    public void hasPrefixedKeys() {
        HostAndPort hp = new HostAndPort("127.0.0.1", 7379);

        try (UnifiedJedis jedis = prefixingJedis()) {
            jedis.set("foo1", "bar1");
            jedis.set("foo2".getBytes(StandardCharsets.UTF_8), "bar2".getBytes(StandardCharsets.UTF_8));
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
}
