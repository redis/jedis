package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.mcf.InitializationPolicy.Decision;
import redis.clients.jedis.mcf.InitializationPolicy.InitializationContext;

/**
 * Unit tests for {@link InitializationPolicy} implementations.
 * <p>
 * Tests verify the decision logic for different combinations of available, failed, and pending
 * connections for each built-in policy:
 * <ul>
 * <li>{@link InitializationPolicy.BuiltIn#ALL_AVAILABLE}</li>
 * <li>{@link InitializationPolicy.BuiltIn#MAJORITY_AVAILABLE}</li>
 * <li>{@link InitializationPolicy.BuiltIn#ONE_AVAILABLE}</li>
 * </ul>
 */
@DisplayName("InitializationPolicy Unit Tests")
public class InitializationPolicyTest {

  /**
   * Test implementation of InitializationContext for unit testing.
   */
  private static class TestContext implements InitializationContext {

    private final int available;
    private final int failed;
    private final int pending;

    TestContext(int available, int failed, int pending) {
      this.available = available;
      this.failed = failed;
      this.pending = pending;
    }

    @Override
    public int getAvailableConnections() {
      return available;
    }

    @Override
    public int getFailedConnections() {
      return failed;
    }

    @Override
    public int getPendingConnections() {
      return pending;
    }
  }

  @Nested
  @DisplayName("ALL_AVAILABLE Policy Tests")
  class AllAvailablePolicyTests {

    private final InitializationPolicy policy = InitializationPolicy.BuiltIn.ALL_AVAILABLE;

