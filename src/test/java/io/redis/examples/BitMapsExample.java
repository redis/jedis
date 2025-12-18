// EXAMPLE: bitmap_tutorial
// HIDE_START
package io.redis.examples;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitMapsExample {

    @Test
    public void run() {
        RedisClient jedis = RedisClient.create("redis://localhost:6379");
        // HIDE_END

        // REMOVE_START
        jedis.del("pings:2024-01-01-00:00");
        // REMOVE_END

        // STEP_START ping
        boolean res1 = jedis.setbit("pings:2024-01-01-00:00", 123, true);
        System.out.println(res1); // >>> false

        boolean res2 = jedis.getbit("pings:2024-01-01-00:00", 123);
        System.out.println(res2); // >>> true

        boolean res3 = jedis.getbit("pings:2024-01-01-00:00", 456);
        System.out.println(res3); // >>> false
        // STEP_END

        // REMOVE_START
        assertFalse(res1);
        assertTrue(res2);
        assertFalse(res3);
        // REMOVE_END

        // STEP_START bitcount
        long res4 = jedis.bitcount("pings:2024-01-01-00:00");
        System.out.println(res4); // >>> 1
        // STEP_END

        // REMOVE_START
        assertEquals(1, res4);
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END
