package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

public class HimportConnectionStateTest {

  @Test
  public void drainIsEmptyWhenNothingQueued() {
    HimportConnectionState state = new HimportConnectionState();
    assertTrue(state.drainDiscardable().isEmpty());
  }

  @Test
  public void drainReturnsOnlyPreparedFieldsets() {
    HimportConnectionState state = new HimportConnectionState();
    state.markPrepared("a");
    state.markForDiscard("a");
    state.markForDiscard("never-prepared");

    assertEquals(Collections.singletonList("a"), state.drainDiscardable());
    assertFalse(state.isPrepared("a"));
    // fully drained: nothing left for the next command
    assertTrue(state.drainDiscardable().isEmpty());
  }

  @Test
  public void markAfterDrainIsPickedUpAgain() {
    HimportConnectionState state = new HimportConnectionState();
    state.markPrepared("a");
    state.markForDiscard("a");
    state.drainDiscardable();

    state.markPrepared("b");
    state.markForDiscard("b");
    assertEquals(Collections.singletonList("b"), state.drainDiscardable());
  }

  @Test
  public void resetDropsAllState() {
    HimportConnectionState state = new HimportConnectionState();
    state.markPrepared("a");
    state.markForDiscard("a");
    state.reset();

    assertFalse(state.isPrepared("a"));
    assertTrue(state.drainDiscardable().isEmpty());
  }
}