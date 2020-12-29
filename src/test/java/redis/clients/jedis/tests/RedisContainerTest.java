package redis.clients.jedis.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import static org.junit.Assert.assertEquals;

public class RedisContainerTest {
    private static RedisContainer CONTAINER;

    @BeforeClass
    public static void beforeClass() {
        CONTAINER = new RedisContainer();
        CONTAINER.start();
    }

    @AfterClass
    public static void afterClass() {
        if (CONTAINER != null) {
            CONTAINER.stop();
        }
    }

    @Test
    public void happyPath() {
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        Jedis jedis = new Jedis(CONTAINER.getRedisUri());
        jedis.set(key, value);
        assertEquals(value, jedis.get(key));
        jedis.close();
    }
}
