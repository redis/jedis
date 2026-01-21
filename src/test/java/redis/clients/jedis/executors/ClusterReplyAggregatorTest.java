package redis.clients.jedis.executors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.ClusterAggregationException;

public class ClusterReplyAggregatorTest {

  // ==================== aggregateAllSucceeded - Byte Array Tests ====================

  @Test
  public void testAggregateAllSucceeded_identicalByteArrays_returnsFirstArray() {
    byte[] first = new byte[] { 1, 2, 3, 4, 5 };
    byte[] second = new byte[] { 1, 2, 3, 4, 5 };

    // Byte arrays with same content should be treated as equal using Arrays.equals()
    byte[] result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertSame(first, result, "Should return the first byte array when contents are equal");
  }

  @Test
  public void testAggregateAllSucceeded_sameByteArrayReference_returnsArray() {
    byte[] array = new byte[] { 1, 2, 3, 4, 5 };

    byte[] result = ClusterReplyAggregator.aggregateAllSucceeded(array, array);

    assertSame(array, result, "Should return the same array reference");
  }

  @Test
  public void testAggregateAllSucceeded_differentByteArrays_throwsException() {
    byte[] first = new byte[] { 1, 2, 3 };
    byte[] second = new byte[] { 4, 5, 6 };

    ClusterAggregationException exception = assertThrows(
        ClusterAggregationException.class,
        () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
        "Should throw ClusterAggregationException when byte arrays differ"
    );

    assertTrue(exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
        "Exception message should contain policy information");
    assertTrue(exception.getMessage().contains("vs"),
        "Exception message should contain 'vs' to show comparison");
  }

  // ==================== aggregateAllSucceeded - Long Tests ====================

  @Test
  public void testAggregateAllSucceeded_identicalLongValues_returnsFirstValue() {
    Long first = 42L;
    Long second = 42L;

    Long result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(42L, result, "Should return the first Long value");
  }

  @Test
  public void testAggregateAllSucceeded_differentLongValues_throwsException() {
    Long first = 42L;
    Long second = 100L;

    ClusterAggregationException exception = assertThrows(
        ClusterAggregationException.class,
        () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
        "Should throw ClusterAggregationException when Long values differ"
    );

    assertTrue(exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
        "Exception message should contain policy information");
    assertTrue(exception.getMessage().contains("42"),
        "Exception message should contain the first value");
    assertTrue(exception.getMessage().contains("100"),
        "Exception message should contain the second value");
  }

  // ==================== aggregateAllSucceeded - Integer Tests ====================

  @Test
  public void testAggregateAllSucceeded_identicalIntegerValues_returnsFirstValue() {
    Integer first = 123;
    Integer second = 123;

    Integer result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(123, result, "Should return the first Integer value");
  }

  @Test
  public void testAggregateAllSucceeded_differentIntegerValues_throwsException() {
    Integer first = 123;
    Integer second = 456;

    ClusterAggregationException exception = assertThrows(
        ClusterAggregationException.class,
        () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
        "Should throw ClusterAggregationException when Integer values differ"
    );

    assertTrue(exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
        "Exception message should contain policy information");
    assertTrue(exception.getMessage().contains("123"),
        "Exception message should contain the first value");
    assertTrue(exception.getMessage().contains("456"),
        "Exception message should contain the second value");
  }

  // ==================== aggregateAllSucceeded - Double Tests ====================

  @Test
  public void testAggregateAllSucceeded_identicalDoubleValues_returnsFirstValue() {
    Double first = 3.14159;
    Double second = 3.14159;

    Double result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(3.14159, result, "Should return the first Double value");
  }

  @Test
  public void testAggregateAllSucceeded_differentDoubleValues_throwsException() {
    Double first = 3.14159;
    Double second = 2.71828;

    ClusterAggregationException exception = assertThrows(
        ClusterAggregationException.class,
        () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
        "Should throw ClusterAggregationException when Double values differ"
    );

    assertTrue(exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
        "Exception message should contain policy information");
    assertTrue(exception.getMessage().contains("3.14159"),
        "Exception message should contain the first value");
    assertTrue(exception.getMessage().contains("2.71828"),
        "Exception message should contain the second value");
  }

  // ==================== aggregateAllSucceeded - String Tests ====================

  @Test
  public void testAggregateAllSucceeded_identicalStringValues_returnsFirstValue() {
    String first = "OK";
    String second = "OK";

    String result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals("OK", result, "Should return the first String value");
  }

  @Test
  public void testAggregateAllSucceeded_differentStringValues_throwsException() {
    String first = "OK";
    String second = "ERROR";

    ClusterAggregationException exception = assertThrows(
        ClusterAggregationException.class,
        () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
        "Should throw ClusterAggregationException when String values differ"
    );

    assertTrue(exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
        "Exception message should contain policy information");
    assertTrue(exception.getMessage().contains("OK"),
        "Exception message should contain the first value");
    assertTrue(exception.getMessage().contains("ERROR"),
        "Exception message should contain the second value");
  }
}