    @Test
    @DisplayName("Should return SUCCESS when all connections are available")
    void shouldSucceedWhenAllAvailable() {
      TestContext ctx = new TestContext(3, 0, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS when single connection is available and no others")
    void shouldSucceedWithSingleConnection() {
      TestContext ctx = new TestContext(1, 0, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS when no connections configured (0,0,0)")
    void shouldFailWithEmptyContext() {
      TestContext ctx = new TestContext(0, 0, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when any connection fails")
    void shouldFailWhenAnyConnectionFails() {
      TestContext ctx = new TestContext(2, 1, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when all connections fail")
    void shouldFailWhenAllConnectionsFail() {
      TestContext ctx = new TestContext(0, 3, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when single connection fails")
    void shouldFailWithSingleFailure() {
      TestContext ctx = new TestContext(0, 1, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL immediately when any failure even with pending")
    void shouldFailImmediatelyWithAnyFailure() {
      TestContext ctx = new TestContext(1, 1, 2);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when connections are pending")
    void shouldContinueWhenPending() {
      TestContext ctx = new TestContext(2, 0, 1);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when all connections are pending")
    void shouldContinueWhenAllPending() {
      TestContext ctx = new TestContext(0, 0, 5);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }
  }

  @Nested
  @DisplayName("MAJORITY_AVAILABLE Policy Tests")
  class MajorityAvailablePolicyTests {

    private final InitializationPolicy policy = InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE;

    @Test
    @DisplayName("Should return SUCCESS when majority is reached (3 of 5)")
    void shouldSucceedWithMajority() {
      TestContext ctx = new TestContext(3, 1, 1);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS when all are available")
    void shouldSucceedWhenAllAvailable() {
      TestContext ctx = new TestContext(5, 0, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS with exact majority (2 of 3)")
    void shouldSucceedWithExactMajority() {
      TestContext ctx = new TestContext(2, 1, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS with single connection out of one")
    void shouldSucceedWithSingleConnection() {
      TestContext ctx = new TestContext(1, 0, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS early when majority reached with pending (3 of 5)")
    void shouldSucceedEarlyWithMajority() {
      TestContext ctx = new TestContext(3, 0, 2);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS early when majority reached (2 of 3, 1 pending)")
    void shouldSucceedEarlyWithMajorityOf3() {
      TestContext ctx = new TestContext(2, 0, 1);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should handle even number of connections (3 of 4)")
    void shouldHandleEvenNumberOfConnections() {
      TestContext ctx = new TestContext(3, 1, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should handle two connections (2 of 2)")
    void shouldHandleTwoConnections() {
      TestContext ctx = new TestContext(2, 0, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS with large number of connections (51 of 100)")
    void shouldSucceedWithLargeNumberOfConnections() {
      TestContext ctx = new TestContext(51, 49, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when no connections configured (0,0,0)")
    void shouldFailWithEmptyContext() {
      TestContext ctx = new TestContext(0, 0, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when majority is impossible (2 failed of 3)")
    void shouldFailWhenMajorityImpossible() {
      TestContext ctx = new TestContext(0, 2, 1);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when all connections fail")
    void shouldFailWhenAllFail() {
      TestContext ctx = new TestContext(0, 5, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when single database fails")
    void shouldFailWithSingleDatabaseFailed() {
      TestContext ctx = new TestContext(0, 1, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when majority not reached and no pending (1 of 3)")
    void shouldFailWhenMajorityNotReachedNoPending() {
      TestContext ctx = new TestContext(1, 2, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL early when majority impossible with pending (1,3,1)")
    void shouldFailEarlyWhenMajorityImpossibleWithPending() {
      TestContext ctx = new TestContext(1, 3, 1);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL early when majority impossible (3 failed of 5, 2 pending)")
    void shouldFailEarlyWhenMajorityImpossible() {
      TestContext ctx = new TestContext(0, 3, 2);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should fail with even split (2 of 4)")
    void shouldFailWithEvenSplit() {
      TestContext ctx = new TestContext(2, 2, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should fail with one of two connections")
    void shouldFailWithOneOfTwo() {
      TestContext ctx = new TestContext(1, 1, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when majority possible but not yet reached")
    void shouldContinueWhenMajorityPossible() {
      TestContext ctx = new TestContext(1, 1, 3);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when all pending")
    void shouldContinueWhenAllPending() {
      TestContext ctx = new TestContext(0, 0, 5);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when below majority with pending (2 of 4)")
    void shouldContinueBelowMajority() {
      TestContext ctx = new TestContext(2, 0, 2);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE with one available and one pending (1,0,1)")
    void shouldContinueWithOneAvailableOnePending() {
      TestContext ctx = new TestContext(1, 0, 1);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when majority still possible (1,1,1)")
    void shouldContinueWhenMajorityStillPossible() {
      TestContext ctx = new TestContext(1, 1, 1);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }
  }

  @Nested
  @DisplayName("ONE_AVAILABLE Policy Tests")
  class OneAvailablePolicyTests {

    private final InitializationPolicy policy = InitializationPolicy.BuiltIn.ONE_AVAILABLE;

    @Test
    @DisplayName("Should return SUCCESS when one connection is available")
    void shouldSucceedWithOneAvailable() {
      TestContext ctx = new TestContext(1, 2, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS when all connections are available")
    void shouldSucceedWhenAllAvailable() {
      TestContext ctx = new TestContext(5, 0, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS early with one available and pending")
    void shouldSucceedEarlyWithOneAvailable() {
      TestContext ctx = new TestContext(1, 0, 4);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return SUCCESS with multiple available")
    void shouldSucceedWithMultipleAvailable() {
      TestContext ctx = new TestContext(3, 2, 0);
      assertEquals(Decision.SUCCESS, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when no connections configured (0,0,0)")
    void shouldFailWithEmptyContext() {
      TestContext ctx = new TestContext(0, 0, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when all connections fail")
    void shouldFailWhenAllFail() {
      TestContext ctx = new TestContext(0, 5, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return FAIL when single connection fails")
    void shouldFailWithSingleFailure() {
      TestContext ctx = new TestContext(0, 1, 0);
      assertEquals(Decision.FAIL, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when all connections are pending")
    void shouldContinueWhenAllPending() {
      TestContext ctx = new TestContext(0, 0, 5);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }

    @Test
    @DisplayName("Should return CONTINUE when some failed but some pending")
    void shouldContinueWithFailedAndPending() {
      TestContext ctx = new TestContext(0, 2, 3);
      assertEquals(Decision.CONTINUE, policy.evaluate(ctx));
    }
  }
}
