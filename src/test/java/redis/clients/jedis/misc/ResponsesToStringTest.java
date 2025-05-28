package redis.clients.jedis.misc;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.resps.GeoRadiusResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponsesToStringTest {

  @Test
  public void GeoRadiusResponse() {
    byte[] member = {0x01, 0x02, 0x03, 0x04};

    GeoRadiusResponse response = new GeoRadiusResponse(member);
    response.setDistance(5);
    response.setCoordinate(new GeoCoordinate(2, 3));
    response.setRawScore(10);

    GeoRadiusResponse response_copy = new GeoRadiusResponse(member);
    response_copy.setDistance(5);
    response_copy.setCoordinate(new GeoCoordinate(2, 3));
    response_copy.setRawScore(10);

    assertTrue(response.equals(response));
    assertEquals(response, response_copy);
    assertNotEquals(response, new Object());
  }
}
