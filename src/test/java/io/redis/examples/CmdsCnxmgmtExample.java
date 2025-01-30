// EXAMPLE: cmds_cnxmgmt
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END

import redis.clients.jedis.Jedis;

// HIDE_START
public class CmdsCnxmgmtExample {
    @Test
    public void run() {
// HIDE_END
        Jedis jedis = new Jedis("redis://localhost:6379");

        // STEP_START auth1
        // REMOVE_START
        jedis.configSet("requirepass", "temp_pass");
        // REMOVE_END
        // Note: you must use the `Jedis` class rather than `UnifiedJedis`
        // to access the `auth` commands.
        String authResult1 = jedis.auth("default",  "temp_pass");
        System.out.println(authResult1); // >>> OK
        // REMOVE_START
        Assert.assertEquals("OK", authResult1);
        jedis.configSet("requirepass", "");
        // REMOVE_END
        // STEP_END
       
        // STEP_START auth2
        // REMOVE_START
        jedis.aclSetUser("test-user", "on", ">strong_password", "+acl");
        // REMOVE_END
        // Note: you must use the `Jedis` class rather than `UnifiedJedis`
        // to access the `auth` commands.
        String authResult2 = jedis.auth("test-user", "strong_password");
        System.out.println(authResult2); // >>> OK
        // REMOVE_START
        Assert.assertEquals("OK", authResult2);
        jedis.aclDelUser("test-user");
        // REMOVE_END
        // STEP_END
        
        // HIDE_START
    }
}
// HIDE_END
