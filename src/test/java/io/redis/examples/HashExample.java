//EXAMPLE: hash_tutorial
package io.redis.examples;

import redis.clients.jedis.UnifiedJedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
//REMOVE_START
import org.junit.Test;
import static org.junit.Assert.assertEquals;
//REMOVE_END

public class HashExample {
  @Test
  public void run() {
    try (UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379")) {
      // REMOVE_START
      jedis.del("bike:1", "bike:1:stats");
      // REMOVE_END

      // STEP_START set_get_all
      Map<String, String> bike1 = new HashMap<>();
      bike1.put("model", "Deimos");
      bike1.put("brand", "Ergonom");
      bike1.put("type", "Enduro bikes");
      bike1.put("price", "4972");

      Long res1 = jedis.hset("bike:1", bike1);
      System.out.println(res1); // 4

      String res2 = jedis.hget("bike:1", "model");
      System.out.println(res2); // Deimos

      String res3 = jedis.hget("bike:1", "price");
      System.out.println(res3); // 4972

      Map<String, String> res4 = jedis.hgetAll("bike:1");
      System.out.println(res4); // {type=Enduro bikes, brand=Ergonom, price=4972, model=Deimos}
      // STEP_END

      // REMOVE_START
      assertEquals(4, res1.longValue());
      assertEquals("Deimos", res2);
      assertEquals("4972", res3);
      assertEquals("Deimos", res4.get("model"));
      assertEquals("Ergonom", res4.get("brand"));
      assertEquals("Enduro bikes", res4.get("type"));
      assertEquals("4972", res4.get("price"));
      // REMOVE_END

      // STEP_START hmget
      List<String> res5 = jedis.hmget("bike:1", "model", "price");
      System.out.println(res5); // [Deimos, 4972]
      // STEP_END

      // REMOVE_START
      assert res5.toString().equals("[Deimos, 4972]");
      // REMOVE_END

      // STEP_START hincrby
      Long res6 = jedis.hincrBy("bike:1", "price", 100);
      System.out.println(res6); // 5072
      Long res7 = jedis.hincrBy("bike:1", "price", -100);
      System.out.println(res7); // 4972
      // STEP_END

      // REMOVE_START
      assertEquals(5072, res6.longValue());
      assertEquals(4972, res7.longValue());
      // REMOVE_END

      // STEP_START incrby_get_mget
      Long res8 = jedis.hincrBy("bike:1:stats", "rides", 1);
      System.out.println(res8); // 1
      Long res9 = jedis.hincrBy("bike:1:stats", "rides", 1);
      System.out.println(res9); // 2
      Long res10 = jedis.hincrBy("bike:1:stats", "rides", 1);
      System.out.println(res10); // 3
      Long res11 = jedis.hincrBy("bike:1:stats", "crashes", 1);
      System.out.println(res11); // 1
      Long res12 = jedis.hincrBy("bike:1:stats", "owners", 1);
      System.out.println(res12); // 1
      String res13 = jedis.hget("bike:1:stats", "rides");
      System.out.println(res13); // 3
      List<String> res14 = jedis.hmget("bike:1:stats", "crashes", "owners");
      System.out.println(res14); // [1, 1]
      // STEP_END

      // REMOVE_START
      assertEquals(1, res8.longValue());
      assertEquals(2, res9.longValue());
      assertEquals(3, res10.longValue());
      assertEquals(1, res11.longValue());
      assertEquals(1, res12.longValue());
      assertEquals("3", res13);
      assertEquals("[1, 1]", res14.toString());
      // REMOVE_END
    }
  }
}
