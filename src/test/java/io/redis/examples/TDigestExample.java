//EXAMPLE: tdigest_tutorial
//HIDE_START
package io.redis.examples;
//HIDE_END

//REMOVE_START
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;
import java.util.List;
//REMOVE_END

public class TDigestExample {
    @Test
    public void run(){
        //HIDE_START
        UnifiedJedis unifiedJedis = new UnifiedJedis("redis://127.0.0.1:6379");
        //HIDE_END

        //REMOVE_START
        unifiedJedis.del("racer_ages");
        unifiedJedis.del("bikes:sales");
        //REMOVE_END

        //STEP_START tdig_start
        String res1 = unifiedJedis.tdigestCreate("bikes:sales", 100);
        System.out.println(res1); // >>> True

        String res2 = unifiedJedis.tdigestAdd("bikes:sales", 21);
        System.out.println(res2); // >>> OK

        String res3 = unifiedJedis.tdigestAdd("bikes:sales", 150, 95, 75, 34);
        System.out.println(res3); // >>> OK
        //STEP_END

        //REMOVE_START
        Assert.assertEquals("OK","OK");
        //REMOVE_END

        //STEP_START tdig_cdf
        String res4 = unifiedJedis.tdigestCreate("racer_ages");
        System.out.println(res4); // >>> True

        String res5 = unifiedJedis.tdigestAdd("racer_ages", 45.88,
                44.2,
                58.03,
                19.76,
                39.84,
                69.28,
                50.97,
                25.41,
                19.27,
                85.71,
                42.63);
        System.out.println(res5); // >>> OK

        List<Long> res6 = unifiedJedis.tdigestRank("racer_ages", 50);
        System.out.println(res6); // >>> [7]

        List<Long> res7 = unifiedJedis.tdigestRank("racer_ages", 50, 40);
        System.out.println(res7); // >>> [7, 4]
        //STEP_END

        //STEP_START tdig_quant
        List<Double> res8 = unifiedJedis.tdigestQuantile("racer_ages", 0.5);
        System.out.println(res8); // >>> [44.2]

        List<Double> res9 = unifiedJedis.tdigestByRank("racer_ages", 4);
        System.out.println(res9); // >>> [42.63]
        //STEP_END

        //STEP_START tdig_min
        double res10 = unifiedJedis.tdigestMin("racer_ages");
        System.out.println(res10); // >>> 19.27

        double res11 = unifiedJedis.tdigestMax("racer_ages");
        System.out.println(res11); // >>> 85.71
        //STEP_END

        //STEP_START tdig_reset
        String res12 = unifiedJedis.tdigestReset("racer_ages");
        System.out.println(res12); // >>> OK
        //STEP_END
    }
}
