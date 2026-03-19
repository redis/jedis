package redis.clients.jedis.executors.aggregators;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.ByteArrayMapMatcher;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.JedisByteMap;
import redis.clients.jedis.util.JedisByteMapMatcher;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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

      /**
       * Provides test cases: {firstMap, secondMap, expectedResult}.
       */
      Stream<Object[]> mapProvider() {
        Map<String, Integer> firstMap = new HashMap<>();
        firstMap.put("key1", 1);
        firstMap.put("key2", 2);

        Map<String, Integer> secondMap = new HashMap<>();
        secondMap.put("key3", 3);
        secondMap.put("key4", 4);

        Map<String, Integer> expectedFirstOnly = new HashMap<>();
        expectedFirstOnly.put("key1", 1);
        expectedFirstOnly.put("key2", 2);

        Map<String, Integer> expectedMergedMap = new HashMap<>();
        expectedMergedMap.put("key1", 1);
        expectedMergedMap.put("key2", 2);
        expectedMergedMap.put("key3", 3);
        expectedMergedMap.put("key4", 4);

        Map<String, Integer> overlappingMap = new HashMap<>();
        overlappingMap.put("key1", 1);
        overlappingMap.put("key3", 3);

        Map<String, Integer> expectedOverlappingMap = new HashMap<>();
        expectedOverlappingMap.put("key1", 1);
        expectedOverlappingMap.put("key2", 2);
        expectedOverlappingMap.put("key3", 3);

        return Stream.of(
          // empty + non-empty → non-empty
          new Object[] { new HashMap<String, Integer>(), firstMap, expectedFirstOnly },
          // non-empty + empty → non-empty
          new Object[] { firstMap, new HashMap<String, Integer>(), expectedFirstOnly },
          // empty + empty → empty
          new Object[] { new HashMap<String, Integer>(), new HashMap<String, Integer>(),
              new HashMap<String, Integer>() },
          // null + null → null
          new Object[] { null, null, null },
          // null + empty → empty
          new Object[] { null, new HashMap<String, Integer>(), new HashMap<String, Integer>() },
          // unmodifiableMap + non-empty → non-empty
          new Object[] { Collections.emptyMap(), firstMap, expectedFirstOnly },
          // non-empty + unmodifiableMap → non-empty
          new Object[] { firstMap, Collections.emptyMap(), expectedFirstOnly },
          // maps with different keys
          new Object[] { firstMap, secondMap, expectedMergedMap },
          // maps with overlapping keys, second map takes precedence
          new Object[] { firstMap, overlappingMap, expectedOverlappingMap }

        );

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
          // unmodifiableSet + non-empty → non-empty
          new Object[] { Collections.emptySet(), nonEmptySet, expectedSet },
          // non-empty + unmodifiableSet → non-empty
          new Object[] { nonEmptySet, Collections.emptySet(), expectedSet },
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
          // overlapping elements
          new Object[] { new HashSet<>(Arrays.asList("a".getBytes(), "b".getBytes())),
              new HashSet<>(Arrays.asList("b".getBytes(), "c".getBytes())),
              new HashSet<>(Arrays.asList("a".getBytes(), "b".getBytes(), "c".getBytes())) },
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
    }

    // ==================== aggregateDefault - JedisByteHashMap Tests ====================

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultJedisByteHashMapTests {

      /**
       * Provides test cases: {firstMap, secondMap, expectedResult}.
       */
      Stream<Object[]> jedisByteHashMapProvider() {

        JedisByteHashMap first = new JedisByteHashMap();
        first.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
        first.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });

        JedisByteHashMap second = new JedisByteHashMap();
        second.put(new byte[] { 'k', '3' }, new byte[] { 'v', '3' });
        second.put(new byte[] { 'k', '4' }, new byte[] { 'v', '4' });

        JedisByteHashMap expectedFirstOnly = new JedisByteHashMap();
        expectedFirstOnly.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
        expectedFirstOnly.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });

        JedisByteHashMap expectedFirstSecondMerged = new JedisByteHashMap();
        expectedFirstSecondMerged.put(new byte[] { 'k', '1' }, new byte[] { 'v', '1' });
        expectedFirstSecondMerged.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });
        expectedFirstSecondMerged.put(new byte[] { 'k', '3' }, new byte[] { 'v', '3' });
        expectedFirstSecondMerged.put(new byte[] { 'k', '4' }, new byte[] { 'v', '4' });

        JedisByteHashMap overlapFirstKeys = new JedisByteHashMap();
        overlapFirstKeys.put(new byte[] { 'k', '1' }, new byte[] { 'v', 'A' });
        overlapFirstKeys.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });
        overlapFirstKeys.put(new byte[] { 'k', '3' }, new byte[] { 'v', '3' });

        JedisByteHashMap overlapKeysMerged = new JedisByteHashMap();
        overlapKeysMerged.put(new byte[] { 'k', '1' }, new byte[] { 'v', 'A' });
        overlapKeysMerged.put(new byte[] { 'k', '2' }, new byte[] { 'v', '2' });
        overlapKeysMerged.put(new byte[] { 'k', '3' }, new byte[] { 'v', '3' });

        return Stream.of(
          // empty + non-empty → non-empty
          new Object[] { new JedisByteHashMap(), first, expectedFirstOnly },
          // non-empty + empty → non-empty
          new Object[] { first, new JedisByteHashMap(), expectedFirstOnly },
          // empty + empty → empty
          new Object[] { new JedisByteHashMap(), new JedisByteHashMap(), new JedisByteHashMap() },
          // null + null → null
          new Object[] { null, null, null },
          // null + empty → empty
          new Object[] { null, new JedisByteHashMap(), new JedisByteHashMap() },
          // maps with no overlapping keys
          new Object[] { first, second, expectedFirstSecondMerged },
          // maps with overlapping keys, second map takes precedence
          new Object[] { first, overlapFirstKeys, overlapKeysMerged });
      }

      @ParameterizedTest
      @MethodSource("jedisByteHashMapProvider")
      void testAggregateDefault_jedisByteHashMap(JedisByteHashMap first, JedisByteHashMap second,
          JedisByteHashMap expected) {
        ClusterReplyAggregator<Map<byte[], byte[]>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);

        aggregator.add(first);
        aggregator.add(second);

        Map<byte[], byte[]> result = aggregator.getResult();

        if (expected == null) {
          assertNull(result);
        } else {
          assertThat(result, instanceOf(JedisByteHashMap.class));
          assertThat(result, ByteArrayMapMatcher.contentEquals(expected));
        }
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
    }

    // ==================== aggregateDefault - JedisByteMap Tests ====================

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AggregateDefaultJedisByteMapTests {

      /**
       * Provides test cases: {firstMap, secondMap, expectedResult}.
       */
      Stream<Object[]> jedisByteMapProvider() {

        JedisByteMap<String> first = new JedisByteMap<>();
        first.put(new byte[] { 'k', '1' }, "v1");
        first.put(new byte[] { 'k', '2' }, "v2");

        JedisByteMap<String> second = new JedisByteMap<>();
        second.put(new byte[] { 'k', '3' }, "v3");
        second.put(new byte[] { 'k', '4' }, "v4");

        JedisByteMap<String> expectedFirstOnly = new JedisByteMap<>();
        expectedFirstOnly.put(new byte[] { 'k', '1' }, "v1");
        expectedFirstOnly.put(new byte[] { 'k', '2' }, "v2");

        JedisByteMap<String> expectedFirstSecondMerged = new JedisByteMap<>();
        expectedFirstSecondMerged.put(new byte[] { 'k', '1' }, "v1");
        expectedFirstSecondMerged.put(new byte[] { 'k', '2' }, "v2");
        expectedFirstSecondMerged.put(new byte[] { 'k', '3' }, "v3");
        expectedFirstSecondMerged.put(new byte[] { 'k', '4' }, "v4");

        JedisByteMap<String> overlapFirstKeys = new JedisByteMap<>();
        overlapFirstKeys.put(new byte[] { 'k', '1' }, "vA");
        overlapFirstKeys.put(new byte[] { 'k', '2' }, "v2");
        overlapFirstKeys.put(new byte[] { 'k', '3' }, "v3");

        JedisByteMap<String> overlapKeysMerged = new JedisByteMap<>();
        overlapKeysMerged.put(new byte[] { 'k', '1' }, "vA");
        overlapKeysMerged.put(new byte[] { 'k', '2' }, "v2");
        overlapKeysMerged.put(new byte[] { 'k', '3' }, "v3");

        return Stream.of(
          // empty + non-empty → non-empty
          new Object[] { new JedisByteMap<>(), first, expectedFirstOnly },
          // non-empty + empty → non-empty
          new Object[] { first, new JedisByteMap<>(), expectedFirstOnly },
          // empty + empty → empty
          new Object[] { new JedisByteMap<>(), new JedisByteMap<>(), new JedisByteMap<>() },
          // null + null → null
          new Object[] { null, null, null },
          // null + empty → empty
          new Object[] { null, new JedisByteMap<>(), new JedisByteMap<>() },
          // maps with no overlapping keys
          new Object[] { first, second, expectedFirstSecondMerged },
          // maps with overlapping keys, second map takes precedence
          new Object[] { first, overlapFirstKeys, overlapKeysMerged });
      }

      @ParameterizedTest
      @MethodSource("jedisByteMapProvider")
      void testAggregateDefault_jedisByteHashMap(Map<byte[], String> first,
          Map<byte[], String> second, Map<byte[], String> expected) {
        ClusterReplyAggregator<Map<byte[], String>> aggregator = new ClusterReplyAggregator<>(
            CommandFlagsRegistry.ResponsePolicy.DEFAULT);

        aggregator.add(first);
        aggregator.add(second);

        Map<byte[], String> result = aggregator.getResult();

        if (expected == null) {
          assertNull(result);
        } else {
          assertThat(result, instanceOf(JedisByteMap.class));
          assertThat(result, JedisByteMapMatcher.contentEquals(expected));
        }
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
