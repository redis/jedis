// EXAMPLE: bf_tutorial

// HIDE_START
package io.redis.examples;

import redis.clients.jedis.UnifiedJedis;
import org.junit.Test;
import org.junit.Assert;
import java.util.List;

public class BloomFilterExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        // HIDE_END

        // REMOVE_START
        jedis.del("bikes:models");
        // REMOVE_END

        // STEP_START bloom
        String res1 = jedis.bfReserve("bikes:models", 0.01, 1000);
        System.out.println(res1); // >>> OK

        // REMOVE_START
        Assert.assertEquals("OK", res1);
        // REMOVE_END

        boolean res2 = jedis.bfAdd("bikes:models", "Smoky Mountain Striker");
        System.out.println(res2); // >>> True

        boolean res3 = jedis.bfExists("bikes:models", "Smoky Mountain Striker");
        System.out.println(res3); // >>> True

        List<Boolean> res4 = jedis.bfMAdd("bikes:models",
                "Rocky Mountain Racer",
                "Cloudy City Cruiser",
                "Windy City Wippet");
        System.out.println(res4); // >>> [True, True, True]

        List<Boolean> res5 = jedis.bfMExists("bikes:models",
                "Rocky Mountain Racer",
                "Cloudy City Cruiser",
                "Windy City Wippet");
        System.out.println(res5); // >>> [True, True, True]
        // STEP_END
    }
}
