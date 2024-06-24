// EXAMPLE: set_tutorial
package io.redis.examples;

//REMOVE_START
import org.junit.Test;
import static org.junit.Assert.*;
//REMOVE_END

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringExample {
  @Test
  public void run() {
    try (UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379")) {

      // STEP_START set_get
      String res1 = jedis.set("bike:1", "Deimos");
      System.out.println(res1); // OK
      String res2 = jedis.get("bike:1");
      System.out.println(res2); // Deimos
      // STEP_END

      // REMOVE_START
      assertEquals("OK", res1);
      assertEquals("Deimos", res2);
      // REMOVE_END

      // STEP_START setnx_xx
      Long res3 = jedis.setnx("bike:1", "bike");
      System.out.println(res3); // 0 (because key already exists)
      System.out.println(jedis.get("bike:1")); // Deimos (value is unchanged)
      String res4 = jedis.set("bike:1", "bike", SetParams.setParams().xx()); // set the value to "bike" if it
      // already
      // exists
      System.out.println(res4); // OK
      // STEP_END

      // REMOVE_START
      assertEquals(0L, res3.longValue());
      assertEquals("OK", res4);
      // REMOVE_END

      // STEP_START mset
      String res5 = jedis.mset("bike:1", "Deimos", "bike:2", "Ares", "bike:3", "Vanth");
      System.out.println(res5); // OK
      List<String> res6 = jedis.mget("bike:1", "bike:2", "bike:3");
      System.out.println(res6); // [Deimos, Ares, Vanth]
      // STEP_END

      // REMOVE_START
      assertEquals("OK", res5);
      List<String> expected = new ArrayList<>(Arrays.asList("Deimos", "Ares", "Vanth"));
      assertEquals(expected, res6);
      // REMOVE_END

      // STEP_START incr
      jedis.set("total_crashes", "0");
      Long res7 = jedis.incr("total_crashes");
      System.out.println(res7); // 1
      Long res8 = jedis.incrBy("total_crashes", 10);
      System.out.println(res8); // 11
      // STEP_END

      // REMOVE_START
      assertEquals(1L, res7.longValue());
      assertEquals(11L, res8.longValue());
      // REMOVE_END
    }
  }
}
