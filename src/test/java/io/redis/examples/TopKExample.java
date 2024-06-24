//EXAMPLE: topk_tutorial
//HIDE_START
package io.redis.examples;
//HIDE_END

//REMOVE_START
import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;

import java.util.List;
//REMOVE_END

public class TopKExample {
    @Test
    public void run(){
        //HIDE_START
        UnifiedJedis unifiedJedis = new UnifiedJedis("redis://127.0.0.1:6379");
        //HIDE_END

        //REMOVE_START
        unifiedJedis.del("bikes:keywords");
        //REMOVE_END

        //STEP_START topk
        String res1 = unifiedJedis.topkReserve("bikes:keywords", 5L, 2000L, 7L, 0.925D);
        System.out.println(res1); // >>> True

        List<String> res2 = unifiedJedis.topkAdd("bikes:keywords",
                "store",
                "seat",
                "handlebars",
                "handles",
                "pedals",
                "tires",
                "store",
                "seat");

        System.out.println(res2); // >>> [None, None, None, None, None, 'handlebars', None, None]

        List<String> res3 = unifiedJedis.topkList("bikes:keywords");
        System.out.println(res3); // >>> ['store', 'seat', 'pedals', 'tires', 'handles']

        List<Boolean> res4 = unifiedJedis.topkQuery("bikes:keywords", "store", "handlebars");
        System.out.println(res4); // >>> [1, 0]
        //STEP_END
    }
}
