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

import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.JedisByteMap;
import redis.clients.jedis.util.KeyValue;

public class ClusterReplyAggregatorTest {

  // ==================== aggregateAllSucceeded Tests ====================
  // Per Redis ALL_SUCCEEDED spec: returns successfully only if there are no error replies.
  // Error handling is done separately by the caller (MultiNodeResultAggregator.addError()),
  // so aggregateAllSucceeded simply returns the first reply when aggregating successful responses.

  @Test
  public void testAggregateAllSucceeded_byteArrays_returnsFirstArray() {
    byte[] first = new byte[] { 1, 2, 3, 4, 5 };
    byte[] second = new byte[] { 1, 2, 3, 4, 5 };

    byte[] result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertSame(first, result, "Should return the first byte array");
  }

  @Test
  public void testAggregateAllSucceeded_sameByteArrayReference_returnsArray() {
    byte[] array = new byte[] { 1, 2, 3, 4, 5 };

    byte[] result = ClusterReplyAggregator.aggregateAllSucceeded(array, array);

    assertSame(array, result, "Should return the same array reference");
  }

  @Test
  public void testAggregateAllSucceeded_differentByteArrays_returnsFirst() {
    byte[] first = new byte[] { 1, 2, 3 };
    byte[] second = new byte[] { 4, 5, 6 };

    byte[] result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertSame(first, result,
      "Should return the first byte array regardless of content difference");
  }

  @Test
  public void testAggregateAllSucceeded_longValues_returnsFirstValue() {
    Long first = 42L;
    Long second = 42L;

    Long result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(42L, result, "Should return the first Long value");
  }

  @Test
  public void testAggregateAllSucceeded_differentLongValues_returnsFirst() {
    Long first = 42L;
    Long second = 100L;

    Long result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(42L, result, "Should return the first Long value regardless of difference");
  }

  @Test
  public void testAggregateAllSucceeded_integerValues_returnsFirstValue() {
    Integer first = 123;
    Integer second = 123;

    Integer result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(123, result, "Should return the first Integer value");
  }

  @Test
  public void testAggregateAllSucceeded_differentIntegerValues_returnsFirst() {
    Integer first = 123;
    Integer second = 456;

    Integer result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(123, result, "Should return the first Integer value regardless of difference");
  }

  @Test
  public void testAggregateAllSucceeded_doubleValues_returnsFirstValue() {
    Double first = 3.14159;
    Double second = 3.14159;

    Double result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(3.14159, result, "Should return the first Double value");
  }

  @Test
  public void testAggregateAllSucceeded_differentDoubleValues_returnsFirst() {
    Double first = 3.14159;
    Double second = 2.71828;

    Double result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals(3.14159, result, "Should return the first Double value regardless of difference");
  }

  @Test
  public void testAggregateAllSucceeded_stringValues_returnsFirstValue() {
    String first = "OK";
    String second = "OK";

    String result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals("OK", result, "Should return the first String value");
  }

  @Test
  public void testAggregateAllSucceeded_differentStringValues_returnsFirst() {
    String first = "OK";
    String second = "DIFFERENT";

    String result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertEquals("OK", result, "Should return the first String value regardless of difference");
  }

