package redis.clients.jedis.executors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    ClusterAggregationException exception = assertThrows(ClusterAggregationException.class,
      () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
      "Should throw ClusterAggregationException when byte arrays differ");

    assertTrue(
      exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
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

    ClusterAggregationException exception = assertThrows(ClusterAggregationException.class,
      () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
      "Should throw ClusterAggregationException when Long values differ");

    assertTrue(
      exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
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

    ClusterAggregationException exception = assertThrows(ClusterAggregationException.class,
      () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
      "Should throw ClusterAggregationException when Integer values differ");

    assertTrue(
      exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
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

    ClusterAggregationException exception = assertThrows(ClusterAggregationException.class,
      () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
      "Should throw ClusterAggregationException when Double values differ");

    assertTrue(
      exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
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

    ClusterAggregationException exception = assertThrows(ClusterAggregationException.class,
      () -> ClusterReplyAggregator.aggregateAllSucceeded(first, second),
      "Should throw ClusterAggregationException when String values differ");

    assertTrue(
      exception.getMessage().contains("ALL_SUCCEEDED policy requires all replies to be equal"),
      "Exception message should contain policy information");
    assertTrue(exception.getMessage().contains("OK"),
      "Exception message should contain the first value");
    assertTrue(exception.getMessage().contains("ERROR"),
      "Exception message should contain the second value");
  }

  // ==================== aggregateDefault - List<String> Tests ====================

  @Test
  public void testAggregateDefault_twoStringLists_concatenatesThem() {
    List<String> first = new ArrayList<>(Arrays.asList("key1", "key2"));
    List<String> second = new ArrayList<>(Arrays.asList("key3", "key4"));

    @SuppressWarnings("unchecked")
    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain all elements from both lists");
    assertEquals(Arrays.asList("key1", "key2", "key3", "key4"), result,
      "Should concatenate lists in order");
  }

  @Test
  public void testAggregateDefault_emptyAndNonEmptyStringLists_returnsNonEmptyElements() {
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>(Arrays.asList("key1", "key2"));

    @SuppressWarnings("unchecked")
    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain elements from non-empty list");
    assertEquals(Arrays.asList("key1", "key2"), result,
      "Should contain all elements from second list");
  }

  @Test
  public void testAggregateDefault_nonEmptyAndEmptyStringLists_returnsFirstElements() {
    List<String> first = new ArrayList<>(Arrays.asList("key1", "key2"));
    List<String> second = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain elements from first list");
    assertEquals(Arrays.asList("key1", "key2"), result,
      "Should contain all elements from first list");
  }

  @Test
  public void testAggregateDefault_twoEmptyStringLists_returnsEmptyList() {
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>();

    @SuppressWarnings("unchecked")
    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertTrue(result.isEmpty(), "Should return empty list when both lists are empty");
  }

  // ==================== aggregateDefault - List<byte[]> Tests ====================

  @Test
  public void testAggregateDefault_twoByteArrayLists_concatenatesThem() {
    List<byte[]> first = new ArrayList<>(Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 }));
    List<byte[]> second = new ArrayList<>(Arrays.asList(new byte[] { 5, 6 }, new byte[] { 7, 8 }));

    @SuppressWarnings("unchecked")
    List<byte[]> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain all byte arrays from both lists");
    assertArrayEquals(new byte[] { 1, 2 }, result.get(0));
    assertArrayEquals(new byte[] { 3, 4 }, result.get(1));
    assertArrayEquals(new byte[] { 5, 6 }, result.get(2));
    assertArrayEquals(new byte[] { 7, 8 }, result.get(3));
  }

  // ==================== aggregateDefault - Different List Implementations ====================

  @Test
  public void testAggregateDefault_linkedListAndArrayList_concatenatesThem() {
    List<String> first = new LinkedList<>(Arrays.asList("a", "b"));
    List<String> second = new ArrayList<>(Arrays.asList("c", "d"));

    @SuppressWarnings("unchecked")
    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should concatenate different list implementations");
    assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    assertTrue(result instanceof ArrayList, "Result should be an ArrayList");
  }

  // ==================== aggregateDefault - Non-List Types Fallback ====================

  @Test
  public void testAggregateDefault_nonListTypes_returnsExisting() {
    String first = "existing";
    String second = "new";

    String result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals("existing", result, "Should return existing value for non-list types");
  }

  @Test
  public void testAggregateDefault_longValues_returnsExisting() {
    Long first = 100L;
    Long second = 200L;

    Long result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(100L, result, "Should return existing Long value");
  }

  // ==================== aggregateDefault - Preserves Original Lists ====================

  @Test
  public void testAggregateDefault_doesNotModifyOriginalLists() {
    List<String> first = new ArrayList<>(Arrays.asList("a", "b"));
    List<String> second = new ArrayList<>(Arrays.asList("c", "d"));

    ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, first.size(), "First list should not be modified");
    assertEquals(2, second.size(), "Second list should not be modified");
    assertEquals(Arrays.asList("a", "b"), first);
    assertEquals(Arrays.asList("c", "d"), second);
  }

  // ==================== aggregateDefault - Map Tests ====================

  @Test
  public void testAggregateDefault_twoMapsWithDifferentKeys_mergesThem() {
    Map<String, Integer> first = new HashMap<>();
    first.put("key1", 1);
    first.put("key2", 2);

    Map<String, Integer> second = new HashMap<>();
    second.put("key3", 3);
    second.put("key4", 4);

    @SuppressWarnings("unchecked")
    Map<String, Integer> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain all entries from both maps");
    assertEquals(1, result.get("key1"));
    assertEquals(2, result.get("key2"));
    assertEquals(3, result.get("key3"));
    assertEquals(4, result.get("key4"));
  }

  @Test
  public void testAggregateDefault_twoMapsWithOverlappingKeys_secondMapTakesPrecedence() {
    Map<String, String> first = new HashMap<>();
    first.put("shared", "first_value");
    first.put("unique1", "value1");

    Map<String, String> second = new HashMap<>();
    second.put("shared", "second_value");
    second.put("unique2", "value2");

    @SuppressWarnings("unchecked")
    Map<String, String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(3, result.size(), "Should contain merged entries");
    assertEquals("second_value", result.get("shared"), "Second map's value should overwrite first");
    assertEquals("value1", result.get("unique1"));
    assertEquals("value2", result.get("unique2"));
  }

  @Test
  public void testAggregateDefault_emptyAndNonEmptyMaps_returnsNonEmptyEntries() {
    Map<String, Integer> first = new HashMap<>();
    Map<String, Integer> second = new HashMap<>();
    second.put("key1", 1);
    second.put("key2", 2);

    @SuppressWarnings("unchecked")
    Map<String, Integer> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain entries from non-empty map");
    assertEquals(1, result.get("key1"));
    assertEquals(2, result.get("key2"));
  }

  @Test
  public void testAggregateDefault_nonEmptyAndEmptyMaps_returnsFirstEntries() {
    Map<String, Integer> first = new HashMap<>();
    first.put("key1", 1);
    first.put("key2", 2);
    Map<String, Integer> second = new HashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Integer> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain entries from first map");
    assertEquals(1, result.get("key1"));
    assertEquals(2, result.get("key2"));
  }

  @Test
  public void testAggregateDefault_twoEmptyMaps_returnsEmptyMap() {
    Map<String, Integer> first = new HashMap<>();
    Map<String, Integer> second = new HashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Integer> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertTrue(result.isEmpty(), "Should return empty map when both maps are empty");
  }

  @Test
  public void testAggregateDefault_differentMapImplementations_mergesThem() {
    Map<String, String> first = new LinkedHashMap<>();
    first.put("a", "1");
    first.put("b", "2");

    Map<String, String> second = new HashMap<>();
    second.put("c", "3");
    second.put("d", "4");

    @SuppressWarnings("unchecked")
    Map<String, String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should merge different map implementations");
    assertEquals("1", result.get("a"));
    assertEquals("2", result.get("b"));
    assertEquals("3", result.get("c"));
    assertEquals("4", result.get("d"));
    assertTrue(result instanceof HashMap, "Result should be a HashMap");
  }

  @Test
  public void testAggregateDefault_doesNotModifyOriginalMaps() {
    Map<String, String> first = new HashMap<>();
    first.put("a", "1");
    first.put("b", "2");

    Map<String, String> second = new HashMap<>();
    second.put("c", "3");
    second.put("d", "4");

    ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, first.size(), "First map should not be modified");
    assertEquals(2, second.size(), "Second map should not be modified");
    assertEquals("1", first.get("a"));
    assertEquals("2", first.get("b"));
    assertEquals("3", second.get("c"));
    assertEquals("4", second.get("d"));
  }

  // ==================== aggregateDefault - Set Tests ====================

  @Test
  public void testAggregateDefault_twoSetsWithDifferentElements_mergesThem() {
    Set<String> first = new HashSet<>(Arrays.asList("a", "b"));
    Set<String> second = new HashSet<>(Arrays.asList("c", "d"));

    @SuppressWarnings("unchecked")
    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain all elements from both sets");
    assertTrue(result.contains("a"));
    assertTrue(result.contains("b"));
    assertTrue(result.contains("c"));
    assertTrue(result.contains("d"));
    assertTrue(result instanceof HashSet, "Result should be a HashSet");
  }

  @Test
  public void testAggregateDefault_twoSetsWithOverlappingElements_mergesWithoutDuplicates() {
    Set<String> first = new HashSet<>(Arrays.asList("a", "b", "c"));
    Set<String> second = new HashSet<>(Arrays.asList("b", "c", "d"));

    @SuppressWarnings("unchecked")
    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain unique elements from both sets");
    assertTrue(result.contains("a"));
    assertTrue(result.contains("b"));
    assertTrue(result.contains("c"));
    assertTrue(result.contains("d"));
  }

  @Test
  public void testAggregateDefault_emptyAndNonEmptySets_returnsNonEmptyElements() {
    Set<String> first = new HashSet<>();
    Set<String> second = new HashSet<>(Arrays.asList("a", "b"));

    @SuppressWarnings("unchecked")
    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain elements from non-empty set");
    assertTrue(result.contains("a"));
    assertTrue(result.contains("b"));
  }

  @Test
  public void testAggregateDefault_nonEmptyAndEmptySets_returnsFirstElements() {
    Set<String> first = new HashSet<>(Arrays.asList("a", "b"));
    Set<String> second = new HashSet<>();

    @SuppressWarnings("unchecked")
    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain elements from first set");
    assertTrue(result.contains("a"));
    assertTrue(result.contains("b"));
  }

  @Test
  public void testAggregateDefault_twoEmptySets_returnsEmptySet() {
    Set<String> first = new HashSet<>();
    Set<String> second = new HashSet<>();

    @SuppressWarnings("unchecked")
    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertTrue(result.isEmpty(), "Should return empty set when both are empty");
    assertTrue(result instanceof HashSet, "Result should be a HashSet");
  }

  @Test
  public void testAggregateDefault_differentSetImplementations_mergesThem() {
    Set<String> first = new LinkedHashSet<>(Arrays.asList("a", "b"));
    Set<String> second = new HashSet<>(Arrays.asList("c", "d"));

    @SuppressWarnings("unchecked")
    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should merge different set implementations");
    assertTrue(result.contains("a"));
    assertTrue(result.contains("b"));
    assertTrue(result.contains("c"));
    assertTrue(result.contains("d"));
    assertTrue(result instanceof HashSet, "Result should be a HashSet");
  }

  @Test
  public void testAggregateDefault_doesNotModifyOriginalSets() {
    Set<String> first = new HashSet<>(Arrays.asList("a", "b"));
    Set<String> second = new HashSet<>(Arrays.asList("c", "d"));

    ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, first.size(), "First set should not be modified");
    assertEquals(2, second.size(), "Second set should not be modified");
    assertTrue(first.contains("a"));
    assertTrue(first.contains("b"));
    assertTrue(second.contains("c"));
    assertTrue(second.contains("d"));
  }

  @Test
  public void testAggregateDefault_byteArraySets_mergesThem() {
    // Testing with byte[] sets similar to what BINARY_SET returns
    Set<byte[]> first = new HashSet<>();
    first.add(new byte[] { 1, 2 });
    first.add(new byte[] { 3, 4 });

    Set<byte[]> second = new HashSet<>();
    second.add(new byte[] { 5, 6 });

    @SuppressWarnings("unchecked")
    Set<byte[]> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(3, result.size(), "Should contain all byte arrays from both sets");
    assertTrue(result instanceof HashSet, "Result should be a HashSet");
  }
}
