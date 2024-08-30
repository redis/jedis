// EXAMPLE: cmds_generic
// REMOVE_START
package io.redis.examples;

import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.Assert;

// REMOVE_END
// HIDE_START
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;
// HIDE_END

// HIDE_START
public class CmdsGenericExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        //REMOVE_START
        // Clear any keys here before using them in tests.

        //REMOVE_END
// HIDE_END


        // STEP_START copy

        // STEP_END

        // Tests for 'copy' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START del
        String delResult1 = jedis.set("key1", "Hello");
        System.out.println(delResult1); // >>> OK

        String delResult2 = jedis.set("key2", "World");
        System.out.println(delResult2); // >>> OK

        long delResult3 = jedis.del("key1", "key2", "key3");
        System.out.println(delResult3); // >>> 2
        // STEP_END

        // Tests for 'del' step.
        // REMOVE_START
        Assert.assertEquals("OK", delResult1);
        Assert.assertEquals("OK", delResult2);
        Assert.assertEquals(2, delResult3);
        // REMOVE_END


        // STEP_START dump

        // STEP_END

        // Tests for 'dump' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START exists

        // STEP_END

        // Tests for 'exists' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START expire

        // STEP_END

        // Tests for 'expire' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START expireat

        // STEP_END

        // Tests for 'expireat' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START expiretime

        // STEP_END

        // Tests for 'expiretime' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START keys

        // STEP_END

        // Tests for 'keys' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START migrate

        // STEP_END

        // Tests for 'migrate' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START move

        // STEP_END

        // Tests for 'move' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START object_encoding

        // STEP_END

        // Tests for 'object_encoding' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START object_freq

        // STEP_END

        // Tests for 'object_freq' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START object_idletime

        // STEP_END

        // Tests for 'object_idletime' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START object_refcount

        // STEP_END

        // Tests for 'object_refcount' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START persist

        // STEP_END

        // Tests for 'persist' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START pexpire

        // STEP_END

        // Tests for 'pexpire' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START pexpireat

        // STEP_END

        // Tests for 'pexpireat' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START pexpiretime

        // STEP_END

        // Tests for 'pexpiretime' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START pttl

        // STEP_END

        // Tests for 'pttl' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START randomkey

        // STEP_END

        // Tests for 'randomkey' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START rename

        // STEP_END

        // Tests for 'rename' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START renamenx

        // STEP_END

        // Tests for 'renamenx' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START restore

        // STEP_END

        // Tests for 'restore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START scan1

        // STEP_END

        // Tests for 'scan1' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START scan2

        // STEP_END

        // Tests for 'scan2' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START scan3

        // STEP_END

        // Tests for 'scan3' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START scan4

        // STEP_END

        // Tests for 'scan4' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START sort

        // STEP_END

        // Tests for 'sort' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START sort_ro

        // STEP_END

        // Tests for 'sort_ro' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START touch

        // STEP_END

        // Tests for 'touch' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START ttl

        // STEP_END

        // Tests for 'ttl' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START type

        // STEP_END

        // Tests for 'type' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START unlink

        // STEP_END

        // Tests for 'unlink' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START wait

        // STEP_END

        // Tests for 'wait' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START waitaof

        // STEP_END

        // Tests for 'waitaof' step.
        // REMOVE_START

        // REMOVE_END


// HIDE_START
        
    }
}
// HIDE_END