  @Test
  public void testAggregateAllSucceeded_booleanValues_returnsFirst() {
    // This tests the use case mentioned in the bug: MSETEX with NX/XX conditions
    // where different shards can legitimately return different non-error values
    Boolean first = true;
    Boolean second = false;

    Boolean result = ClusterReplyAggregator.aggregateAllSucceeded(first, second);

    assertTrue(result, "Should return the first Boolean value regardless of difference");
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
  public void testAggregateDefault_linkedListAndArrayList_mutatesLinkedListInPlace() {
    List<String> first = new LinkedList<>(Arrays.asList("a", "b"));
    List<String> second = new ArrayList<>(Arrays.asList("c", "d"));

    @SuppressWarnings("unchecked")
    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should concatenate different list implementations");
    assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    assertSame(first, result,
      "Result should be the same instance as first list (mutated in place)");
    assertTrue(result instanceof LinkedList, "Result should remain a LinkedList");
  }

  // ==================== aggregateDefault - Unsupported Types Throw Exception ====================

  @Test
  public void testAggregateDefault_nonListTypes_throwsUnsupportedAggregationException() {
    String first = "existing";
    String second = "new";

    UnsupportedAggregationException exception = assertThrows(UnsupportedAggregationException.class,
      () -> ClusterReplyAggregator.aggregateDefault(first, second));

    assertTrue(exception.getMessage().contains("DEFAULT policy requires"),
      "Exception message should describe the policy requirement");
    assertTrue(exception.getMessage().contains("String"),
      "Exception message should mention the unsupported type");
  }

  @Test
  public void testAggregateDefault_longValues_throwsUnsupportedAggregationException() {
    Long first = 100L;
    Long second = 200L;

    UnsupportedAggregationException exception = assertThrows(UnsupportedAggregationException.class,
      () -> ClusterReplyAggregator.aggregateDefault(first, second));

    assertTrue(exception.getMessage().contains("DEFAULT policy requires"),
      "Exception message should describe the policy requirement");
    assertTrue(exception.getMessage().contains("Long"),
      "Exception message should mention the unsupported type");
  }

  // ==================== aggregateDefault - Mutates Existing ArrayList In Place
  // ====================

  @Test
  public void testAggregateDefault_mutatesExistingArrayListInPlace() {
    List<String> first = new ArrayList<>(Arrays.asList("a", "b"));
    List<String> second = new ArrayList<>(Arrays.asList("c", "d"));

    List<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    // Result should be the same instance as first (mutated in place)
    assertSame(first, result, "Result should be the same instance as first list");
    assertEquals(4, first.size(), "First list should be mutated with all elements");
    assertEquals(Arrays.asList("a", "b", "c", "d"), first);
    // Second list should NOT be modified
    assertEquals(2, second.size(), "Second list should not be modified");
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
  public void testAggregateDefault_mutatesExistingHashMapInPlace() {
    Map<String, String> first = new HashMap<>();
    first.put("a", "1");
    first.put("b", "2");

    Map<String, String> second = new HashMap<>();
    second.put("c", "3");
    second.put("d", "4");

    Map<String, String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    // Result should be the same instance as first (mutated in place)
    assertSame(first, result, "Result should be the same instance as first map");
    assertEquals(4, first.size(), "First map should be mutated with all entries");
    assertEquals("1", first.get("a"));
    assertEquals("2", first.get("b"));
    assertEquals("3", first.get("c"));
    assertEquals("4", first.get("d"));
    // Second map should NOT be modified
    assertEquals(2, second.size(), "Second map should not be modified");
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
  public void testAggregateDefault_mutatesExistingHashSetInPlace() {
    Set<String> first = new HashSet<>(Arrays.asList("a", "b"));
    Set<String> second = new HashSet<>(Arrays.asList("c", "d"));

    Set<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    // Result should be the same instance as first (mutated in place)
    assertSame(first, result, "Result should be the same instance as first set");
    assertEquals(4, first.size(), "First set should be mutated with all elements");
    assertTrue(first.contains("a"));
    assertTrue(first.contains("b"));
    assertTrue(first.contains("c"));
    assertTrue(first.contains("d"));
    // Second set should NOT be modified
    assertEquals(2, second.size(), "Second set should not be modified");
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

  // ==================== aggregateDefault - JedisByteHashMap Tests ====================

  @Test
  public void testAggregateDefault_twoJedisByteHashMapsWithDifferentKeys_mergesThem() {
    JedisByteHashMap first = new JedisByteHashMap();
    first.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
    first.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });

    JedisByteHashMap second = new JedisByteHashMap();
    second.put(new byte[] { 'k', '3' }, new byte[] { 'v', '3' });
    second.put(new byte[] { 'k', '4' }, new byte[] { 'v', '4' });

    @SuppressWarnings("unchecked")
    JedisByteHashMap result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain all entries from both maps");
    assertArrayEquals(new byte[] { 'v', '1' }, result.get(new byte[] { 'k', '1' }));
    assertArrayEquals(new byte[] { 'v', '2' }, result.get(new byte[] { 'k', '2' }));
    assertArrayEquals(new byte[] { 'v', '3' }, result.get(new byte[] { 'k', '3' }));
    assertArrayEquals(new byte[] { 'v', '4' }, result.get(new byte[] { 'k', '4' }));
    assertTrue(result instanceof JedisByteHashMap, "Result should be a JedisByteHashMap");
  }

  @Test
  public void testAggregateDefault_twoJedisByteHashMapsWithOverlappingKeys_secondMapTakesPrecedence() {
    JedisByteHashMap first = new JedisByteHashMap();
    first.put(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' }, new byte[] { 'f', 'i', 'r', 's', 't' });
    first.put(new byte[] { 'u', 'n', 'i', 'q', '1' }, new byte[] { 'v', 'a', 'l', '1' });

    JedisByteHashMap second = new JedisByteHashMap();
    second.put(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' },
      new byte[] { 's', 'e', 'c', 'o', 'n', 'd' });
    second.put(new byte[] { 'u', 'n', 'i', 'q', '2' }, new byte[] { 'v', 'a', 'l', '2' });

    @SuppressWarnings("unchecked")
    JedisByteHashMap result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(3, result.size(), "Should contain merged entries");
    assertArrayEquals(new byte[] { 's', 'e', 'c', 'o', 'n', 'd' },
      result.get(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' }),
      "Second map's value should overwrite first");
    assertArrayEquals(new byte[] { 'v', 'a', 'l', '1' },
      result.get(new byte[] { 'u', 'n', 'i', 'q', '1' }));
    assertArrayEquals(new byte[] { 'v', 'a', 'l', '2' },
      result.get(new byte[] { 'u', 'n', 'i', 'q', '2' }));
  }

  @Test
  public void testAggregateDefault_emptyAndNonEmptyJedisByteHashMaps_returnsNonEmptyEntries() {
    JedisByteHashMap first = new JedisByteHashMap();
    JedisByteHashMap second = new JedisByteHashMap();
    second.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
    second.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });

    @SuppressWarnings("unchecked")
    JedisByteHashMap result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain entries from non-empty map");
    assertArrayEquals(new byte[] { 'v', '1' }, result.get(new byte[] { 'k', '1' }));
    assertArrayEquals(new byte[] { 'v', '2' }, result.get(new byte[] { 'k', '2' }));
  }

  @Test
  public void testAggregateDefault_nonEmptyAndEmptyJedisByteHashMaps_returnsFirstEntries() {
    JedisByteHashMap first = new JedisByteHashMap();
    first.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
    first.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });
    JedisByteHashMap second = new JedisByteHashMap();

    @SuppressWarnings("unchecked")
    JedisByteHashMap result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain entries from first map");
    assertArrayEquals(new byte[] { 'v', '1' }, result.get(new byte[] { 'k', '1' }));
    assertArrayEquals(new byte[] { 'v', '2' }, result.get(new byte[] { 'k', '2' }));
  }

