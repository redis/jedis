// EXAMPLE: vecset_tutorial
// REMOVE_START
package io.redis.examples;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
// REMOVE_END
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.params.VSimParams;

import java.util.*;

public class VectorSetExample {

  @Test
  public void run() {
    try (UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379")) {
      // REMOVE_START
      jedis.del("points", "quantSetQ8", "quantSetNoQ", "quantSetBin", "setNotReduced",
        "setReduced");
      // REMOVE_END

      // STEP_START vadd
      boolean res1 = jedis.vadd("points", new float[] { 1.0f, 1.0f }, "pt:A");
      System.out.println(res1); // >>> true

      boolean res2 = jedis.vadd("points", new float[] { -1.0f, -1.0f }, "pt:B");
      System.out.println(res2); // >>> true

      boolean res3 = jedis.vadd("points", new float[] { -1.0f, 1.0f }, "pt:C");
      System.out.println(res3); // >>> true

      boolean res4 = jedis.vadd("points", new float[] { 1.0f, -1.0f }, "pt:D");
      System.out.println(res4); // >>> true

      boolean res5 = jedis.vadd("points", new float[] { 1.0f, 0.0f }, "pt:E");
      System.out.println(res5); // >>> true

      String res6 = jedis.type("points");
      System.out.println(res6); // >>> vectorset
      // STEP_END
      // REMOVE_START
      assertTrue(res1);
      assertTrue(res2);
      assertTrue(res3);
      assertTrue(res4);
      assertTrue(res5);
      assertEquals("vectorset", res6);
      // REMOVE_END

      // STEP_START vcardvdim
      long res7 = jedis.vcard("points");
      System.out.println(res7); // >>> 5

      long res8 = jedis.vdim("points");
      System.out.println(res8); // >>> 2
      // STEP_END
      // REMOVE_START
      assertEquals(5L, res7);
      assertEquals(2L, res8);
      // REMOVE_END

      // STEP_START vemb
      List<Double> res9 = jedis.vemb("points", "pt:A");
      System.out.println(res9); // >>> [0.9999999..., 0.9999999...]

      List<Double> res10 = jedis.vemb("points", "pt:B");
      System.out.println(res10); // >>> [-0.9999999..., -0.9999999...]

      List<Double> res11 = jedis.vemb("points", "pt:C");
      System.out.println(res11); // >>> [-0.9999999..., 0.9999999...]

      List<Double> res12 = jedis.vemb("points", "pt:D");
      System.out.println(res12); // >>> [0.9999999..., -0.9999999...]

      List<Double> res13 = jedis.vemb("points", "pt:E");
      System.out.println(res13); // >>> [1, 0]
      // STEP_END
      // REMOVE_START
      assertTrue(Math.abs(1 - res9.get(0)) < 0.01);
      assertTrue(Math.abs(1 - res9.get(1)) < 0.01);
      assertTrue(Math.abs(-1 - res10.get(0)) < 0.01);
      assertTrue(Math.abs(-1 - res10.get(1)) < 0.01);
      assertTrue(Math.abs(-1 - res11.get(0)) < 0.01);
      assertTrue(Math.abs(1 - res11.get(1)) < 0.01);
      assertTrue(Math.abs(1 - res12.get(0)) < 0.01);
      assertTrue(Math.abs(-1 - res12.get(1)) < 0.01);
      assertEquals(Arrays.asList(1.0, 0.0), res13);
      // REMOVE_END

      // STEP_START attr
      boolean res14 = jedis.vsetattr("points", "pt:A",
        "{\"name\":\"Point A\",\"description\":\"First point added\"}");
      System.out.println(res14); // >>> true

      String res15 = jedis.vgetattr("points", "pt:A");
      System.out.println(res15);
      // >>> {"name":"Point A","description":"First point added"}

      boolean res16 = jedis.vsetattr("points", "pt:A", "");
      System.out.println(res16); // >>> true

      String res17 = jedis.vgetattr("points", "pt:A");
      System.out.println(res17); // >>> null
      // STEP_END
      // REMOVE_START
      assertTrue(res14);
      assertTrue(res15.contains("\"name\":\"Point A\""));
      assertTrue(res15.contains("\"description\":\"First point added\""));
      assertTrue(res16);
      assertNull(res17);
      // REMOVE_END

      // STEP_START vrem
      boolean res18 = jedis.vadd("points", new float[] { 0.0f, 0.0f }, "pt:F");
      System.out.println(res18); // >>> true

      long res19 = jedis.vcard("points");
      System.out.println(res19); // >>> 6

      boolean res20 = jedis.vrem("points", "pt:F");
      System.out.println(res20); // >>> true

      long res21 = jedis.vcard("points");
      System.out.println(res21); // >>> 5
      // STEP_END
      // REMOVE_START
      assertTrue(res18);
      assertEquals(6L, res19);
      assertTrue(res20);
      assertEquals(5L, res21);
      // REMOVE_END

      // STEP_START vsim_basic
      List<String> res22 = jedis.vsim("points", new float[] { 0.9f, 0.1f });
      System.out.println(res22);
      // >>> ["pt:E", "pt:A", "pt:D", "pt:C", "pt:B"]
      // STEP_END
      // REMOVE_START
      assertEquals(Arrays.asList("pt:E", "pt:A", "pt:D", "pt:C", "pt:B"), res22);
      // REMOVE_END

      // STEP_START vsim_options
      Map<String, Double> res23 = jedis.vsimByElementWithScores("points", "pt:A",
        new VSimParams().count(4));
      System.out.println(res23);
      // >>> {pt:A=1.0, pt:E≈0.85355, pt:D=0.5, pt:C=0.5}
      // STEP_END
      // REMOVE_START
      assertEquals(1.0, res23.get("pt:A"), 0.0001);
      assertEquals(0.5, res23.get("pt:C"), 0.0001);
      assertEquals(0.5, res23.get("pt:D"), 0.0001);
      assertTrue(Math.abs(res23.get("pt:E") - 0.8535) < 0.01);
      // REMOVE_END

      // STEP_START vsim_filter
      boolean res24 = jedis.vsetattr("points", "pt:A", "{\"size\":\"large\",\"price\":18.99}");
      System.out.println(res24); // >>> true

      boolean res25 = jedis.vsetattr("points", "pt:B", "{\"size\":\"large\",\"price\":35.99}");
      System.out.println(res25); // >>> true

      boolean res26 = jedis.vsetattr("points", "pt:C", "{\"size\":\"large\",\"price\":25.99}");
      System.out.println(res26); // >>> true

      boolean res27 = jedis.vsetattr("points", "pt:D", "{\"size\":\"small\",\"price\":21.00}");
      System.out.println(res27); // >>> true

      boolean res28 = jedis.vsetattr("points", "pt:E", "{\"size\":\"small\",\"price\":17.75}");
      System.out.println(res28); // >>> true

      List<String> res29 = jedis.vsimByElement("points", "pt:A",
        new VSimParams().filter(".size == \"large\""));
      System.out.println(res29); // >>> ["pt:A", "pt:C", "pt:B"]

      List<String> res30 = jedis.vsimByElement("points", "pt:A",
        new VSimParams().filter(".size == \"large\" && .price > 20.00"));
      System.out.println(res30); // >>> ["pt:C", "pt:B"]
      // STEP_END
      // REMOVE_START
      assertTrue(res24);
      assertTrue(res25);
      assertTrue(res26);
      assertTrue(res27);
      assertTrue(res28);
      assertEquals(Arrays.asList("pt:C", "pt:B"), res30);
      // REMOVE_END

      // STEP_START add_quant
      boolean res31 = jedis.vadd("quantSetQ8", new float[] { 1.262185f, 1.958231f }, "quantElement",
        new VAddParams().q8());
      System.out.println(res31); // >>> true

      List<Double> res32 = jedis.vemb("quantSetQ8", "quantElement");
      System.out.println("Q8: " + res32);
      // >>> Q8: [~1.264, ~1.958]

      boolean res33 = jedis.vadd("quantSetNoQ", new float[] { 1.262185f, 1.958231f },
        "quantElement", new VAddParams().noQuant());
      System.out.println(res33); // >>> true

      List<Double> res34 = jedis.vemb("quantSetNoQ", "quantElement");
      System.out.println("NOQUANT: " + res34);
      // >>> NOQUANT: [~1.262185, ~1.958231]

      boolean res35 = jedis.vadd("quantSetBin", new float[] { 1.262185f, 1.958231f },
        "quantElement", new VAddParams().bin());
      System.out.println(res35); // >>> true

      List<Double> res36 = jedis.vemb("quantSetBin", "quantElement");
      System.out.println("BIN: " + res36);
      // >>> BIN: [1, 1]
      // STEP_END
      // REMOVE_START
      assertTrue(res31);
      assertTrue(res33);
      assertTrue(res35);
      assertEquals(2, res32.size());
      assertEquals(2, res34.size());
      assertEquals(2, res36.size());
      assertTrue(Math.abs(res32.get(0) - 1.26) < 0.05);
      assertTrue(Math.abs(res32.get(1) - 1.958) < 0.01);
      assertTrue(Math.abs(res34.get(0) - 1.2622) < 0.01);
      assertTrue(Math.abs(res34.get(1) - 1.9582) < 0.01);
      assertEquals(Arrays.asList(1.0, 1.0), res36);
      // REMOVE_END

      // STEP_START add_reduce
      float[] values = new float[300];
      for (int i = 0; i < 300; i++)
        values[i] = i / 299.0f;

      boolean res37 = jedis.vadd("setNotReduced", values, "element");
      System.out.println(res37); // >>> true

      long res38 = jedis.vdim("setNotReduced");
      System.out.println(res38); // >>> 300

      boolean res39 = jedis.vadd("setReduced", values, "element", 100, new VAddParams());
      System.out.println(res39); // >>> true

      long res40 = jedis.vdim("setReduced");
      System.out.println(res40); // >>> 100
      // STEP_END
      // REMOVE_START
      assertTrue(res37);
      assertEquals(300L, res38);
      assertTrue(res39);
      assertEquals(100L, res40);
      // REMOVE_END

      // HIDE_START
      jedis.close();
    }
  }
}
// HIDE_END
