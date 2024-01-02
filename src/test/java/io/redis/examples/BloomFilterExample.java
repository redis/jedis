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
        UnifiedJedis unifiedJedis = new UnifiedJedis("redis://localhost:6379");
        // HIDE_END

        // STEP_START bloom
        String res1 = unifiedJedis.bfReserve("bikes:models", 0.01, 1000);
        System.out.println(res1); // >>> True

        boolean res2 = unifiedJedis.bfAdd("bikes:models", "Smoky Mountain Striker");
        System.out.println(res2); // >>> True

        boolean res3 = unifiedJedis.bfExists("bikes:models", "Smoky Mountain Striker");
        System.out.println(res3); // >>> True

        List<Boolean> res4 = unifiedJedis.bfMAdd("bikes:models",
                "Rocky Mountain Racer",
                "Cloudy City Cruiser",
                "Windy City Wippet");
        System.out.println(res4); // >>> True

        List<Boolean> res5 = unifiedJedis.bfMExists("bikes:models",
                "Rocky Mountain Racer",
                "Cloudy City Cruiser",
                "Windy City Wippet");
        System.out.println(res5); // >>> [True, True, True]
        // STEP_END

        // REMOVE_START
        Assert.assertEquals("OK", res1);
        // REMOVE_END
    }
}