  @Test
  public void testAggregateDefault_mutatesExistingJedisByteHashMapInPlace() {
    JedisByteHashMap first = new JedisByteHashMap();
    first.put(new byte[] { 'a' }, new byte[] { '1' });
    first.put(new byte[] { 'b' }, new byte[] { '2' });

    JedisByteHashMap second = new JedisByteHashMap();
    second.put(new byte[] { 'c' }, new byte[] { '3' });
    second.put(new byte[] { 'd' }, new byte[] { '4' });

    JedisByteHashMap result = ClusterReplyAggregator.aggregateDefault(first, second);

    // Result should be the same instance as first (mutated in place)
    assertSame(first, result, "Result should be the same instance as first map");
    assertEquals(4, first.size(), "First map should be mutated with all entries");
    assertArrayEquals(new byte[] { '1' }, first.get(new byte[] { 'a' }));
    assertArrayEquals(new byte[] { '2' }, first.get(new byte[] { 'b' }));
    assertArrayEquals(new byte[] { '3' }, first.get(new byte[] { 'c' }));
    assertArrayEquals(new byte[] { '4' }, first.get(new byte[] { 'd' }));
    // Second map should NOT be modified
    assertEquals(2, second.size(), "Second map should not be modified");
    assertArrayEquals(new byte[] { '3' }, second.get(new byte[] { 'c' }));
    assertArrayEquals(new byte[] { '4' }, second.get(new byte[] { 'd' }));
  }

  // ==================== aggregateDefault - JedisByteMap Tests ====================

