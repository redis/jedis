// EXAMPLE: cmds_generic
// REMOVE_START
package io.redis.examples;

import org.junit.Test;

// REMOVE_END
// HIDE_START
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.ExpiryOption;
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
        assert delResult1.equals("OK");
        assert delResult2.equals("OK");
        assert delResult3 == 2;
        // REMOVE_END


        // STEP_START expire
        String expireResult1 = jedis.set("mykey", "Hello");
        System.out.println(expireResult1);  // >>> OK

        long expireResult2 = jedis.expire("mykey", 10);
        System.out.println(expireResult2);  // >>> 1

        long expireResult3 = jedis.ttl("mykey");
        System.out.println(expireResult3);  // >>> 10

        String expireResult4 = jedis.set("mykey", "Hello World");
        System.out.println(expireResult4);  // >>> OK

        long expireResult5 = jedis.ttl("mykey");
        System.out.println(expireResult5);  // >>> -1

        long expireResult6 = jedis.expire("mykey", 10, ExpiryOption.XX);
        System.out.println(expireResult6);  // >>> 0

        long expireResult7 = jedis.ttl("mykey");
        System.out.println(expireResult7);  // >>> -1

        long expireResult8 = jedis.expire("mykey", 10, ExpiryOption.NX);
        System.out.println(expireResult8);  // >>> 1

        long expireResult9 = jedis.ttl("mykey");
        System.out.println(expireResult9);  // >>> 10
        // STEP_END

        // Tests for 'expire' step.
        // REMOVE_START
        assert expireResult1.equals("OK");
        assert expireResult2 == 1;
        assert expireResult3 == 10;
        assert expireResult4.equals("OK");
        assert expireResult5 == -1;
        assert expireResult6 == 0;
        assert expireResult7 == -1;
        assert expireResult8 == 1;
        assert expireResult9 == 10;
        jedis.del("mykey");
        // REMOVE_END


        // STEP_START ttl
        String ttlResult1 = jedis.set("mykey", "Hello");
        System.out.println(ttlResult1); // >>> OK

        long ttlResult2 = jedis.expire("mykey", 10);
        System.out.println(ttlResult2); // >>> 1

        long ttlResult3 = jedis.ttl("mykey");
        System.out.println(ttlResult3); // >>> 10
        // STEP_END

        // Tests for 'ttl' step.
        // REMOVE_START
        assert ttlResult1.equals("OK");
        assert ttlResult2 == 1;
        assert ttlResult3 == 10;
        jedis.del("mykey");
        // REMOVE_END

// HIDE_START
        
    }
}
// HIDE_END

