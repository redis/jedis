package redis.clients.jedis.resps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StreamEntryDeletionResultTest {

  @Test
  public void testFromCode() {
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, StreamEntryDeletionResult.fromCode(-1));
    assertEquals(StreamEntryDeletionResult.DELETED, StreamEntryDeletionResult.fromCode(1));
    assertEquals(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED,
      StreamEntryDeletionResult.fromCode(2));
  }

  @Test
  public void testFromCodeInvalid() {
    assertThrows(IllegalArgumentException.class, () -> StreamEntryDeletionResult.fromCode(0));
    assertThrows(IllegalArgumentException.class, () -> StreamEntryDeletionResult.fromCode(3));
    assertThrows(IllegalArgumentException.class, () -> StreamEntryDeletionResult.fromCode(-2));
  }

  @Test
  public void testFromLong() {
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, StreamEntryDeletionResult.fromLong(-1L));
    assertEquals(StreamEntryDeletionResult.DELETED, StreamEntryDeletionResult.fromLong(1L));
    assertEquals(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED,
      StreamEntryDeletionResult.fromLong(2L));
  }

  @Test
  public void testFromLongNull() {
    assertThrows(IllegalArgumentException.class, () -> StreamEntryDeletionResult.fromLong(null));
  }

  @Test
  public void testGetCode() {
    assertEquals(-1, StreamEntryDeletionResult.NOT_FOUND.getCode());
    assertEquals(1, StreamEntryDeletionResult.DELETED.getCode());
    assertEquals(2,
      StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED.getCode());
  }

  @Test
  public void testToString() {
    assertEquals("NOT_FOUND(-1)", StreamEntryDeletionResult.NOT_FOUND.toString());
    assertEquals("DELETED(1)", StreamEntryDeletionResult.DELETED.toString());
    assertEquals("NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED(2)",
      StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED.toString());
  }
}
