package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StreamEntryIDTest {

  // Redis stream IDs are two unsigned 64-bit integers; values in the upper half
  // of the range must round-trip unchanged.
  @Test
  public void maxSequenceRoundTrips() {
    StreamEntryID id = new StreamEntryID("0-18446744073709551615");
    assertEquals(0L, id.getTime());
    assertEquals(-1L, id.getSequence()); // 2^64-1 stored as raw bits
    assertEquals("0-18446744073709551615", id.toString());
  }

  @Test
  public void maxTimeRoundTrips() {
    StreamEntryID id = new StreamEntryID("18446744073709551615-0");
    assertEquals("18446744073709551615-0", id.toString());
  }

  @Test
  public void ordersHighValuesAsUnsigned() {
    StreamEntryID low = new StreamEntryID("0-1");
    StreamEntryID high = new StreamEntryID("0-9223372036854775808"); // 2^63
    assertTrue(high.compareTo(low) > 0);
    assertTrue(low.compareTo(high) < 0);

    StreamEntryID smallTime = new StreamEntryID("1-0");
    StreamEntryID bigTime = new StreamEntryID("9223372036854775808-0");
    assertTrue(bigTime.compareTo(smallTime) > 0);
  }

  @Test
  public void commonIdsUnchanged() {
    StreamEntryID a = new StreamEntryID("1526919030474-55");
    assertEquals("1526919030474-55", a.toString());
    StreamEntryID b = new StreamEntryID("1526919030474-56");
    assertTrue(b.compareTo(a) > 0);
    assertEquals(a, new StreamEntryID("1526919030474-55"));
  }
}
