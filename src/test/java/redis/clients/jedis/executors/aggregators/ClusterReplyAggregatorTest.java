package redis.clients.jedis.executors.aggregators;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.JedisByteMap;
import redis.clients.jedis.util.KeyValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterReplyAggregatorTest {

  // ==================== aggregateAllSucceeded Tests ====================
  // Per Redis ALL_SUCCEEDED spec: returns successfully only if there are no error replies.
  // Error handling is done separately by the caller (MultiNodeResultAggregator.addError()),
  // so aggregateAllSucceeded simply returns the first reply when aggregating successful responses.
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class AggregateAllSucceededTests {
    /**
     * Provides test cases for ALL_SUCCEEDED aggregator. Each Object[] contains {firstValue,
     * secondValue, thirdValue}. Includes Long, Integer, Double, String, Boolean.
     */
    Stream<Object[]> valuesProvider() {
      return Stream.of(new Object[] { 42L, 42L, 100L }, // Long
        new Object[] { 123, 123, 456 }, // Integer
        new Object[] { 3.14159, 3.14159, 2.71828 }, // Double
        new Object[] { "OK", "OK", "DIFFERENT" }, // String
        new Object[] { true, true, false }, // Boolean
        new Object[] { false, false, true }, // Boolean
        new Object[] { new byte[] { 1, 2 }, new byte[] { 1, 2 }, new byte[] { 3, 4 } } // byte[]
      );
    }

    @ParameterizedTest
    @MethodSource("valuesProvider")
    void testAggregateAllSucceeded_returnsFirstValue(Object first, Object second, Object third) {
      @SuppressWarnings("unchecked")
      ClusterReplyAggregator<Object> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.ALL_SUCCEEDED);

      // first addition
      aggregator.add(first);
      assertThat("First value should be result", aggregator.getResult(), equalTo(first));

      // add same value again
      aggregator.add(second);
      assertThat("Result should remain first value", aggregator.getResult(), equalTo(first));

      // add a different value
      aggregator.add(third);
      assertThat("Result should still remain first value", aggregator.getResult(), equalTo(first));
    }
  }
  // ==================== aggregateDefault - List<String> Tests ====================

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  public class AggregateDefaultTests {

    // ==================== aggregateDefault - Unsupported Types Throw Exception
    // ====================

    @Test
    public void testAggregateDefault_nonListTypes_throwsUnsupportedAggregationException() {
      String first = "existing";

      ClusterReplyAggregator<String> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.DEFAULT);
      UnsupportedAggregationException exception = assertThrows(
        UnsupportedAggregationException.class, () -> aggregator.add(first));

      assertTrue(exception.getMessage().contains("DEFAULT policy requires"),
        "Exception message should describe the policy requirement");
      assertTrue(exception.getMessage().contains("String"),
        "Exception message should mention the unsupported type");
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultListTests {

      /**
       * Provides test cases: {firstList, secondList, expectedResult}.
       */
      Stream<Object[]> listProvider() {
        return Stream.of(
          // aggregate non empty lists
          new Object[] { Arrays.asList("key1", "key2"), Arrays.asList("key3", "key4"),
              Arrays.asList("key1", "key2", "key3", "key4") },
          // aggregate null and non empty list
          new Object[] { null, Arrays.asList("key1", "key2"), Arrays.asList("key1", "key2") },
          // aggregate empty and non empty list
          new Object[] { Collections.emptyList(), Arrays.asList("key1", "key2"),
              Arrays.asList("key1", "key2") },
          // aggregate empty and non empty list
          new Object[] { new ArrayList<>(), Arrays.asList("key1", "key2"),
              Arrays.asList("key1", "key2") },
          // aggregate non empty and empty list
          new Object[] { Arrays.asList("key1", "key2"), new ArrayList<>(),
              Arrays.asList("key1", "key2") },
          // aggregate two empty lists
          new Object[] { new ArrayList<>(), new ArrayList<>(), new ArrayList<>() }, // both empty →
                                                                                    // empty
          // aggregate two null lists
          new Object[] { null, null, null });
      }

      @ParameterizedTest
      @MethodSource("listProvider")
      void testAggregateDefault_lists(List<String> first, List<String> second,
          List<String> expected) {
        ClusterReplyAggregator<List<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);

        aggregator.add(first);
        aggregator.add(second);

        List<String> result = aggregator.getResult();
        assertThat("Aggregated list should match expected", result, equalTo(expected));
      }

      // ==================== aggregateDefault - List<byte[]> Tests ====================

      @Test
      public void testAggregateDefault_twoByteArrayLists_concatenatesThem() {
        List<byte[]> first = new ArrayList<>(
            Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 }));
        List<byte[]> second = new ArrayList<>(
            Arrays.asList(new byte[] { 5, 6 }, new byte[] { 7, 8 }));

        ClusterReplyAggregator<List<byte[]>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);
        List<byte[]> result = aggregator.getResult();
        assertEquals(4, result.size(), "Should contain all byte arrays from both lists");
        assertThat(result, contains(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5, 6 },
          new byte[] { 7, 8 }));
      }

      // ==================== aggregateDefault - Different List Implementations ====================

      @Test
      public void testAggregateDefault_linkedListAndArrayList() {
        List<String> first = new LinkedList<>(Arrays.asList("a", "b"));
        List<String> second = new ArrayList<>(Arrays.asList("c", "d"));

        ClusterReplyAggregator<List<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        List<String> result = aggregator.getResult();
        assertEquals(4, result.size(), "Should concatenate different list implementations");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
      }
      // ==================== aggregateDefault - Mutates Existing ArrayList In Place
      // ====================

      @Test
      public void testAggregateDefault_singleReplyDoesNotCreateNewList() {
        List<String> first = null;
        List<String> second = new ArrayList<>(Arrays.asList("c", "d"));

        ClusterReplyAggregator<List<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        List<String> result = aggregator.getResult();

        // If single non null reply, Result should be the same instance
        assertSame(second, result, "Result should be the same instance as first non null reply");
        assertThat(result, contains("c", "d"));
        assertThat(result, sameInstance(second));
      }
    }

    // ==================== aggregateDefault - Map Tests ====================

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultMapTests {

      @Test
      public void testAggregateDefault_twoMapsWithDifferentKeys_mergesThem() {
        Map<String, Integer> first = new HashMap<>();
        first.put("key1", 1);
        first.put("key2", 2);

        Map<String, Integer> second = new HashMap<>();
        second.put("key3", 3);
        second.put("key4", 4);

        ClusterReplyAggregator<Map<String, Integer>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Map<String, Integer> result = aggregator.getResult();
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

        ClusterReplyAggregator<Map<String, String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Map<String, String> result = aggregator.getResult();
        assertEquals(3, result.size(), "Should contain merged entries");
        assertEquals("second_value", result.get("shared"),
          "Second map's value should overwrite first");
        assertEquals("value1", result.get("unique1"));
        assertEquals("value2", result.get("unique2"));
      }

      /**
       * Provides test cases: {firstMap, secondMap, expectedResult}.
       */
      Stream<Object[]> mapProvider() {
        Map<String, Integer> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("key1", 1);
        nonEmptyMap.put("key2", 2);

        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("key1", 1);
        expectedMap.put("key2", 2);

        return Stream.of(
          // empty + non-empty → non-empty
          new Object[] { new HashMap<String, Integer>(), nonEmptyMap, expectedMap },
          // non-empty + empty → non-empty
          new Object[] { nonEmptyMap, new HashMap<String, Integer>(), expectedMap },
          // empty + empty → empty
          new Object[] { new HashMap<String, Integer>(), new HashMap<String, Integer>(),
              new HashMap<String, Integer>() },
          // null + null → null
          new Object[] { null, null, null },
          // null + empty → empty
          new Object[] { null, new HashMap<String, Integer>(), new HashMap<String, Integer>() });
      }

      @ParameterizedTest
      @MethodSource("mapProvider")
      void testAggregateDefault_maps(Map<String, Integer> first, Map<String, Integer> second,
          Map<String, Integer> expected) {
        ClusterReplyAggregator<Map<String, Integer>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);

        aggregator.add(first);
        aggregator.add(second);

        Map<String, Integer> result = aggregator.getResult();
        assertThat("Aggregated map should match expected", result, equalTo(expected));
      }

      @Test
      public void testAggregateDefault_differentMapImplementations_mergesThem() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("a", "1");
        first.put("b", "2");

        Map<String, String> second = new HashMap<>();
        second.put("c", "3");
        second.put("d", "4");

        ClusterReplyAggregator<Map<String, String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Map<String, String> result = aggregator.getResult();
        assertEquals(4, result.size(), "Should merge different map implementations");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        assertEquals("4", result.get("d"));
        assertTrue(result instanceof HashMap, "Result should be a HashMap");
      }

      @Test
      public void testAggregateDefault_mergesHashMaps() {
        Map<String, String> first = new HashMap<>();
        first.put("a", "1");
        first.put("b", "2");

        Map<String, String> second = new HashMap<>();
        second.put("c", "3");
        second.put("d", "4");

        ClusterReplyAggregator<Map<String, String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Map<String, String> result = aggregator.getResult();
        // ClusterReplyAggregator merges maps
        assertEquals(4, result.size(), "Result should contain all entries");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        assertEquals("4", result.get("d"));
        // Second map should NOT be modified
        assertEquals(2, second.size(), "Second map should not be modified");
        assertEquals("3", second.get("c"));
        assertEquals("4", second.get("d"));
      }

      @Test
      public void testAggregateDefault_unmodifiableMap_returnsNewHashMap() {
        Map<String, String> first = Collections.emptyMap();
        Map<String, String> second = new HashMap<>();
        second.put("a", "1");
        second.put("b", "2");
        Map<String, String> third = new HashMap<>();
        second.put("c", "3");
        second.put("d", "4");

        ClusterReplyAggregator<Map<String, String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);
        aggregator.add(third);

        Map<String, String> result = aggregator.getResult();
        assertNotNull(result);
        assertThat(result, instanceOf(HashMap.class));
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        assertEquals("4", result.get("d"));
      }
    }

    // ==================== aggregateDefault - Set Tests ====================

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultSetTests {

      /**
       * Provides test cases: {firstSet, secondSet, expectedResult}.
       */
      Stream<Object[]> setProvider() {
        Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> expectedSet = new HashSet<>(Arrays.asList("a", "b"));

        return Stream.of(
          // empty + non-empty → non-empty
          new Object[] { new HashSet<String>(), nonEmptySet, expectedSet },
          // non-empty + empty → non-empty
          new Object[] { nonEmptySet, new HashSet<String>(), expectedSet },
          // empty + empty → empty
          new Object[] { new HashSet<String>(), new HashSet<String>(), new HashSet<String>() },
          // sets with overlapping elements, merges without duplicates
          new Object[] { new HashSet<String>(Arrays.asList("a", "b", "c")),
              new HashSet<String>(Arrays.asList("b", "c", "d")),
              new HashSet<String>(Arrays.asList("a", "b", "c", "d")) },
          // sets with different elements, merges all elements
          new Object[] { new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("c", "d")),
              new HashSet<String>(Arrays.asList("a", "b", "c", "d")) },
          // different set implementations, merges all elements
          new Object[] { new LinkedHashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("c", "d")),
              new HashSet<String>(Arrays.asList("a", "b", "c", "d")) });

      }

      @ParameterizedTest
      @MethodSource("setProvider")
      void testAggregateDefault_sets(Set<String> first, Set<String> second, Set<String> expected) {
        ClusterReplyAggregator<Set<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);

        aggregator.add(first);
        aggregator.add(second);

        Set<String> result = aggregator.getResult();
        assertThat("Aggregated set should match expected", result, instanceOf(HashSet.class));
        assertThat("Aggregated set should match expected", result,
          containsInAnyOrder(expected.toArray(new String[0])));
      }

      /**
       * Provides test cases: {firstSet, secondSet, expectedResult}.
       */
      Stream<Object[]> setByteArrayProvider() {
        Set<byte[]> nonEmptySet1 = new HashSet<>(Arrays.asList("a".getBytes(), "b".getBytes()));
        Set<byte[]> nonEmptySet2 = new HashSet<>(Arrays.asList("c".getBytes(), "d".getBytes()));

        Set<byte[]> expectedSet = new HashSet<>(
            Arrays.asList("a".getBytes(), "b".getBytes(), "c".getBytes(), "d".getBytes()));
        return Stream.of(
          // set of byte arrays
          new Object[] { nonEmptySet1, nonEmptySet2, expectedSet },
          // empty + non-empty → non-empty
          new Object[] { new HashSet<byte[]>(), nonEmptySet1, nonEmptySet1 },
          // non-empty + empty → non-empty
          new Object[] { nonEmptySet1, new HashSet<byte[]>(), nonEmptySet1 });

      }

      @ParameterizedTest
      @MethodSource("setByteArrayProvider")
      void testAggregateDefault_sets_byteArrays(Set<byte[]> first, Set<byte[]> second,
          Set<byte[]> expected) {
        ClusterReplyAggregator<Set<byte[]>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);

        aggregator.add(first);
        aggregator.add(second);

        Set<byte[]> result = aggregator.getResult();
        assertThat("Aggregated set should match expected", result, instanceOf(HashSet.class));
        assertThat(result.toArray(new byte[0][]),
          arrayContainingInAnyOrder(expected.toArray(new byte[0][])));
      }

      @Test
      public void testAggregateDefault_singleSet_returnsSameInstance() {
        Set<String> first = null;
        Set<String> second = new HashSet<>(Arrays.asList("c", "d"));

        ClusterReplyAggregator<Set<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Set<String> result = aggregator.getResult();

        // ClusterReplyAggregator mutates the first set in place
        assertThat(result, sameInstance(second));
        assertThat(result, contains("c", "d"));
      }

      @Test
      public void testAggregateDefault_byteArraySets_mergesThem() {
        // Testing with byte[] sets similar to what BINARY_SET returns
        Set<byte[]> first = new HashSet<>();
        first.add(new byte[] { 1, 2 });
        first.add(new byte[] { 3, 4 });

        Set<byte[]> second = new HashSet<>();
        second.add(new byte[] { 5, 6 });

        ClusterReplyAggregator<Set<byte[]>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Set<byte[]> result = aggregator.getResult();

        assertEquals(3, result.size(), "Should contain all byte arrays from both sets");
        assertTrue(result instanceof HashSet, "Result should be a HashSet");
      }

      @Test
      public void testAggregateDefault_byteArraySets_overlapping_mergesThem() {
        // Testing with byte[] sets similar to what BINARY_SET returns
        Set<byte[]> first = new HashSet<>();
        first.add(new byte[] { 1, 2 });
        first.add(new byte[] { 3, 4 });

        Set<byte[]> second = new HashSet<>();
        second.add(new byte[] { 3, 4 });
        second.add(new byte[] { 5, 6 });

        ClusterReplyAggregator<Set<byte[]>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        Set<byte[]> result = aggregator.getResult();

        assertEquals(3, result.size(), "Should contain all byte arrays from both sets");
        assertTrue(result instanceof HashSet, "Result should be a HashSet");
      }
    }

    // ==================== aggregateDefault - JedisByteHashMap Tests ====================

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultJedisByteHashMapTests {

      @Test
      public void testAggregateDefault_twoJedisByteHashMapsWithDifferentKeys_mergesThem() {
        JedisByteHashMap first = new JedisByteHashMap();
        first.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
        first.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });

        JedisByteHashMap second = new JedisByteHashMap();
        second.put(new byte[] { 'k', '3' }, new byte[] { 'v', '3' });
        second.put(new byte[] { 'k', '4' }, new byte[] { 'v', '4' });

        ClusterReplyAggregator<JedisByteHashMap> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteHashMap result = aggregator.getResult();

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
        first.put(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' },
          new byte[] { 'f', 'i', 'r', 's', 't' });
        first.put(new byte[] { 'u', 'n', 'i', 'q', '1' }, new byte[] { 'v', 'a', 'l', '1' });

        JedisByteHashMap second = new JedisByteHashMap();
        second.put(new byte[] { 's', 'h', 'a', 'r', 'e', 'd' },
          new byte[] { 's', 'e', 'c', 'o', 'n', 'd' });
        second.put(new byte[] { 'u', 'n', 'i', 'q', '2' }, new byte[] { 'v', 'a', 'l', '2' });

        ClusterReplyAggregator<JedisByteHashMap> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteHashMap result = aggregator.getResult();

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

        ClusterReplyAggregator<JedisByteHashMap> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteHashMap result = aggregator.getResult();

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

        ClusterReplyAggregator<JedisByteHashMap> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteHashMap result = aggregator.getResult();

        assertEquals(2, result.size(), "Should contain entries from first map");
        assertArrayEquals(new byte[] { 'v', '1' }, result.get(new byte[] { 'k', '1' }));
        assertArrayEquals(new byte[] { 'v', '2' }, result.get(new byte[] { 'k', '2' }));
      }

      @Test
      public void testAggregateDefault_mergesJedisByteHashMaps() {
        JedisByteHashMap first = new JedisByteHashMap();
        first.put(new byte[] { 'a' }, new byte[] { '1' });
        first.put(new byte[] { 'b' }, new byte[] { '2' });

        JedisByteHashMap second = new JedisByteHashMap();
        second.put(new byte[] { 'c' }, new byte[] { '3' });
        second.put(new byte[] { 'd' }, new byte[] { '4' });

        ClusterReplyAggregator<JedisByteHashMap> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteHashMap result = aggregator.getResult();

        // ClusterReplyAggregator creates a new JedisByteHashMap with merged entries
        assertEquals(4, result.size(), "Result should contain all entries");
        assertArrayEquals(new byte[] { '1' }, result.get(new byte[] { 'a' }));
        assertArrayEquals(new byte[] { '2' }, result.get(new byte[] { 'b' }));
        assertArrayEquals(new byte[] { '3' }, result.get(new byte[] { 'c' }));
        assertArrayEquals(new byte[] { '4' }, result.get(new byte[] { 'd' }));
        // Second map should NOT be modified
        assertEquals(2, second.size(), "Second map should not be modified");
        assertArrayEquals(new byte[] { '3' }, second.get(new byte[] { 'c' }));
        assertArrayEquals(new byte[] { '4' }, second.get(new byte[] { 'd' }));
      }
    }

    // ==================== aggregateDefault - JedisByteMap Tests ====================

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultJedisByteMapTests {

      @Test
      public void testAggregateDefault_twoJedisByteMapsWithDifferentKeys_mergesThem() {
        JedisByteMap<String> first = new JedisByteMap<>();
        first.put(new byte[] { 'k', '1' }, "value1");
        first.put(new byte[] { 'k', '2' }, "value2");

        JedisByteMap<String> second = new JedisByteMap<>();
        second.put(new byte[] { 'k', '3' }, "value3");
        second.put(new byte[] { 'k', '4' }, "value4");

        ClusterReplyAggregator<JedisByteMap<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteMap<String> result = aggregator.getResult();

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

        ClusterReplyAggregator<JedisByteMap<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteMap<String> result = aggregator.getResult();

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

        ClusterReplyAggregator<JedisByteMap<Integer>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteMap<Integer> result = aggregator.getResult();

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

        ClusterReplyAggregator<JedisByteMap<Integer>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteMap<Integer> result = aggregator.getResult();

        assertEquals(2, result.size(), "Should contain entries from first map");
        assertEquals(1, result.get(new byte[] { 'k', '1' }));
        assertEquals(2, result.get(new byte[] { 'k', '2' }));
      }

      @Test
      public void testAggregateDefault_mergesJedisByteMaps() {
        JedisByteMap<String> first = new JedisByteMap<>();
        first.put(new byte[] { 'a' }, "1");
        first.put(new byte[] { 'b' }, "2");

        JedisByteMap<String> second = new JedisByteMap<>();
        second.put(new byte[] { 'c' }, "3");
        second.put(new byte[] { 'd' }, "4");

        ClusterReplyAggregator<JedisByteMap<String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);
        aggregator.add(first);
        aggregator.add(second);

        JedisByteMap<String> result = aggregator.getResult();

        // ClusterReplyAggregator creates a new JedisByteMap with merged entries
        assertEquals(4, result.size(), "Result should contain all entries");
        assertEquals("1", result.get(new byte[] { 'a' }));
        assertEquals("2", result.get(new byte[] { 'b' }));
        assertEquals("3", result.get(new byte[] { 'c' }));
        assertEquals("4", result.get(new byte[] { 'd' }));
        // Second map should NOT be modified
        assertEquals(2, second.size(), "Second map should not be modified");
        assertEquals("3", second.get(new byte[] { 'c' }));
        assertEquals("4", second.get(new byte[] { 'd' }));
      }
    }
  }

  // ==================== aggregateMin - KeyValue Tests ====================

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class AggregateMinTests {

    @Test
    public void testAggregateMin_keyValueLongLong_returnsMinOfEachComponent() {
      KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
      KeyValue<Long, Long> second = KeyValue.of(5L, 25L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MIN);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(5L, result.getKey(), "Should return minimum key");
      assertEquals(20L, result.getValue(), "Should return minimum value");
    }

    @Test
    public void testAggregateMin_keyValueLongLong_firstSmaller() {
      KeyValue<Long, Long> first = KeyValue.of(1L, 2L);
      KeyValue<Long, Long> second = KeyValue.of(10L, 20L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MIN);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(1L, result.getKey(), "Should return minimum key from first");
      assertEquals(2L, result.getValue(), "Should return minimum value from first");
    }

    @Test
    public void testAggregateMin_keyValueLongLong_secondSmaller() {
      KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
      KeyValue<Long, Long> second = KeyValue.of(1L, 2L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MIN);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(1L, result.getKey(), "Should return minimum key from second");
      assertEquals(2L, result.getValue(), "Should return minimum value from second");
    }

    @Test
    public void testAggregateMin_keyValueLongLong_equalValues() {
      KeyValue<Long, Long> first = KeyValue.of(5L, 5L);
      KeyValue<Long, Long> second = KeyValue.of(5L, 5L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MIN);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(5L, result.getKey(), "Should return equal key");
      assertEquals(5L, result.getValue(), "Should return equal value");
    }

    @Test
    public void testAggregateMin_keyValueStringString_returnsMinOfEachComponent() {
      KeyValue<String, String> first = KeyValue.of("b", "y");
      KeyValue<String, String> second = KeyValue.of("a", "z");

      ClusterReplyAggregator<KeyValue<String, String>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MIN);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<String, String> result = aggregator.getResult();

      assertEquals("a", result.getKey(), "Should return minimum key");
      assertEquals("y", result.getValue(), "Should return minimum value");
    }
  }

  // ==================== aggregateMax - KeyValue Tests ====================

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class AggregateMaxTests {

    @Test
    public void testAggregateMax_keyValueLongLong_returnsMaxOfEachComponent() {
      KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
      KeyValue<Long, Long> second = KeyValue.of(5L, 25L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MAX);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(10L, result.getKey(), "Should return maximum key");
      assertEquals(25L, result.getValue(), "Should return maximum value");
    }

    @Test
    public void testAggregateMax_keyValueLongLong_firstLarger() {
      KeyValue<Long, Long> first = KeyValue.of(10L, 20L);
      KeyValue<Long, Long> second = KeyValue.of(1L, 2L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MAX);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(10L, result.getKey(), "Should return maximum key from first");
      assertEquals(20L, result.getValue(), "Should return maximum value from first");
    }

    @Test
    public void testAggregateMax_keyValueLongLong_secondLarger() {
      KeyValue<Long, Long> first = KeyValue.of(1L, 2L);
      KeyValue<Long, Long> second = KeyValue.of(10L, 20L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MAX);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(10L, result.getKey(), "Should return maximum key from second");
      assertEquals(20L, result.getValue(), "Should return maximum value from second");
    }

    @Test
    public void testAggregateMax_keyValueLongLong_equalValues() {
      KeyValue<Long, Long> first = KeyValue.of(5L, 5L);
      KeyValue<Long, Long> second = KeyValue.of(5L, 5L);

      ClusterReplyAggregator<KeyValue<Long, Long>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MAX);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<Long, Long> result = aggregator.getResult();

      assertEquals(5L, result.getKey(), "Should return equal key");
      assertEquals(5L, result.getValue(), "Should return equal value");
    }

    @Test
    public void testAggregateMax_keyValueStringString_returnsMaxOfEachComponent() {
      KeyValue<String, String> first = KeyValue.of("b", "y");
      KeyValue<String, String> second = KeyValue.of("a", "z");

      ClusterReplyAggregator<KeyValue<String, String>> aggregator = new ClusterReplyAggregator<>(
          CommandFlagsRegistry.ResponsePolicy.AGG_MAX);
      aggregator.add(first);
      aggregator.add(second);

      KeyValue<String, String> result = aggregator.getResult();

      assertEquals("b", result.getKey(), "Should return maximum key");
      assertEquals("z", result.getValue(), "Should return maximum value");
    }
  }

}
