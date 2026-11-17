package redis.clients.jedis.misc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.util.SafeEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeoRadiusResponseTest {

  @Test
  public void compareResponsesWithoutCoordinates() {
    GeoRadiusResponse first = new GeoRadiusResponse(SafeEncoder.encode("member"));
    GeoRadiusResponse second = new GeoRadiusResponse(SafeEncoder.encode("member"));

    assertEquals(first, second);
    assertEquals(second, first);
    assertEquals(first.hashCode(), second.hashCode());
    assertTrue(new HashSet<>(Collections.singleton(first)).contains(second));
  }

  @Test
  public void compareDifferentMembersWithoutCoordinates() {
    GeoRadiusResponse first = new GeoRadiusResponse(SafeEncoder.encode("first"));
    GeoRadiusResponse second = new GeoRadiusResponse(SafeEncoder.encode("second"));

    assertNotEquals(first, second);
    assertNotEquals(second, first);
  }

  @Test
  public void compareResponseWithAndWithoutCoordinates() {
    GeoRadiusResponse first = new GeoRadiusResponse(SafeEncoder.encode("member"));
    GeoRadiusResponse second = new GeoRadiusResponse(SafeEncoder.encode("member"));
    second.setCoordinate(new GeoCoordinate(2, 3));

    assertNotEquals(first, second);
    assertNotEquals(second, first);
  }

  @Test
  public void compareBuiltResponsesWithoutCoordinates() {
    List<Object> reply = Collections
        .singletonList(Arrays.asList(SafeEncoder.encode("member"), SafeEncoder.encode("1.5"), 10L));

    List<GeoRadiusResponse> first = BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(reply);
    List<GeoRadiusResponse> second = BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(reply);

    assertEquals(first, second);
    assertEquals(1.5, first.get(0).getDistance());
    assertEquals(10L, first.get(0).getRawScore());
  }
}
