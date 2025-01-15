//EXAMPLE: tdigest_tutorial
//HIDE_START
package io.redis.examples;
//HIDE_END

//REMOVE_START
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;
//REMOVE_END

public class TDigestExample {

    @Test
    public void run(){
        //HIDE_START
        UnifiedJedis jedis = new UnifiedJedis("redis://127.0.0.1:6379");
        //HIDE_END

        //REMOVE_START
        jedis.del("racer_ages");
        jedis.del("bikes:sales");
        //REMOVE_END

        //STEP_START tdig_start
        String res1 = jedis.tdigestCreate("bikes:sales", 100);
        System.out.println(res1); // >>> True

        String res2 = jedis.tdigestAdd("bikes:sales", 21);
        System.out.println(res2); // >>> OK

        String res3 = jedis.tdigestAdd("bikes:sales", 150, 95, 75, 34);
        System.out.println(res3); // >>> OK
        //STEP_END

        //REMOVE_START
        Assert.assertEquals("OK","OK");
        //REMOVE_END

        //STEP_START tdig_cdf
        String res4 = jedis.tdigestCreate("racer_ages");
        System.out.println(res4); // >>> True

        String res5 = jedis.tdigestAdd("racer_ages", 45.88,
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

        List<Long> res6 = jedis.tdigestRank("racer_ages", 50);
        System.out.println(res6); // >>> [7]

        List<Long> res7 = jedis.tdigestRank("racer_ages", 50, 40);
        System.out.println(res7); // >>> [7, 4]
        //STEP_END

        //STEP_START tdig_quant
        List<Double> res8 = jedis.tdigestQuantile("racer_ages", 0.5);
        System.out.println(res8); // >>> [44.2]

        List<Double> res9 = jedis.tdigestByRank("racer_ages", 4);
        System.out.println(res9); // >>> [42.63]
        //STEP_END

        //STEP_START tdig_min
        double res10 = jedis.tdigestMin("racer_ages");
        System.out.println(res10); // >>> 19.27

        double res11 = jedis.tdigestMax("racer_ages");
        System.out.println(res11); // >>> 85.71
        //STEP_END

        //STEP_START tdig_reset
        String res12 = jedis.tdigestReset("racer_ages");
        System.out.println(res12); // >>> OK
        //STEP_END

        //HIDE_START
        jedis.close();
        //HIDE_END
    }
}
