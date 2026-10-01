package redis.clients.jedis.args;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.SafeEncoder;

public class BlessFlagTest {

  @Test
  public void noEvictUsesHyphenatedWireToken() {
    assertArrayEquals(SafeEncoder.encode("NO-EVICT"), BlessFlag.NO_EVICT.getRaw());
    assertEquals("NO-EVICT", BlessFlag.NO_EVICT.toString());
  }

  @Test
  public void ofForwardsTokenVerbatim() {
    BlessFlag future = BlessFlag.of("SOME-FUTURE-FLAG");
    assertArrayEquals(SafeEncoder.encode("SOME-FUTURE-FLAG"), future.getRaw());
    assertEquals("SOME-FUTURE-FLAG", future.toString());
  }

  @Test
  public void ofRejectsEmptyToken() {
    assertThrows(IllegalArgumentException.class, () -> BlessFlag.of(null));
    assertThrows(IllegalArgumentException.class, () -> BlessFlag.of(""));
  }

  @Test
  public void equalityIsByToken() {
    assertEquals(BlessFlag.NO_EVICT, BlessFlag.of("NO-EVICT"));
    assertEquals(BlessFlag.NO_EVICT.hashCode(), BlessFlag.of("NO-EVICT").hashCode());
    assertNotEquals(BlessFlag.NO_EVICT, BlessFlag.of("no-evict"));
  }
}
