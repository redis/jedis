// EXAMPLE: bitmap_tutorial
// HIDE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;

public class BitMapsExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
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
        Assert.assertFalse(res1);
        Assert.assertTrue(res2);
        Assert.assertFalse(res3);
        // REMOVE_END

        // STEP_START bitcount
        long res4 = jedis.bitcount("pings:2024-01-01-00:00");
        System.out.println(res4); // >>> 1
        // STEP_END

        // REMOVE_START
        Assert.assertEquals(res4, 1);
        // REMOVE_END
    }
}
