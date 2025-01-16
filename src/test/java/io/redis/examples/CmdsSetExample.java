// EXAMPLE: cmds_set
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END
import static java.util.stream.Collectors.toList;

import java.util.Set;

// HIDE_START
import redis.clients.jedis.UnifiedJedis;

public class CmdsSetExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        //REMOVE_START
        jedis.del("myset");
        //REMOVE_END
// HIDE_END

        // STEP_START sadd
        long sAddResult1 = jedis.sadd("myset", "Hello");
        System.out.println(sAddResult1); // >>> 1

        long sAddResult2 = jedis.sadd("myset", "World");
        System.out.println(sAddResult2); // >>> 1

        long sAddResult3 = jedis.sadd("myset", "World");
        System.out.println(sAddResult3); // >>> 0

        Set<String> sAddResult4 = jedis.smembers("myset");
        System.out.println(sAddResult4.stream().sorted().collect(toList()));
        // >>> [Hello, World]
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(1, sAddResult1);
        Assert.assertEquals(1, sAddResult2);
        Assert.assertEquals(0, sAddResult3);
        Assert.assertArrayEquals(new String[] {"Hello", "World"}, sAddResult4.stream().sorted().toArray());
        jedis.del("myset");
        // REMOVE_END

        // STEP_START smembers
        long sMembersResult1 = jedis.sadd("myset", "Hello", "World");
        System.out.println(sMembersResult1); // >>> 2

        Set<String> sMembersResult2 = jedis.smembers("myset");
        System.out.println(sMembersResult2.stream().sorted().collect(toList()));
        // >>> [Hello, World]
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(2, sMembersResult1);
        Assert.assertArrayEquals(new String[] {"Hello", "World"}, sMembersResult2.stream().sorted().toArray());
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END
