//EXAMPLE: cms_tutorial
//HIDE_START
package io.redis.examples;
//HIDE_END

//REMOVE_START
import redis.clients.jedis.UnifiedJedis;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
//REMOVE_END

public class CMSExample {

  @Test
  public void run() {

    //HIDE_START
    UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
    //HIDE_END

    //REMOVE_START
    jedis.del("bikes:profit");
    //REMOVE_END

    //STEP_START cms
    String res1 = jedis.cmsInitByProb("bikes:profit", 0.001d, 0.002d);
    System.out.println(res1); // >>> OK

    long res2 = jedis.cmsIncrBy("bikes:profit", "Smoky Mountain Striker", 100L);
    System.out.println(res2); // >>> 100

    List<Long> res3 = jedis.cmsIncrBy("bikes:profit", new HashMap<String, Long>() {{
      put("Rocky Mountain Racer", 200L);
      put("Cloudy City Cruiser", 150L);
    }});
    System.out.println(res3); // >>> [200, 150]

    List<Long> res4 = jedis.cmsQuery("bikes:profit", "Smoky Mountain Striker");
    System.out.println(res4); // >>> [100]

    Map<String, Object> res5 = jedis.cmsInfo("bikes:profit");
    System.out.println(res5.get("width") + " " + res5.get("depth") + " " + res5.get("count")); // >>> 2000 9 450
    //STEP_END
  }

}
