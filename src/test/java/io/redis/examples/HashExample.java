//EXAMPLE: hash_example
//HIDE_START
package io.redis.examples;

import redis.clients.jedis.UnifiedJedis;

import java.util.Map;
//REMOVE_START
import org.junit.Test;
import static org.junit.Assert.assertEquals;
//REMOVE_END

public class HashExample {
  @Test
  public void run() {
    try (UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379")) {
      //REMOVE_START
      jedis.del("bike:1", "bike:2", "bike:2:stats");
      //REMOVE_END

      //HIDE_END
      //STEP_START set_get_getall
      var res1 = jedis.hset("bike:1", Map.of(
          "model", "Deimos", //
          "brand", "Ergonom", //
          "type", "Enduro bikes", //
          "price", "4972"
      ));
      System.out.println(res1); // 4

      var res2 = jedis.hget("bike:1", "model");
      System.out.println(res2); // Deimos

      var res3 = jedis.hget("bike:1", "price");
      System.out.println(res3); // 4972

      var res4 = jedis.hgetAll("bike:1");
      System.out.println(res4); // {type=Enduro bikes, brand=Ergonom, price=4972, model=Deimos}
      //STEP_END

      //REMOVE_START
      assertEquals(4, res1);
      assertEquals("Deimos", res2);
      assertEquals("4972", res3);
      assertEquals("Deimos", res4.get("model"));
      assertEquals("Ergonom", res4.get("brand"));
      assertEquals("Enduro bikes", res4.get("type"));
      assertEquals("4972", res4.get("price"));
      //REMOVE_END

      //STEP_START hmget
      var res5 = jedis.hmget("bike:1", "model", "price");
      System.out.println(res5); // [Deimos, 4972]
      //STEP_END

      //REMOVE_START
      assert res5.toString().equals("[Deimos, 4972]");
      //REMOVE_END

      //STEP_START hincrby
      var res6 = jedis.hincrBy("bike:1", "price", 100);
      System.out.println(res6); // 5072
      var res7 = jedis.hincrBy("bike:1", "price", -100);
      System.out.println(res7); // 4972
      //STEP_END

      //REMOVE_START
      assertEquals(5072, res6);
      assertEquals(4972, res7);
      //REMOVE_END

      //STEP_START example_set_get
      var res8 = jedis.hset("bike:2", Map.of(
          "model", "Vanth", //
          "brand", "Tots", //
          "type", "eBikes", //
          "price", "4971"
      ));

      System.out.println(res8); // 4
      var res9 = jedis.hget("bike:2", "model");
      System.out.println(res9); // Vanth
      var res10 = jedis.hgetAll("bike:2");
      System.out.println(res10); // {type=eBikes, brand=Tots, price=4971, model=Vanth}
      //STEP_END

      //REMOVE_START
      assertEquals(4, res8);
      assertEquals("Vanth", res9);
      assertEquals("Vanth", res10.get("model"));
      assertEquals("Tots", res10.get("brand"));
      assertEquals("eBikes", res10.get("type"));
      assertEquals("4971", res10.get("price"));
      //REMOVE_END

      //STEP_START incryby_get_mget
      var res11 = jedis.hincrBy("bike:2:stats", "rides", 1);
      System.out.println(res11); // 1
      var res12 = jedis.hincrBy("bike:2:stats", "rides", 1);
      System.out.println(res12); // 2
      var res13 = jedis.hincrBy("bike:2:stats", "rides", 1);
      System.out.println(res13); // 3
      var res14 = jedis.hincrBy("bike:2:stats", "crashes", 1);
      System.out.println(res14); // 1
      var res15 = jedis.hincrBy("bike:2:stats", "owners", 1);
      System.out.println(res15); // 1
      var res16 = jedis.hget("bike:2:stats", "rides");
      System.out.println(res16); // 3
      var res17 = jedis.hmget("bike:2:stats", "crashes", "owners");
      System.out.println(res17); // [1, 1]
      //STEP_END

      //REMOVE_START
      assertEquals(1, res11);
      assertEquals(2, res12);
      assertEquals(3, res13);
      assertEquals(1, res14);
      assertEquals(1, res15);
      assertEquals("3", res16);
      assertEquals("[1, 1]", res17.toString());
      //REMOVE_END
    }
  }
}
