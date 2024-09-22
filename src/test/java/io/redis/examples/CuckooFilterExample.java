// EXAMPLE: cuckoo_tutorial

// HIDE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;

public class CuckooFilterExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        // HIDE_END

        // REMOVE_START
        jedis.del("bikes:models");
        // REMOVE_END

        // STEP_START cuckoo
        String res1 = jedis.cfReserve("bikes:models", 1000000);
        System.out.println(res1); // >>> OK

        // REMOVE_START
        Assert.assertEquals(res1, "OK");
        // REMOVE_END

        boolean res2 = jedis.cfAdd("bikes:models", "Smoky Mountain Striker");
        System.out.println(res2); // >>> True

        boolean res3 = jedis.cfExists("bikes:models", "Smoky Mountain Striker");
        System.out.println(res3); // >>> True

        boolean res4 = jedis.cfExists("bikes:models", "Terrible Bike Name");
        System.out.println(res4); // >>> False

        boolean res5 = jedis.cfDel("bikes:models", "Smoky Mountain Striker");
        System.out.println(res5); // >>> True

        // REMOVE_START
        Assert.assertTrue(res5);
        // REMOVE_END
        // STEP_END
    }
}
