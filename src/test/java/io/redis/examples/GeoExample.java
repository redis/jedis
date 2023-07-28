//EXAMPLE: geo_tutorial
package io.redis.examples;

// REMOVE_START
import org.junit.Test;
// REMOVE_END
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.resps.GeoRadiusResponse;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class GeoExample {
  @Test
  public void run() {
    try (UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379")) {
      // REMOVE_START
      jedis.del("bikes:rentable");
      // REMOVE_END

      // STEP_START geoadd
      long res1 = jedis.geoadd("bikes:rentable", -122.27652, 37.805186, "station:1");
      System.out.println(res1); // 1

      long res2 = jedis.geoadd("bikes:rentable", -122.2674626, 37.8062344, "station:2");
      System.out.println(res2); // 1

      long res3 = jedis.geoadd("bikes:rentable", -122.2469854, 37.8104049, "station:3");
      System.out.println(res2); // 1
      // STEP_END

      // REMOVE_START
      assertEquals(1, res1);
      assertEquals(1, res1);
      assertEquals(1, res1);
      // REMOVE_END

      // STEP_START geosearch
      List<GeoRadiusResponse> res4 = jedis.geosearch(
          "bikes:rentable",
          new GeoCoordinate(-122.27652, 37.805186),
          5,
          GeoUnit.KM
      );
      List<String> members = res4.stream() //
          .map(GeoRadiusResponse::getMemberByString) //
          .collect(Collectors.toList());
      System.out.println(members); // [station:1, station:2, station:3]
      // STEP_END

      // REMOVE_START
      assertEquals("[station:1, station:2, station:3]", members.toString());
      // REMOVE_END
    }
  }
}
