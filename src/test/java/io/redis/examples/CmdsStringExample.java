// EXAMPLE: cmds_string
// REMOVE_START
package io.redis.examples;

import org.junit.jupiter.api.Test;
// REMOVE_END

// HIDE_START
import redis.clients.jedis.RedisClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
// HIDE_END

// HIDE_START
public class CmdsStringExample {

    @Test
    public void run() {
        RedisClient jedis = RedisClient.create("redis://localhost:6379");

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
        assertEquals("OK", incrResult1);
        assertEquals(11, incrResult2);
        assertEquals("11", incrResult3);
        jedis.del("mykey");
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END

