// EXAMPLE: cmds_servermgmt
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END
import java.util.Set;

import redis.clients.jedis.Jedis;
// HIDE_START
import redis.clients.jedis.UnifiedJedis;

public class CmdsServerMgmtExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
// HIDE_END

        // STEP_START flushall
        // REMOVE_START
        jedis.set("testkey1", "1");
        jedis.set("testkey2", "2");
        jedis.set("testkey3", "3");
        // REMOVE_END
        String flushAllResult1 = jedis.flushAll();
        System.out.println(flushAllResult1); // >>> OK

        Set<String> flushAllResult2 = jedis.keys("*");
        System.out.println(flushAllResult2); // >>> []
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", flushAllResult1);
        Assert.assertEquals("[]", flushAllResult2.toString());
        // REMOVE_END

        // STEP_START info
        // Note: you must use the `Jedis` class to access the `info`
        // command rather than `UnifiedJedis`.
        Jedis jedis2 = new Jedis("redis://localhost:6379");

        String infoResult = jedis2.info();
        
        // Check the first 8 characters of the result (the full `info` string
        // is much longer than this).
        System.out.println(infoResult.substring(0, 8)); // >>> # Server

        jedis2.close();
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("# Server", infoResult.substring(0, 8));
        // REMOVE_END
        
// HIDE_START
        jedis.close();
    }
}
// HIDE_END
