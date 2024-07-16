// EXAMPLE: bitfield_tutorial
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
import java.util.List;
// REMOVE_END

// HIDE_START
import redis.clients.jedis.UnifiedJedis;
// HIDE_END

// HIDE_START
public class BitfieldExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
// HIDE_END

        //REMOVE_START
        // Clear any keys here before using them in tests.
        jedis.del("bike:1:stats");
        //REMOVE_END

        // STEP_START bf
        List<Long> res1 = jedis.bitfield("bike:1:stats", "SET", "u32", "#0", "1000");
        System.out.println(res1);   // >>> [0]

        List<Long> res2 = jedis.bitfield("bike:1:stats", "INCRBY", "u32", "#0", "-50", "INCRBY", "u32", "#1", "1");
        System.out.println(res2);   // >>> [950, 1]

        List<Long> res3 = jedis.bitfield("bike:1:stats", "INCRBY", "u32", "#0", "500", "INCRBY", "u32", "#1", "1");
        System.out.println(res3);   // >>> [1450, 2]

        List<Long> res4 = jedis.bitfield("bike:1:stats", "GET", "u32", "#0", "GET", "u32", "#1");
        System.out.println(res4);   // >>> [1450, 2]
        // STEP_END

        // Tests for 'bf' step.
        // REMOVE_START
        Assert.assertEquals("[0]", res1.toString());
        Assert.assertEquals("[950, 1]", res2.toString());
        Assert.assertEquals("[1450, 2]", res3.toString());
        Assert.assertEquals("[1450, 2]", res4.toString());
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END
