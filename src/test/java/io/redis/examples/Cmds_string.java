// EXAMPLE: cmds_string
// REMOVE_START
package io.redis.examples;

import org.junit.Test;

// REMOVE_END
// HIDE_START
import redis.clients.jedis.UnifiedJedis;
// HIDE_END

// HIDE_START
public class Cmds_string {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        //REMOVE_START
        // Clear any keys here before using them in tests.

        //REMOVE_END
// HIDE_END


        // STEP_START append1

        // STEP_END

        // Tests for 'append1' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START append2

        // STEP_END

        // Tests for 'append2' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START decr

        // STEP_END

        // Tests for 'decr' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START decrby

        // STEP_END

        // Tests for 'decrby' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START get

        // STEP_END

        // Tests for 'get' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START getdel

        // STEP_END

        // Tests for 'getdel' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START getex

        // STEP_END

        // Tests for 'getex' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START getrange

        // STEP_END

        // Tests for 'getrange' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START getset

        // STEP_END

        // Tests for 'getset' step.
        // REMOVE_START

        // REMOVE_END


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
        assert incrResult1.equals("OK");
        assert incrResult2 == 11;
        assert incrResult3.equals("11");
        jedis.del("mykey");
        // REMOVE_END


        // STEP_START incrby

        // STEP_END

        // Tests for 'incrby' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START incrbyfloat

        // STEP_END

        // Tests for 'incrbyfloat' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START lcs1

        // STEP_END

        // Tests for 'lcs1' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START lcs2

        // STEP_END

        // Tests for 'lcs2' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START lcs3

        // STEP_END

        // Tests for 'lcs3' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START lcs4

        // STEP_END

        // Tests for 'lcs4' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START lcs5

        // STEP_END

        // Tests for 'lcs5' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START mget

        // STEP_END

        // Tests for 'mget' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START mset

        // STEP_END

        // Tests for 'mset' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START msetnx

        // STEP_END

        // Tests for 'msetnx' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START psetex

        // STEP_END

        // Tests for 'psetex' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START set

        // STEP_END

        // Tests for 'set' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START setex

        // STEP_END

        // Tests for 'setex' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START setnx

        // STEP_END

        // Tests for 'setnx' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START setrange1

        // STEP_END

        // Tests for 'setrange1' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START setrange2

        // STEP_END

        // Tests for 'setrange2' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START strlen

        // STEP_END

        // Tests for 'strlen' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START substr

        // STEP_END

        // Tests for 'substr' step.
        // REMOVE_START

        // REMOVE_END


// HIDE_START
    }
}
// HIDE_END

