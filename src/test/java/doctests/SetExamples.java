//EXAMPLE: sets_tutorial
//HIDE_START
package doctests;
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;
import java.util.List;
import java.util.Set;

public class SetExamples {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        // HIDE_END
        // REMOVE_START
        jedis.del("bikes:racing:france");
        jedis.del("bikes:racing:usa");
        // REMOVE_END
        // STEP_START sadd
        long res1 = jedis.sadd("bikes:racing:france", "bike:1");
        System.out.println(res1);  // >>> 1

        long res2 = jedis.sadd("bikes:racing:france", "bike:1");
        System.out.println(res2);  // >>> 0

        long res3 = jedis.sadd("bikes:racing:france", "bike:2", "bike:3");
        System.out.println(res3);  // >>> 2

        long res4 = jedis.sadd("bikes:racing:usa", "bike:1", "bike:4");
        System.out.println(res4);  // >>> 2
        // STEP_END

        // REMOVE_START
        assert res1 == 1;
        assert res2 == 0;
        assert res3 == 2;
        assert res4 == 2;
        // REMOVE_END

        // STEP_START sismember
        // HIDE_START
        jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
        jedis.sadd("bikes:racing:usa", "bike:1", "bike:4");
        // HIDE_END

        boolean res5 = jedis.sismember("bikes:racing:usa", "bike:1");
        System.out.println(res5);  // >>> true

        boolean res6 = jedis.sismember("bikes:racing:usa", "bike:2");
        System.out.println(res6);  // >>> false
        // STEP_END

        // REMOVE_START
        assert res5 == true;
        assert res6 == false;
        // REMOVE_END

        // STEP_START sinter
        // HIDE_START
        jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
        jedis.sadd("bikes:racing:usa", "bike:1", "bike:4");
        // HIDE_END

        Set<String> res7 = jedis.sinter("bikes:racing:france", "bikes:racing:usa");
        System.out.println(res7);  // >>> [bike:1]
        // STEP_END

        // REMOVE_START
        assert res7.toString().equals("[bike:1]");
        // REMOVE_END

        // STEP_START scard
        jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");

        long res8 = jedis.scard("bikes:racing:france");
        System.out.println(res8);  // >>> 3
        // STEP_END

        // REMOVE_START
        assert res8 == 3;
        jedis.del("bikes:racing:france");
        // REMOVE_END

        // STEP_START sadd_smembers
        long res9 = jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
        System.out.println(res9);  // >>> 3

        Set<String> res10 = jedis.smembers("bikes:racing:france");
        System.out.println(res10);  // >>> [bike:1, bike:2, bike:3]
        // STEP_END

        // REMOVE_START
        assert res9 == 3;
        assert res10.toString().equals("[bike:1, bike:2, bike:3]");
        // REMOVE_END

        // STEP_START smismember
        boolean res11 = jedis.sismember("bikes:racing:france", "bike:1");
        System.out.println(res11);  // >>> true

        List<Boolean> res12 = jedis.smismember("bikes:racing:france", "bike:2", "bike:3", "bike:4");
        System.out.println(res12);  // >>> [true,true,false]
        // STEP_END

        // REMOVE_START
        assert res11 == true;
        assert res12.toString().equals("[true, true, false]");
        // REMOVE_END

        // STEP_START sdiff
        jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
        jedis.sadd("bikes:racing:usa", "bike:1", "bike:4");

        Set<String> res13 = jedis.sdiff("bikes:racing:france", "bikes:racing:usa");
        System.out.println(res13);  // >>> [bike:2, bike:3]

        // REMOVE_START
        assert res13.toString().equals("[bike:2, bike:3]");
        jedis.del("bikes:racing:france");
        jedis.del("bikes:racing:usa");
        // REMOVE_END

        // STEP_START multisets
        jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
        jedis.sadd("bikes:racing:usa", "bike:1", "bike:4");
        jedis.sadd("bikes:racing:italy", "bike:1", "bike:2", "bike:3", "bike:4");

        Set<String> res14 = jedis.sinter("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy");
        System.out.println(res14);  // >>> [bike:1]

        Set<String> res15 = jedis.sunion("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy");
        System.out.println(res15);  // >>> [bike:1, bike:2, bike:3, bike:4]

        Set<String> res16 = jedis.sdiff("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy");
        System.out.println(res16);  // >>> []

        Set<String> res17 = jedis.sdiff("bikes:racing:usa", "bikes:racing:france");
        System.out.println(res17);  // >>> [bike:4]

        Set<String> res18 = jedis.sdiff("bikes:racing:france", "bikes:racing:usa");
        System.out.println(res18);  // >>> [bike:2, bike:3]

        // REMOVE_START
        assert res14.toString().equals("[bike:1]");
        assert res15.toString().equals("[bike:1, bike:2, bike:3, bike:4]");
        assert res16.toString().equals("[]");
        assert res17.toString().equals("[bike:4]");
        assert res18.toString().equals("[bike:2, bike:3]");
        jedis.del("bikes:racing:france");
        jedis.del("bikes:racing:usa");
        jedis.del("bikes:racing:italy");
        // REMOVE_END

        // STEP_START srem
        jedis.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5");

        long res19 = jedis.srem("bikes:racing:france", "bike:1");
        System.out.println(res18);  // >>> 1

        String res20 = jedis.spop("bikes:racing:france");
        System.out.println(res20);  // >>> bike:3

        Set<String> res21 = jedis.smembers("bikes:racing:france");
        System.out.println(res21);  // >>> [bike:2, bike:4, bike:5]

        String res22 = jedis.srandmember("bikes:racing:france");
        System.out.println(res22);  // >>> bike:4
        // STEP_END

        // REMOVE_START
        assert res19 == 1;
        // REMOVE_END

        // HIDE_START
        jedis.close();
        // HIDE_END
    }
}
