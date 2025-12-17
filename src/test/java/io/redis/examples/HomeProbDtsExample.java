// EXAMPLE: home_prob_dts
package io.redis.examples;
// REMOVE_START
import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisClient;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
// REMOVE_END
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeProbDtsExample {

    @Test
    public void run() {
        RedisClient jedis = RedisClient.create("redis://localhost:6379");

        // REMOVE_START
        jedis.del(
            "recorded_users", "other_users",
            "group:1", "group:2", "both_groups",
            "items_sold",
            "male_heights", "female_heights", "all_heights",
            "top_3_songs"
        );
        // REMOVE_END

        // STEP_START bloom
        List<Boolean> res1 = jedis.bfMAdd(
            "recorded_users",
            "andy", "cameron", "david", "michelle"
        );
        System.out.println(res1);  // >>> [true, true, true, true]

        boolean res2 = jedis.bfExists("recorded_users", "cameron");
        System.out.println(res2);  // >>> true

        boolean res3 = jedis.bfExists("recorded_users", "kaitlyn");
        System.out.println(res3);  // >>> false
        // STEP_END
        // REMOVE_START
        assertEquals("[true, true, true, true]", res1.toString());
        assertTrue(res2);
        assertFalse(res3);
        // REMOVE_END

        // STEP_START cuckoo
        boolean res4 = jedis.cfAdd("other_users", "paolo");
        System.out.println(res4);  // >>> true

        boolean res5 = jedis.cfAdd("other_users", "kaitlyn");
        System.out.println(res5);  // >>> true

        boolean res6 = jedis.cfAdd("other_users", "rachel");
        System.out.println(res6);  // >>> true

        List<Boolean> res7 = jedis.cfMExists(
            "other_users",
            "paolo", "rachel", "andy"
        );
        System.out.println(res7);  // >>> [true, true, false]

        boolean res8 = jedis.cfDel("other_users", "paolo");
        System.out.println(res8);  // >>> true

        boolean res9 = jedis.cfExists("other_users", "paolo");
        System.out.println(res9);  // >>> false
        // STEP_END
        // REMOVE_START
        assertTrue(res4);
        assertTrue(res5);
        assertTrue(res6);
        assertEquals("[true, true, false]", res7.toString());
        assertTrue(res8);
        assertFalse(res9);
        // REMOVE_END

        // STEP_START hyperloglog
        long res10 = jedis.pfadd("group:1", "andy", "cameron", "david");
        System.out.println(res10);  // >>> 1

        long res11 = jedis.pfcount("group:1");
        System.out.println(res11);  // >>> 3

        long res12 = jedis.pfadd(
            "group:2",
            "kaitlyn", "michelle", "paolo", "rachel"
        );
        System.out.println(res12);  // >>> 1

        long res13 = jedis.pfcount("group:2");
        System.out.println(res13);  // >>> 4

        String res14 = jedis.pfmerge("both_groups", "group:1", "group:2");
        System.out.println(res14);  // >>> OK

        long res15 = jedis.pfcount("both_groups");
        System.out.println(res15);  // >>> 7
        // STEP_END
        // REMOVE_START
        assertEquals(1, res10);
        assertEquals(3, res11);
        assertEquals(1, res12);
        assertEquals(4, res13);
        assertEquals("OK", res14);
        assertEquals(7, res15);
        // REMOVE_END

        // STEP_START cms
        // Specify that you want to keep the counts within 0.01
        // (0.1%) of the true value with a 0.005 (0.05%) chance
        // of going outside this limit.
        String res16 = jedis.cmsInitByProb("items_sold", 0.01, 0.005);
        System.out.println(res16);  // >>> OK

        Map<String, Long> firstItemIncrements = new HashMap<>();
        firstItemIncrements.put("bread", 300L);
        firstItemIncrements.put("tea", 200L);
        firstItemIncrements.put("coffee", 200L);
        firstItemIncrements.put("beer", 100L);

        List<Long> res17 = jedis.cmsIncrBy("items_sold",
            firstItemIncrements
        );
        res17.sort(null);
        System.out.println();  // >>> [100, 200, 200, 300]

        Map<String, Long> secondItemIncrements = new HashMap<>();
        secondItemIncrements.put("bread", 100L);
        secondItemIncrements.put("coffee", 150L);

        List<Long> res18 = jedis.cmsIncrBy("items_sold",
            secondItemIncrements
        );
        res18.sort(null);
        System.out.println(res18);  // >>> [350, 400]

        List<Long> res19 = jedis.cmsQuery(
            "items_sold",
            "bread", "tea", "coffee", "beer"
        );
        res19.sort(null);
        System.out.println(res19);  // >>> [100, 200, 350, 400]
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res16);
        assertEquals("[100, 200, 200, 300]", res17.toString());
        assertEquals("[350, 400]", res18.toString());
        assertEquals("[100, 200, 350, 400]", res19.toString());
        // REMOVE_END

        // STEP_START tdigest
        String res20 = jedis.tdigestCreate("male_heights");
        System.out.println(res20);  // >>> OK

        String res21 = jedis.tdigestAdd("male_heights", 
            175.5, 181, 160.8, 152, 177, 196, 164);
        System.out.println(res21);  // >>> OK

        double res22 = jedis.tdigestMin("male_heights");
        System.out.println(res22);  // >>> 152.0

        double res23 = jedis.tdigestMax("male_heights");
        System.out.println(res23);  // >>> 196.0

        List<Double> res24 = jedis.tdigestQuantile("male_heights", 0.75);
        System.out.println(res24);  // >>> [181.0]

        // Note that the CDF value for 181 is not exactly 0.75.
        // Both values are estimates.
        List<Double> res25 = jedis.tdigestCDF("male_heights", 181);
        System.out.println(res25);  // >>> [0.7857142857142857]

        String res26 = jedis.tdigestCreate("female_heights");
        System.out.println(res26);  // >>> OK

        String res27 = jedis.tdigestAdd("female_heights",
            155.5, 161, 168.5, 170, 157.5, 163, 171);
        System.out.println(res27);  // >>> OK

        List<Double> res28 = jedis.tdigestQuantile("female_heights", 0.75);
        System.out.println(res28);  // >>> [170.0]

        String res29 = jedis.tdigestMerge(
            "all_heights",
            "male_heights", "female_heights"
        );
        System.out.println(res29);  // >>> OK
        List<Double> res30 = jedis.tdigestQuantile("all_heights", 0.75);
        System.out.println(res30);  // >>> [175.5]
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res20);
        assertEquals("OK", res21);
        assertEquals(152.0, res22);
        assertEquals(196.0, res23);
        assertEquals("[181.0]", res24.toString());
        assertEquals("[0.7857142857142857]", res25.toString());
        assertEquals("OK", res26);
        assertEquals("OK", res27);
        assertEquals("[170.0]", res28.toString());
        assertEquals("OK", res29);
        assertEquals("[175.5]", res30.toString());
        // REMOVE_END

        // STEP_START topk
        String res31 = jedis.topkReserve("top_3_songs", 3L, 2000L, 7L, 0.925D);
        System.out.println(res31);  // >>> OK

        Map<String, Long> songIncrements = new HashMap<>();
        songIncrements.put("Starfish Trooper", 3000L);
        songIncrements.put("Only one more time", 1850L);
        songIncrements.put("Rock me, Handel", 1325L);
        songIncrements.put("How will anyone know?", 3890L);
        songIncrements.put("Average lover", 4098L);
        songIncrements.put("Road to everywhere", 770L);

        List<String> res32 = jedis.topkIncrBy("top_3_songs",
            songIncrements
        );
        System.out.println(res32);
        // >>> [null, null, null, null, null, Rock me, Handel]

        List<String> res33 = jedis.topkList("top_3_songs");
        System.out.println(res33);
        // >>> [Average lover, How will anyone know?, Starfish Trooper]

        List<Boolean> res34 = jedis.topkQuery("top_3_songs",
            "Starfish Trooper", "Road to everywhere"
        );
        System.out.println(res34);
        // >>> [true, false]
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res31);
        // Value of res32 is not deterministic.
        assertEquals("[Average lover, How will anyone know?, Starfish Trooper]", res33.toString());
        assertEquals("[true, false]", res34.toString());
        // REMOVE_END
    }
}