package redis.clients.jedis.resps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StreamEntryDeletionResultTest {

  @Test
  public void testFromCode() {
    assertEquals(StreamTrimResult.NOT_FOUND, StreamTrimResult.fromCode(-1));
    assertEquals(StreamTrimResult.DELETED, StreamTrimResult.fromCode(1));
    assertEquals(StreamTrimResult.ACKNOWLEDGED_NOT_DELETED, StreamTrimResult.fromCode(2));
  }

  @Test
  public void testFromCodeInvalid() {
    assertThrows(IllegalArgumentException.class, () -> StreamTrimResult.fromCode(0));
    assertThrows(IllegalArgumentException.class, () -> StreamTrimResult.fromCode(3));
    assertThrows(IllegalArgumentException.class, () -> StreamTrimResult.fromCode(-2));
  }

  @Test
  public void testFromLong() {
    assertEquals(StreamTrimResult.NOT_FOUND, StreamTrimResult.fromLong(-1L));
    assertEquals(StreamTrimResult.DELETED, StreamTrimResult.fromLong(1L));
    assertEquals(StreamTrimResult.ACKNOWLEDGED_NOT_DELETED, StreamTrimResult.fromLong(2L));
  }

  @Test
  public void testFromLongNull() {
    assertThrows(IllegalArgumentException.class, () -> StreamTrimResult.fromLong(null));
  }

  @Test
  public void testGetCode() {
    assertEquals(-1, StreamTrimResult.NOT_FOUND.getCode());
    assertEquals(1, StreamTrimResult.DELETED.getCode());
    assertEquals(2, StreamTrimResult.ACKNOWLEDGED_NOT_DELETED.getCode());
  }

  @Test
  public void testToString() {
    assertEquals("NOT_FOUND(-1)", StreamTrimResult.NOT_FOUND.toString());
    assertEquals("DELETED(1)", StreamTrimResult.DELETED.toString());
    assertEquals("ACKNOWLEDGED_NOT_DELETED(2)", StreamTrimResult.ACKNOWLEDGED_NOT_DELETED.toString());
  }
}
