// EXAMPLE: hll_tutorial
package io.redis.examples;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;
public class HyperLogLogExample {

    @Test
    public void run() {
        // HIDE_START
        UnifiedJedis unifiedJedis = new UnifiedJedis("redis://localhost:6379");
        // HIDE_END

        // REMOVE_START
        unifiedJedis.del("bikes", "commuter_bikes", "all_bikes");
        // REMOVE_END

        // STEP_START pfadd
        long res1 = unifiedJedis.pfadd("bikes", "Hyperion", "Deimos", "Phoebe", "Quaoar");
        System.out.println(res1); // >>> 1

        long res2 = unifiedJedis.pfcount("bikes");
        System.out.println(res2); // >>> 4

        long res3 = unifiedJedis.pfadd("commuter_bikes", "Salacia", "Mimas", "Quaoar");
        System.out.println(res3); // >>> 1

        String res4 = unifiedJedis.pfmerge("all_bikes", "bikes", "commuter_bikes");
        System.out.println(res4); // >>> True

        long res5 = unifiedJedis.pfcount("all_bikes");
        System.out.println(res5); // >>> 6
        // STEP_END

        // REMOVE_START
        Assert.assertEquals("OK", res4);
        // REMOVE_END
    }
}
