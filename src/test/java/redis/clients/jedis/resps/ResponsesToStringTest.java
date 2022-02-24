package redis.clients.jedis.resps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import redis.clients.jedis.GeoCoordinate;

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
        assertThat(toStringResult, CoreMatchers.containsString("key-name"));
        assertThat(toStringResult, CoreMatchers.containsString("elem"));

        // test hashCode
        assertEquals(elem.hashCode(), elem_copy.hashCode());
    }

    @Test
    public void KeyedZSetElementTest() {
        // test equals
        KeyedZSetElement elem = new KeyedZSetElement("key-name", "elem", 1d);
        KeyedZSetElement elem_copy = new KeyedZSetElement("key-name", "elem", 1d);
        assertEquals(elem, elem);
        assertEquals(elem, elem_copy);
        assertNotEquals(elem, new Object());

        // test toString
        String toStringResult = elem.toString();
        assertThat(toStringResult, CoreMatchers.containsString("key=key-name"));
        assertThat(toStringResult, CoreMatchers.containsString("elem"));
        assertThat(toStringResult, CoreMatchers.containsString("1"));

        // test hashCode
        assertEquals(elem.hashCode(), elem_copy.hashCode());
    }

    @Test
    public void GeoRadiusResponse() {
        byte[] member = { 0x01, 0x02, 0x03, 0x04 };

        GeoRadiusResponse response = new GeoRadiusResponse(member);
        response.setDistance(5);
        response.setCoordinate(new GeoCoordinate(2,3));
        response.setRawScore(10);

        GeoRadiusResponse response_copy = new GeoRadiusResponse(member);
        response_copy.setDistance(5);
        response_copy.setCoordinate(new GeoCoordinate(2,3));
        response_copy.setRawScore(10);

        assertEquals(response, response);
        assertEquals(response, response_copy);
        assertNotEquals(response, new Object());
    }
}
