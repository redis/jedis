package redis.clients.jedis.executors.aggregators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandFlagsRegistry.ResponsePolicy;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.JedisBroadcastException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

public class MultiNodeResultAggregatorTest {

  private static final HostAndPort NODE_1 = HostAndPort.from("127.0.0.1:7001");
  private static final HostAndPort NODE_2 = HostAndPort.from("127.0.0.1:7002");
  private static final HostAndPort NODE_3 = HostAndPort.from("127.0.0.1:7003");
  private static final HostAndPort UNKNOWN_NODE = HostAndPort.from("unknown:0");

  // ==================== Constructor Tests ====================

  @Test
  public void testConstructor_initializesWithResponsePolicy() {
    MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
        ResponsePolicy.ALL_SUCCEEDED);

    assertEquals(ResponsePolicy.ALL_SUCCEEDED, aggregator.getResponsePolicy(),
      "Should store the provided response policy");
  }

  @Test
  public void testConstructor_initializesWithDifferentPolicies() {
    for (ResponsePolicy policy : ResponsePolicy.values()) {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(policy);
      assertEquals(policy, aggregator.getResponsePolicy(),
        "Should store the response policy: " + policy);
    }
  }

  @Nested
  class BasicTests {
    // ==================== getResponsePolicy Tests ====================

    @Test
    public void testGetResponsePolicy_returnsCorrectPolicy() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_SUM);

      assertEquals(ResponsePolicy.AGG_SUM, aggregator.getResponsePolicy(),
        "getResponsePolicy should return the policy passed to constructor");
    }

    // ==================== addSuccess(HostAndPort, T) Tests ====================
    @Test
    public void testAddSuccess_withNode_addsToRepliesMap() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addSuccess(NODE_1, "OK");
      aggregator.addError(NODE_2, new RuntimeException("error"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(2, replies.size(), "Should have entries for both nodes");
      assertEquals("OK", replies.get(NODE_1), "Should contain the success reply for NODE_1");
      assertTrue(replies.get(NODE_2) instanceof RuntimeException,
        "Should contain the error for NODE_2");
    }

    @Test
    public void testAddSuccess_withNullNode_aggregatesResult() {
      MultiNodeResultAggregator<List<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(null, Collections.singletonList("S1"));
      aggregator.addSuccess(null, Collections.singletonList("S2"));
      List<String> result = aggregator.getResult();

      assertThat(result, contains("S1", "S2"));
    }

    @Test
    public void testAddSuccess_withNullNode_doesNotAddToRepliesMap() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addSuccess(null, "result");
      aggregator.addError(NODE_1, new RuntimeException("error"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(1, replies.size(), "Should only have entry for NODE_1, not null node");
      assertFalse(replies.containsKey(null), "Should not contain null key");
    }

    // ==================== addSuccess(T) Tests ====================

    @Test
    public void testAddSuccess_withoutNode_recordsResult() {
      MultiNodeResultAggregator<List<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(Collections.singletonList("S1"));

      assertThat(aggregator.getResult(), contains("S1"));
    }

    @Test
    public void testAddSuccess_withoutNode_aggregatesMultipleResults() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_SUM);

      aggregator.addSuccess(10L);
      aggregator.addSuccess(20L);
      aggregator.addSuccess(30L);
      Long result = aggregator.getResult();

      assertEquals(60L, result, "Should aggregate results using AGG_SUM policy");
    }

    // ==================== addError(HostAndPort, Exception) Tests ====================

    @Test
    public void testAddError_withNode_recordsErrorAndNode() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);
      RuntimeException error = new RuntimeException("Connection failed");

      aggregator.addError(NODE_1, error);

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(1, replies.size(), "Should have one error entry");
      assertSame(error, replies.get(NODE_1), "Should contain the exception for NODE_1");
    }

    @Test
    public void testAddError_multipleNodes_recordsAllErrors() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);
      RuntimeException error1 = new RuntimeException("Error 1");
      RuntimeException error2 = new RuntimeException("Error 2");

      aggregator.addError(NODE_1, error1);
      aggregator.addError(NODE_2, error2);

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(2, replies.size(), "Should have two error entries");
      assertSame(error1, replies.get(NODE_1));
      assertSame(error2, replies.get(NODE_2));
    }

    // ==================== addError(Exception) Tests ====================

    @Test
    public void testAddError_withJedisClusterOperationException_extractsNode() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);
      JedisClusterOperationException error = new JedisClusterOperationException("Cluster error",
          NODE_1);

      aggregator.addError(error);

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(1, replies.size(), "Should have one error entry");
      assertTrue(replies.containsKey(NODE_1),
        "Should extract node from JedisClusterOperationException");
      assertSame(error, replies.get(NODE_1), "Should contain the original exception");
    }

    @Test
    public void testAddError_withJedisClusterOperationException_nullNode_usesUnknown() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);
      // JedisClusterOperationException without node (null)
      JedisClusterOperationException error = new JedisClusterOperationException("Cluster error");

      aggregator.addError(error);

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(1, replies.size(), "Should have one error entry");
      assertTrue(replies.containsKey(UNKNOWN_NODE), "Should use unknown:0 when node is null");
    }

    @Test
    public void testAddError_withRegularException_usesUnknownNode() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);
      RuntimeException error = new RuntimeException("Generic error");

      aggregator.addError(error);

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(1, replies.size(), "Should have one error entry");
      assertTrue(replies.containsKey(UNKNOWN_NODE),
        "Should use unknown:0 for non-JedisClusterOperationException");
      assertSame(error, replies.get(UNKNOWN_NODE));
    }

    // ==================== getResult() - ONE_SUCCEEDED Policy Tests ====================

    @Test
    public void testGetResult_oneSucceeded_allNodesSucceed_returnsAggregatedResult() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      aggregator.addSuccess(NODE_1, "OK");
      aggregator.addSuccess(NODE_2, "OK");
      aggregator.addSuccess(NODE_3, "OK");

      String result = aggregator.getResult();
      assertEquals("OK", result, "Should return successful result when all nodes succeed");
    }

    @Test
    public void testGetResult_oneSucceeded_oneNodeSucceedsOthersFail_returnsSuccess() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      aggregator.addError(NODE_1, new RuntimeException("Error 1"));
      aggregator.addSuccess(NODE_2, "OK");
      aggregator.addError(NODE_3, new RuntimeException("Error 3"));

      // ONE_SUCCEEDED should return success if at least one node succeeded
      String result = aggregator.getResult();
      assertEquals("OK", result, "Should return success if at least one node succeeded");
    }

    @Test
    public void testGetResult_oneSucceeded_allNodesFail_throwsException() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      aggregator.addError(NODE_1, new RuntimeException("Error 1"));
      aggregator.addError(NODE_2, new RuntimeException("Error 2"));
      aggregator.addError(NODE_3, new RuntimeException("Error 3"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult(), "Should throw JedisBroadcastException when all nodes fail");

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(3, replies.size(), "Should contain all error replies");
    }

    @Test
    public void testGetResult_oneSucceeded_mixedResults_returnsFirstSuccess() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      aggregator.addError(NODE_1, new RuntimeException("Error"));
      aggregator.addSuccess(NODE_2, 42L);
      aggregator.addSuccess(NODE_3, 100L);

      Long result = aggregator.getResult();
      // ONE_SUCCEEDED returns the first successful result (existing value)
      assertEquals(42L, result, "Should return the first successful result");
    }

  }

  // ==================== getResult() - ALL_SUCCEEDED Policy Tests ====================
  @Nested
  class AllSucceededPolicyTests {
    @Test
    public void testGetResult_allSucceeded_allNodesSucceed_returnsResult() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addSuccess(NODE_1, "OK");
      aggregator.addSuccess(NODE_2, "OK");

      String result = aggregator.getResult();
      assertEquals("OK", result, "Should return result when all nodes succeed with equal values");
    }

    @Test
    public void testGetResult_allSucceeded_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addSuccess(NODE_1, "OK");
      aggregator.addError(NODE_2, new RuntimeException("Connection failed"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult(), "Should throw JedisBroadcastException when any node fails");

      assertTrue(ex.getMessage().contains("failed"),
        "Exception message should indicate broadcast failure");
    }

    @Test
    public void testGetResult_allSucceeded_allNodesFail_throwsException() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addError(NODE_1, new RuntimeException("Error 1"));
      aggregator.addError(NODE_2, new RuntimeException("Error 2"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "Should throw JedisBroadcastException when all nodes fail");
    }
  }

  // ==================== getResult() - DEFAULT Policy Tests ====================
  // Nested class for comprehensive DEFAULT policy testing

  @Nested
  class DefaultPolicyTests {

    @Test
    public void testGetResult_default_allNodesSucceed_returnsResult() {
      MultiNodeResultAggregator<ArrayList<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(NODE_1, new ArrayList<>(Collections.singletonList("a")));
      aggregator.addSuccess(NODE_2, new ArrayList<>(Collections.singletonList("b")));

      ArrayList<String> result = aggregator.getResult();
      assertEquals(new ArrayList<>(Arrays.asList("a", "b")), result,
        "Should return result when all nodes succeed");
    }

    @Test
    public void testGetResult_default_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<ArrayList<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(NODE_1, new ArrayList<>(Collections.singletonList("a")));
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "DEFAULT policy should throw when any node fails");
    }
  }

  // ==================== getResult() - AGG_SUM Policy Tests ====================
  @Nested
  class AggSumPolicyTests {

    @Test
    public void testGetResult_aggSum_sumsLongResults() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_SUM);

      aggregator.addSuccess(NODE_1, 5L);
      aggregator.addSuccess(NODE_2, 10L);
      aggregator.addSuccess(NODE_3, 15L);

      Long result = aggregator.getResult();
      assertEquals(30L, result, "Should sum all Long results");
    }

    @Test
    public void testGetResult_aggSum_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_SUM);

      aggregator.addSuccess(NODE_1, 5L);
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "AGG_SUM should throw when any node fails");
    }

  }
  // ==================== getResult() - AGG_MIN Policy Tests ====================

  @Nested
  class AggMinPolicyTests {

    @Test
    public void testGetResult_aggMin_returnsMinimumValue() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_MIN);

      aggregator.addSuccess(NODE_1, 100L);
      aggregator.addSuccess(NODE_2, 50L);
      aggregator.addSuccess(NODE_3, 75L);

      Long result = aggregator.getResult();
      assertEquals(50L, result, "Should return minimum Long value");
    }

    @Test
    public void testGetResult_aggMin_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_MIN);

      aggregator.addSuccess(NODE_1, 100L);
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "AGG_MIN should throw when any node fails");
    }
  }

  // ==================== getResult() - AGG_MAX Policy Tests ====================

  @Nested
  class AggMaxPolicyTests {

    @Test
    public void testGetResult_aggMax_returnsMaximumValue() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_MAX);

      aggregator.addSuccess(NODE_1, 25L);
      aggregator.addSuccess(NODE_2, 100L);
      aggregator.addSuccess(NODE_3, 75L);

      Long result = aggregator.getResult();
      assertEquals(100L, result, "Should return maximum Long value");
    }

    @Test
    public void testGetResult_aggMax_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_MAX);

      aggregator.addSuccess(NODE_1, 100L);
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "AGG_MAX should throw when any node fails");
    }
  }

  // ==================== getResult() - AGG_LOGICAL_AND Policy Tests ====================

  @Nested
  class AggLogicalAndPolicyTests {

    @Test
    public void testGetResult_aggLogicalAnd_allTrue_returnsTrue() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_LOGICAL_AND);

      aggregator.addSuccess(NODE_1, 1L);
      aggregator.addSuccess(NODE_2, 1L);
      aggregator.addSuccess(NODE_3, 1L);

      Long result = aggregator.getResult();
      assertEquals(1L, result, "Logical AND of all true (1L) should be 1L");
    }

    @Test
    public void testGetResult_aggLogicalAnd_oneFalse_returnsFalse() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_LOGICAL_AND);

      aggregator.addSuccess(NODE_1, 1L);
      aggregator.addSuccess(NODE_2, 0L);
      aggregator.addSuccess(NODE_3, 1L);

      Long result = aggregator.getResult();
      assertEquals(0L, result, "Logical AND with one false (0L) should be 0L");
    }

    @Test
    public void testGetResult_aggLogicalAnd_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_LOGICAL_AND);

      aggregator.addSuccess(NODE_1, 1L);
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "AGG_LOGICAL_AND should throw when any node fails");
    }
  }

  // ==================== getResult() - AGG_LOGICAL_OR Policy Tests ====================

  @Nested
  class AggLogicalOrPolicyTests {

    @Test
    public void testGetResult_aggLogicalOr_allFalse_returnsFalse() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_LOGICAL_OR);

      aggregator.addSuccess(NODE_1, 0L);
      aggregator.addSuccess(NODE_2, 0L);
      aggregator.addSuccess(NODE_3, 0L);

      Long result = aggregator.getResult();
      assertEquals(0L, result, "Logical OR of all false (0L) should be 0L");
    }

    @Test
    public void testGetResult_aggLogicalOr_oneTrue_returnsTrue() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_LOGICAL_OR);

      aggregator.addSuccess(NODE_1, 0L);
      aggregator.addSuccess(NODE_2, 1L);
      aggregator.addSuccess(NODE_3, 0L);

      Long result = aggregator.getResult();
      assertEquals(1L, result, "Logical OR with one true (1L) should be 1L");
    }

    @Test
    public void testGetResult_aggLogicalOr_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_LOGICAL_OR);

      aggregator.addSuccess(NODE_1, 1L);
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "AGG_LOGICAL_OR should throw when any node fails");
    }
  }

  // ==================== getResult() - SPECIAL Policy Tests ====================

  @Nested
  class SpecialPolicyTests {

    @Test
    public void testGetResult_special_returnsFirstResult() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.SPECIAL);

      aggregator.addSuccess(NODE_1, "first");
      aggregator.addSuccess(NODE_2, "second");

      String result = aggregator.getResult();
      assertEquals("first", result, "SPECIAL policy should return existing/first result");
    }

    @Test
    public void testGetResult_special_oneNodeFails_throwsException() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.SPECIAL);

      aggregator.addSuccess(NODE_1, "first");
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "SPECIAL policy should throw when any node fails");
    }
  }

  // ==================== Edge Case Tests ====================

  @Nested
  class EdgeCaseTests {

    @Test
    public void testGetResult_noResultsAdded_returnsNull() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      String result = aggregator.getResult();
      assertNull(result, "Should return null when no results have been added");
    }

    @Test
    public void testGetResult_singleSuccess_returnsResult() {
      MultiNodeResultAggregator<List<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(NODE_1, Collections.singletonList("S1"));

      List<String> result = aggregator.getResult();
      assertThat(result, contains("S1"));
    }

    @Test
    public void testGetResult_singleError_throwsException() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addError(NODE_1, new RuntimeException("Single error"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      assertEquals(1, ex.getReplies().size(), "Should have one error reply");
    }

    @Test
    public void testAddSuccess_nullResult_handledCorrectly() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(NODE_1, null);

      String result = aggregator.getResult();
      assertNull(result, "Should handle null result correctly");
    }

    @Test
    public void testAddSuccess_nullResult_followedByRealResult() {
      MultiNodeResultAggregator<List<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(NODE_1, null);
      aggregator.addSuccess(NODE_2, Collections.singletonList("S1"));

      assertThat(aggregator.getResult(), contains("S1"));
    }

    @Test
    public void testAddSuccess_realResult_followedByNull() {
      MultiNodeResultAggregator<List<String>> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.DEFAULT);

      aggregator.addSuccess(NODE_1, Collections.singletonList("S1"));
      aggregator.addSuccess(NODE_2, null);

      List<String> result = aggregator.getResult();
      assertThat(result, contains("S1"));
    }

    @Test
    public void testOneSucceeded_errorThenSuccess_returnsSuccess() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      // First add error, then success
      aggregator.addError(NODE_1, new RuntimeException("Error"));
      aggregator.addSuccess(NODE_2, "success");

      String result = aggregator.getResult();
      assertEquals("success", result,
        "ONE_SUCCEEDED should return success even if error came first");
    }

    @Test
    public void testOneSucceeded_successThenError_returnsSuccess() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      // First add success, then error
      aggregator.addSuccess(NODE_1, "success");
      aggregator.addError(NODE_2, new RuntimeException("Error"));

      String result = aggregator.getResult();
      assertEquals("success", result,
        "ONE_SUCCEEDED should return success even if error came after");
    }

    @Test
    public void testBroadcastException_containsCorrectMessage() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addError(NODE_1, new RuntimeException("Connection timeout"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      assertTrue(ex.getMessage().contains("failed"),
        "JedisBroadcastException should have meaningful message");
    }

    @Test
    public void testBroadcastException_containsMixedReplies() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addSuccess(NODE_1, "OK");
      aggregator.addSuccess(NODE_2, "OK");
      aggregator.addError(NODE_3, new RuntimeException("Failed"));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(3, replies.size(), "Should contain all replies (successes and errors)");
      assertEquals("OK", replies.get(NODE_1));
      assertEquals("OK", replies.get(NODE_2));
      assertTrue(replies.get(NODE_3) instanceof RuntimeException);
    }

    @Test
    public void testAggregation_multipleSuccessesBeforeError() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_SUM);

      aggregator.addSuccess(NODE_1, 10L);
      aggregator.addSuccess(NODE_2, 20L);
      aggregator.addError(NODE_3, new RuntimeException("Error"));

      // Should throw because AGG_SUM is not ONE_SUCCEEDED
      assertThrows(JedisBroadcastException.class, () -> aggregator.getResult(),
        "AGG_SUM should throw on any error, even if successes were aggregated");
    }

    @Test
    public void testOneSucceeded_aggregatesMultipleSuccesses() {
      // With ONE_SUCCEEDED, aggregation still happens using the policy's aggregation behavior
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ONE_SUCCEEDED);

      aggregator.addSuccess(NODE_1, "first");
      aggregator.addError(NODE_2, new RuntimeException("Error"));
      aggregator.addSuccess(NODE_3, "second");

      String result = aggregator.getResult();
      // ONE_SUCCEEDED returns existing value (first successful result)
      assertEquals("first", result, "ONE_SUCCEEDED should return the first successful result");
    }

    @Test
    public void testMixedAddSuccessMethods() {
      MultiNodeResultAggregator<Long> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.AGG_SUM);

      // Mix both addSuccess methods
      aggregator.addSuccess(NODE_1, 10L);
      aggregator.addSuccess(20L); // Without node
      aggregator.addSuccess(NODE_2, 30L);

      Long result = aggregator.getResult();
      assertEquals(60L, result, "Should aggregate results from both addSuccess methods");
    }

    @Test
    public void testMixedAddErrorMethods() {
      MultiNodeResultAggregator<String> aggregator = new MultiNodeResultAggregator<>(
          ResponsePolicy.ALL_SUCCEEDED);

      aggregator.addError(NODE_1, new RuntimeException("Error 1"));
      aggregator.addError(new RuntimeException("Error 2")); // Will use unknown:0
      aggregator.addError(new JedisClusterOperationException("Error 3", NODE_2));

      JedisBroadcastException ex = assertThrows(JedisBroadcastException.class,
        () -> aggregator.getResult());

      Map<HostAndPort, Object> replies = ex.getReplies();
      assertEquals(3, replies.size(), "Should have three error entries");
      assertTrue(replies.containsKey(NODE_1));
      assertTrue(replies.containsKey(UNKNOWN_NODE));
      assertTrue(replies.containsKey(NODE_2));
    }
  }
}
