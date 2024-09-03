// EXAMPLE: cmds_sorted_set
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;

// REMOVE_END
// HIDE_START
// HIDE_END
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.Tuple;

// HIDE_START
public class CmdsSortedSetExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        //REMOVE_START
        // Clear any keys here before using them in tests.
        jedis.del("myzset");
        //REMOVE_END
// HIDE_END


        // STEP_START bzmpop

        // STEP_END

        // Tests for 'bzmpop' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START bzpopmax

        // STEP_END

        // Tests for 'bzpopmax' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START bzpopmin

        // STEP_END

        // Tests for 'bzpopmin' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zadd
        Map<String, Double> zAddExampleParams = new HashMap<>();
        zAddExampleParams.put("one", 1.0);
        long zAddResult1 = jedis.zadd("myzset", zAddExampleParams);
        System.out.println(zAddResult1);    // >>> 1

        zAddExampleParams.clear();
        zAddExampleParams.put("uno", 1.0);
        long zAddResult2 = jedis.zadd("myzset", zAddExampleParams);
        System.out.println(zAddResult2);    // >>> 1

        zAddExampleParams.clear();
        zAddExampleParams.put("two", 2.0);
        zAddExampleParams.put("three", 3.0);
        long zAddResult3 = jedis.zadd("myzset", zAddExampleParams);
        System.out.println(zAddResult3);    // >>> 2

        List<Tuple> zAddResult4 = jedis.zrangeWithScores("myzset", new ZRangeParams(0, -1));

        for (Tuple item: zAddResult4) {
            System.out.println("Element: " + item.getElement() + ", Score: " + item.getScore());
        }
        // >>> Element: one, Score: 1.0
        // >>> Element: uno, Score: 1.0
        // >>> Element: two, Score: 2.0
        // >>> Element: three, Score: 3.0
        // STEP_END

        // Tests for 'zadd' step.
        // REMOVE_START
        Assert.assertEquals(1, zAddResult1);
        Assert.assertEquals(1, zAddResult2);
        Assert.assertEquals(2, zAddResult3);
        Assert.assertEquals(new Tuple("one", 1.0), zAddResult4.get(0));
        Assert.assertEquals(new Tuple("uno", 1.0), zAddResult4.get(1));
        Assert.assertEquals(new Tuple("two", 2.0), zAddResult4.get(2));
        Assert.assertEquals(new Tuple("three", 3.0), zAddResult4.get(3));
        jedis.del("myzset");
        // REMOVE_END


        // STEP_START zcard

        // STEP_END

        // Tests for 'zcard' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zcount

        // STEP_END

        // Tests for 'zcount' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zdiff

        // STEP_END

        // Tests for 'zdiff' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zdiffstore

        // STEP_END

        // Tests for 'zdiffstore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zincrby

        // STEP_END

        // Tests for 'zincrby' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zinter

        // STEP_END

        // Tests for 'zinter' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zintercard

        // STEP_END

        // Tests for 'zintercard' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zinterstore

        // STEP_END

        // Tests for 'zinterstore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zlexcount

        // STEP_END

        // Tests for 'zlexcount' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zmpop

        // STEP_END

        // Tests for 'zmpop' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zmscore

        // STEP_END

        // Tests for 'zmscore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zpopmax

        // STEP_END

        // Tests for 'zpopmax' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zpopmin

        // STEP_END

        // Tests for 'zpopmin' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrandmember

        // STEP_END

        // Tests for 'zrandmember' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrange1
        Map<String, Double> zRangeExampleParams1 = new HashMap<>();
        zRangeExampleParams1.put("one", 1.0);
        zRangeExampleParams1.put("two", 2.0);
        zRangeExampleParams1.put("three", 3.0);
        long zRangeResult1 = jedis.zadd("myzset", zRangeExampleParams1);
        System.out.println(zRangeResult1);  // >>> 3

        List<String> zRangeResult2 = jedis.zrange("myzset", new ZRangeParams(0, -1));
        System.out.println(String.join(", ", zRangeResult2));   // >>> one, two, three

        List<String> zRangeResult3 = jedis.zrange("myzset", new ZRangeParams(2, 3));
        System.out.println(String.join(", ", zRangeResult3));   // >> three

        List<String> zRangeResult4 = jedis.zrange("myzset", new ZRangeParams(-2, -1));
        System.out.println(String.join(", ", zRangeResult4));   // >> two, three
        // STEP_END

        // Tests for 'zrange1' step.
        // REMOVE_START
        Assert.assertEquals(3, zRangeResult1);
        Assert.assertEquals("one, two, three", String.join(", ", zRangeResult2));
        Assert.assertEquals("three", String.join(", ", zRangeResult3));
        Assert.assertEquals("two, three", String.join(", ", zRangeResult4));
        jedis.del("myzset");
        // REMOVE_END


        // STEP_START zrange2
        Map<String, Double> zRangeExampleParams2 = new HashMap<>();
        zRangeExampleParams2.put("one", 1.0);
        zRangeExampleParams2.put("two", 2.0);
        zRangeExampleParams2.put("three", 3.0);
        long zRangeResult5 = jedis.zadd("myzset", zRangeExampleParams2);
        System.out.println(zRangeResult5);  // >>> 3

        List<Tuple> zRangeResult6 = jedis.zrangeWithScores("myzset", new ZRangeParams(0, 1));

        for (Tuple item: zRangeResult6) {
            System.out.println("Element: " + item.getElement() + ", Score: " + item.getScore());
        }
        // >>> Element: one, Score: 1.0
        // >>> Element: two, Score: 2.0
        // STEP_END

        // Tests for 'zrange2' step.
        // REMOVE_START
        Assert.assertEquals(3, zRangeResult5);
        Assert.assertEquals(new Tuple("one", 1.0), zRangeResult6.get(0));
        Assert.assertEquals(new Tuple("two", 2.0), zRangeResult6.get(1));
        jedis.del("myzset");
        // REMOVE_END


        // STEP_START zrange3
        Map<String, Double> zRangeExampleParams3 = new HashMap<>();
        zRangeExampleParams3.put("one", 1.0);
        zRangeExampleParams3.put("two", 2.0);
        zRangeExampleParams3.put("three", 3.0);
        long zRangeResult7 = jedis.zadd("myzset", zRangeExampleParams3);
        System.out.println(zRangeResult7);  // >>> 3

        List<String> zRangeResult8 = jedis.zrangeByScore("myzset", "(1", "+inf", 1, 1);
        System.out.println(String.join(", ", zRangeResult8));   // >>> three
        // STEP_END

        // Tests for 'zrange3' step.
        // REMOVE_START
        Assert.assertEquals(3, zRangeResult7);
        Assert.assertEquals("three", String.join(", ", zRangeResult8));
        jedis.del("myzset");
        // REMOVE_END


        // STEP_START zrangebylex

        // STEP_END

        // Tests for 'zrangebylex' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrangebyscore

        // STEP_END

        // Tests for 'zrangebyscore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrangestore

        // STEP_END

        // Tests for 'zrangestore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrank

        // STEP_END

        // Tests for 'zrank' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrem

        // STEP_END

        // Tests for 'zrem' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zremrangebylex

        // STEP_END

        // Tests for 'zremrangebylex' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zremrangebyrank

        // STEP_END

        // Tests for 'zremrangebyrank' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zremrangebyscore

        // STEP_END

        // Tests for 'zremrangebyscore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrevrange

        // STEP_END

        // Tests for 'zrevrange' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrevrangebylex

        // STEP_END

        // Tests for 'zrevrangebylex' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrevrangebyscore

        // STEP_END

        // Tests for 'zrevrangebyscore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zrevrank

        // STEP_END

        // Tests for 'zrevrank' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zscan

        // STEP_END

        // Tests for 'zscan' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zscore

        // STEP_END

        // Tests for 'zscore' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zunion

        // STEP_END

        // Tests for 'zunion' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START zunionstore

        // STEP_END

        // Tests for 'zunionstore' step.
        // REMOVE_START

        // REMOVE_END


// HIDE_START
    }
}
// HIDE_END

