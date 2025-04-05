// EXAMPLE: cmds_list
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END
import java.util.List;

// HIDE_START
import redis.clients.jedis.UnifiedJedis;

public class CmdsListExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        //REMOVE_START
        jedis.del("mylist");
        //REMOVE_END
// HIDE_END

        // STEP_START llen
        long lLenResult1 = jedis.lpush("mylist", "World");
        System.out.println(lLenResult1); // >>> 1

        long lLenResult2 = jedis.lpush("mylist", "Hello");
        System.out.println(lLenResult2); // >>> 2

        long lLenResult3 = jedis.llen("mylist");
        System.out.println(lLenResult3); // >>> 2
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(1, lLenResult1);
        Assert.assertEquals(2, lLenResult2);
        Assert.assertEquals(2, lLenResult3);
        jedis.del("mylist");
        // REMOVE_END

        // STEP_START lpop
        long lPopResult1 = jedis.rpush(
            "mylist", "one", "two", "three", "four", "five"
        );
        System.out.println(lPopResult1); // >>> 5

        String lPopResult2 = jedis.lpop("mylist");
        System.out.println(lPopResult2); // >>> one

        List<String> lPopResult3 = jedis.lpop("mylist", 2);
        System.out.println(lPopResult3); // >>> [two, three]

        List<String> lPopResult4 = jedis.lrange("mylist", 0, -1);
        System.out.println(lPopResult4); // >>> [four, five]
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(5, lPopResult1);
        Assert.assertEquals("one", lPopResult2);
        Assert.assertEquals("[two, three]", lPopResult3.toString());
        Assert.assertEquals("[four, five]", lPopResult4.toString());
        jedis.del("mylist");
        // REMOVE_END

        // STEP_START lpush
        long lPushResult1 = jedis.lpush("mylist", "World");
        System.out.println(lPushResult1); // >>> 1

        long lPushResult2 = jedis.lpush("mylist", "Hello");
        System.out.println(lPushResult2); // >>> 2

        List<String> lPushResult3 = jedis.lrange("mylist", 0, -1);
        System.out.println(lPushResult3);
        // >>> [Hello, World]
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(1, lPushResult1);
        Assert.assertEquals(2, lPushResult2);
        Assert.assertEquals("[Hello, World]", lPushResult3.toString());
        jedis.del("mylist");
        // REMOVE_END

        // STEP_START lrange
        long lRangeResult1 = jedis.rpush("mylist", "one", "two", "three");
        System.out.println(lRangeResult1); // >>> 3

        List<String> lRangeResult2 = jedis.lrange("mylist", 0, 0);
        System.out.println(lRangeResult2); // >>> [one]

        List<String> lRangeResult3 = jedis.lrange("mylist", -3, 2);
        System.out.println(lRangeResult3); // >>> [one, two, three]

        List<String> lRangeResult4 = jedis.lrange("mylist", -100, 100);
        System.out.println(lRangeResult4); // >>> [one, two, three]

        List<String> lRangeResult5 = jedis.lrange("mylist", 5, 10);
        System.out.println(lRangeResult5); // >>> []
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(3, lRangeResult1);
        Assert.assertEquals("[one]", lRangeResult2.toString());
        Assert.assertEquals("[one, two, three]", lRangeResult3.toString());
        Assert.assertEquals("[one, two, three]", lRangeResult4.toString());
        Assert.assertEquals("[]", lRangeResult5.toString());
        jedis.del("mylist");
        // REMOVE_END

        // STEP_START rpop
        long rPopResult1 = jedis.rpush(
            "mylist", "one", "two", "three", "four", "five"
        );
        System.out.println(rPopResult1); // >>> 5

        String rPopResult2 = jedis.rpop("mylist");
        System.out.println(rPopResult2); // >>> five

        List<String> rPopResult3 = jedis.rpop("mylist", 2);
        System.out.println(rPopResult3); // >>> [four, three]

        List<String> rPopResult4 = jedis.lrange("mylist", 0, -1);
        System.out.println(rPopResult4); // >>> [one, two]
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(5, rPopResult1);
        Assert.assertEquals("five", rPopResult2);
        Assert.assertEquals("[four, three]", rPopResult3.toString());
        Assert.assertEquals("[one, two]", rPopResult4.toString());
        jedis.del("mylist");
        // REMOVE_END

        // STEP_START rpush
        long rPushResult1 = jedis.rpush("mylist", "hello");
        System.out.println(rPushResult1); // >>> 1

        long rPushResult2 = jedis.rpush("mylist", "world");
        System.out.println(rPushResult2); // >>> 2

        List<String> rPushResult3 = jedis.lrange("mylist", 0, -1);
        System.out.println(rPushResult3); // >>> [hello, world]
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(1, rPushResult1);
        Assert.assertEquals(2, rPushResult2);
        Assert.assertEquals("[hello, world]", rPushResult3.toString());
        jedis.del("mylist");
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END