  @Test
  public void testAggregateDefault_twoJedisByteMapsWithDifferentKeys_mergesThem() {
    JedisByteMap<String> first = new JedisByteMap<>();
    first.put(new byte[] { 'k', '1' }, "value1");
    first.put(new byte[] { 'k', '2' }, "value2");

    JedisByteMap<String> second = new JedisByteMap<>();
    second.put(new byte[] { 'k', '3' }, "value3");
    second.put(new byte[] { 'k', '4' }, "value4");

    @SuppressWarnings("unchecked")
    JedisByteMap<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(4, result.size(), "Should contain all entries from both maps");
    assertEquals("value1", result.get(new byte[] { 'k', '1' }));
    assertEquals("value2", result.get(new byte[] { 'k', '2' }));
    assertEquals("value3", result.get(new byte[] { 'k', '3' }));
    assertEquals("value4", result.get(new byte[] { 'k', '4' }));
    assertTrue(result instanceof JedisByteMap, "Result should be a JedisByteMap");
  }

  @Test
  public void testAggregateDefault_twoJedisByteMapsWithOverlappingKeys_secondMapTakesPrecedence() {
    JedisByteMap<String> first = new JedisByteMap<>();
    first.put(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' }, "first_value");
    first.put(new byte[] { 'u', 'n', 'i', 'q', '1' }, "unique1");

    JedisByteMap<String> second = new JedisByteMap<>();
    second.put(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' }, "second_value");
    second.put(new byte[] { 'u', 'n', 'i', 'q', '2' }, "unique2");

    @SuppressWarnings("unchecked")
    JedisByteMap<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(3, result.size(), "Should contain merged entries");
    assertEquals("second_value", result.get(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' }),
      "Second map's value should overwrite first");
    assertEquals("unique1", result.get(new byte[] { 'u', 'n', 'i', 'q', '1' }));
    assertEquals("unique2", result.get(new byte[] { 'u', 'n', 'i', 'q', '2' }));
  }

  @Test
  public void testAggregateDefault_emptyAndNonEmptyJedisByteMaps_returnsNonEmptyEntries() {
    JedisByteMap<Integer> first = new JedisByteMap<>();
    JedisByteMap<Integer> second = new JedisByteMap<>();
    second.put(new byte[] { 'k', '1' }, 1);
    second.put(new byte[] { 'k', '2' }, 2);

    @SuppressWarnings("unchecked")
    JedisByteMap<Integer> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain entries from non-empty map");
    assertEquals(1, result.get(new byte[] { 'k', '1' }));
    assertEquals(2, result.get(new byte[] { 'k', '2' }));
  }

  @Test
  public void testAggregateDefault_nonEmptyAndEmptyJedisByteMaps_returnsFirstEntries() {
    JedisByteMap<Integer> first = new JedisByteMap<>();
    first.put(new byte[] { 'k', '1' }, 1);
    first.put(new byte[] { 'k', '2' }, 2);
    JedisByteMap<Integer> second = new JedisByteMap<>();

    @SuppressWarnings("unchecked")
    JedisByteMap<Integer> result = ClusterReplyAggregator.aggregateDefault(first, second);

    assertEquals(2, result.size(), "Should contain entries from first map");
    assertEquals(1, result.get(new byte[] { 'k', '1' }));
    assertEquals(2, result.get(new byte[] { 'k', '2' }));
  }

  @Test
  public void testAggregateDefault_mutatesExistingJedisByteMapInPlace() {
    JedisByteMap<String> first = new JedisByteMap<>();
    first.put(new byte[] { 'a' }, "1");
    first.put(new byte[] { 'b' }, "2");

    JedisByteMap<String> second = new JedisByteMap<>();
    second.put(new byte[] { 'c' }, "3");
    second.put(new byte[] { 'd' }, "4");

    JedisByteMap<String> result = ClusterReplyAggregator.aggregateDefault(first, second);

    // Result should be the same instance as first (mutated in place)
    assertSame(first, result, "Result should be the same instance as first map");
    assertEquals(4, first.size(), "First map should be mutated with all entries");
    assertEquals("1", first.get(new byte[] { 'a' }));
    assertEquals("2", first.get(new byte[] { 'b' }));
    assertEquals("3", first.get(new byte[] { 'c' }));
    assertEquals("4", first.get(new byte[] { 'd' }));
    // Second map should NOT be modified
    assertEquals(2, second.size(), "Second map should not be modified");
    assertEquals("3", second.get(new byte[] { 'c' }));
    assertEquals("4", second.get(new byte[] { 'd' }));
  }

  // ==================== aggregateMin - KeyValue Tests ====================

  @Test
  public void testAggregateMin_keyValueLongLong_returnsMinOfEachComponent() {
    KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
    KeyValue<Long, Long> second = KeyValue.of(5L, 25L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMin(first, second);

    assertEquals(5L, result.getKey(), "Should return minimum key");
    assertEquals(20L, result.getValue(), "Should return minimum value");
  }

  @Test
  public void testAggregateMin_keyValueLongLong_firstSmaller() {
    KeyValue<Long, Long> first = KeyValue.of(1L, 2L);
    KeyValue<Long, Long> second = KeyValue.of(10L, 20L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMin(first, second);

    assertEquals(1L, result.getKey(), "Should return minimum key from first");
    assertEquals(2L, result.getValue(), "Should return minimum value from first");
  }

  @Test
  public void testAggregateMin_keyValueLongLong_secondSmaller() {
    KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
    KeyValue<Long, Long> second = KeyValue.of(1L, 2L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMin(first, second);

    assertEquals(1L, result.getKey(), "Should return minimum key from second");
    assertEquals(2L, result.getValue(), "Should return minimum value from second");
  }

  @Test
  public void testAggregateMin_keyValueLongLong_equalValues() {
    KeyValue<Long, Long> first = KeyValue.of(5L, 5L);
    KeyValue<Long, Long> second = KeyValue.of(5L, 5L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMin(first, second);

    assertEquals(5L, result.getKey(), "Should return equal key");
    assertEquals(5L, result.getValue(), "Should return equal value");
  }

  @Test
  public void testAggregateMin_keyValueStringString_returnsMinOfEachComponent() {
    KeyValue<String, String> first = KeyValue.of("b", "y");
    KeyValue<String, String> second = KeyValue.of("a", "z");

    KeyValue<String, String> result = ClusterReplyAggregator.aggregateMin(first, second);

    assertEquals("a", result.getKey(), "Should return minimum key");
    assertEquals("y", result.getValue(), "Should return minimum value");
  }

  // ==================== aggregateMax - KeyValue Tests ====================

  @Test
  public void testAggregateMax_keyValueLongLong_returnsMaxOfEachComponent() {
    KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
    KeyValue<Long, Long> second = KeyValue.of(5L, 25L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMax(first, second);

    assertEquals(10L, result.getKey(), "Should return maximum key");
    assertEquals(25L, result.getValue(), "Should return maximum value");
  }

  @Test
  public void testAggregateMax_keyValueLongLong_firstLarger() {
    KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
    KeyValue<Long, Long> second = KeyValue.of(1L, 2L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMax(first, second);

    assertEquals(10L, result.getKey(), "Should return maximum key from first");
    assertEquals(20L, result.getValue(), "Should return maximum value from first");
  }

  @Test
  public void testAggregateMax_keyValueLongLong_secondLarger() {
    KeyValue<Long, Long> first = KeyValue.of(1L, 2L);
    KeyValue<Long, Long> second = KeyValue.of(10L, 20L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMax(first, second);

    assertEquals(10L, result.getKey(), "Should return maximum key from second");
    assertEquals(20L, result.getValue(), "Should return maximum value from second");
  }

  @Test
  public void testAggregateMax_keyValueLongLong_equalValues() {
    KeyValue<Long, Long> first = KeyValue.of(5L, 5L);
    KeyValue<Long, Long> second = KeyValue.of(5L, 5L);

    KeyValue<Long, Long> result = ClusterReplyAggregator.aggregateMax(first, second);

    assertEquals(5L, result.getKey(), "Should return equal key");
    assertEquals(5L, result.getValue(), "Should return equal value");
  }

  @Test
  public void testAggregateMax_keyValueStringString_returnsMaxOfEachComponent() {
    KeyValue<String, String> first = KeyValue.of("b", "y");
    KeyValue<String, String> second = KeyValue.of("a", "z");

    KeyValue<String, String> result = ClusterReplyAggregator.aggregateMax(first, second);

    assertEquals("b", result.getKey(), "Should return maximum key");
    assertEquals("z", result.getValue(), "Should return maximum value");
  }
}
