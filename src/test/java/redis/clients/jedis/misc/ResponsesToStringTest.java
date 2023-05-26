package redis.clients.jedis.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.KeyedListElement;

public class ResponsesToStringTest {

  @Test
  public void KeyedListElementTest() {
    // test equals
    KeyedListElement elem = new KeyedListElement("key-name", "elem");
    KeyedListElement elem_copy = new KeyedListElement("key-name", "elem");
    assertEquals(elem, elem);
    assertEquals(elem, elem_copy);
    assertNotEquals(elem, new Object());

    // test toString
    String toStringResult = elem.toString();
    //assertThat(toStringResult, containsString("key-name"));
    //assertThat(toStringResult, containsString("elem"));
    assertEquals("key-name=elem", toStringResult);

    // test hashCode
    assertEquals(elem.hashCode(), elem_copy.hashCode());
  }

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

    assertEquals(response, response);
    assertEquals(response, response_copy);
    assertNotEquals(response, new Object());
  }
}
