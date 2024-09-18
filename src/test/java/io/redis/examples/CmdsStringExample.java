// EXAMPLE: cmds_string
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;

// REMOVE_END
// HIDE_START
import redis.clients.jedis.UnifiedJedis;
// HIDE_END

// HIDE_START
public class CmdsStringExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        //REMOVE_START
        // Clear any keys here before using them in tests.
        jedis.del("mykey");
        //REMOVE_END
// HIDE_END

        // STEP_START incr
        String incrResult1 = jedis.set("mykey", "10");
        System.out.println(incrResult1);    // >>> OK

        long incrResult2 = jedis.incr("mykey");
        System.out.println(incrResult2);    // >>> 11

        String incrResult3 = jedis.get("mykey");
        System.out.println(incrResult3);    // >>> 11
        // STEP_END

        // Tests for 'incr' step.
        // REMOVE_START
        Assert.assertEquals("OK", incrResult1);
        Assert.assertEquals(11, incrResult2);
        Assert.assertEquals("11", incrResult3);
        jedis.del("mykey");
        // REMOVE_END

// HIDE_START
    }
}
// HIDE_END

