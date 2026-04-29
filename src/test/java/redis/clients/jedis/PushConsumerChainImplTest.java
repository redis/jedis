package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit tests for PushConsumerChainImpl that verify the push message processing logic.
 * <p>
 * Tests validate the three main scenarios:
 * <ul>
 * <li>Message propagation when context.propagate() is called</li>
 * <li>Message dropping when context.drop() is called</li>
 * <li>Silent consumption when neither propagate() nor drop() is called</li>
 * </ul>
 * </p>
 */
public class PushConsumerChainImplTest {

  /**
   * Test that when a consumer calls context.propagate(), the message is returned to the caller and
   * subsequent consumers in the chain are skipped.
   */
  @Test
  public void testPropagateReturnsMessageAndSkipsRemainingConsumers() {
    // Track which consumers were invoked
    List<String> invocations = new ArrayList<>();

    // Consumer 1: just logs and passes through
    PushConsumer consumer1 = context -> {
      invocations.add("consumer1");
      return context;
    };

    // Consumer 2: propagates the message
    PushConsumer consumer2 = context -> {
      invocations.add("consumer2");
      context.propagate();
      return context;
    };

    // Consumer 3: should NOT be invoked because consumer2 propagated
    PushConsumer consumer3 = context -> {
      invocations.add("consumer3");
      return context;
    };

    // Create chain with all three consumers
    PushConsumerChainImpl chain = PushConsumerChainImpl.of(consumer1, consumer2, consumer3);

    // Create a test push message
    List<Object> content = new ArrayList<>();
    content.add(SafeEncoder.encode("test-type"));
    content.add(SafeEncoder.encode("arg1"));
    PushMessage message = new PushMessage(content);

    // Process the message
    PushMessage result = chain.process(message);

    // Verify the message was propagated (returned, not null)
    assertNotNull(result, "Message should be propagated (not null)");
    assertEquals("test-type", result.getType(), "Propagated message should have correct type");

    // Verify only consumer1 and consumer2 were invoked, consumer3 was skipped
    assertEquals(2, invocations.size(), "Only first two consumers should be invoked");
    assertEquals("consumer1", invocations.get(0));
    assertEquals("consumer2", invocations.get(1));
  }

  /**
   * Test that when a consumer calls context.drop(), the message is consumed (returns null) and
   * subsequent consumers in the chain are skipped.
   */
  @Test
  public void testDropConsumesMessageAndSkipsRemainingConsumers() {
    // Track which consumers were invoked
    List<String> invocations = new ArrayList<>();

    // Consumer 1: just logs and passes through
    PushConsumer consumer1 = context -> {
      invocations.add("consumer1");
      return context;
    };

    // Consumer 2: drops the message
    PushConsumer consumer2 = context -> {
      invocations.add("consumer2");
      context.drop();
      return context;
    };

    // Consumer 3: should NOT be invoked because consumer2 dropped
    PushConsumer consumer3 = context -> {
      invocations.add("consumer3");
      return context;
    };

    // Create chain with all three consumers
    PushConsumerChainImpl chain = PushConsumerChainImpl.of(consumer1, consumer2, consumer3);

    // Create a test push message
    List<Object> content = new ArrayList<>();
    content.add(SafeEncoder.encode("test-type"));
    content.add(SafeEncoder.encode("arg1"));
    PushMessage message = new PushMessage(content);

    // Process the message
    PushMessage result = chain.process(message);

    // Verify the message was dropped (null)
    assertNull(result, "Message should be dropped (null)");

    // Verify only consumer1 and consumer2 were invoked, consumer3 was skipped
    assertEquals(2, invocations.size(), "Only first two consumers should be invoked");
    assertEquals("consumer1", invocations.get(0));
    assertEquals("consumer2", invocations.get(1));
  }

  /**
   * Test that when no consumer calls propagate() or drop(), the message is silently consumed
   * (returns null) after all consumers have processed it.
   */
  @Test
  public void testSilentConsumptionWhenNoPropagateOrDrop() {
    // Track which consumers were invoked
    List<String> invocations = new ArrayList<>();

    // Consumer 1: just inspects and passes through
    PushConsumer consumer1 = context -> {
      invocations.add("consumer1");
      // Do nothing - just inspect
      return context;
    };

    // Consumer 2: just inspects and passes through
    PushConsumer consumer2 = context -> {
      invocations.add("consumer2");
      // Do nothing - just inspect
      return context;
    };

    // Consumer 3: just inspects and passes through
    PushConsumer consumer3 = context -> {
      invocations.add("consumer3");
      // Do nothing - just inspect
      return context;
    };

    // Create chain with all three consumers
    PushConsumerChainImpl chain = PushConsumerChainImpl.of(consumer1, consumer2, consumer3);

    // Create a test push message
    List<Object> content = new ArrayList<>();
    content.add(SafeEncoder.encode("test-type"));
    content.add(SafeEncoder.encode("arg1"));
    PushMessage message = new PushMessage(content);

    // Process the message
    PushMessage result = chain.process(message);

    // Verify the message was silently consumed (null)
    assertNull(result, "Message should be silently consumed (null) when no action is taken");

    // Verify all three consumers were invoked
    assertEquals(3, invocations.size(), "All three consumers should be invoked");
    assertEquals("consumer1", invocations.get(0));
    assertEquals("consumer2", invocations.get(1));
    assertEquals("consumer3", invocations.get(2));
  }

  /**
   * Test PUBSUB_CONSUMER propagates pub/sub related messages.
   */
  @Test
  public void testPubSubConsumerPropagatesPubSubMessages() {
    // Create chain with only PUBSUB_CONSUMER
    PushConsumerChainImpl chain = PushConsumerChainImpl.of(PushConsumerChainImpl.PUBSUB_CONSUMER);

    // Test pub/sub message types that should be propagated
    String[] pubSubTypes = { "message", "pmessage", "smessage", "subscribe", "psubscribe",
        "ssubscribe", "unsubscribe", "punsubscribe", "sunsubscribe" };

    for (String type : pubSubTypes) {
      List<Object> content = new ArrayList<>();
      content.add(SafeEncoder.encode(type));
      content.add(SafeEncoder.encode("channel"));
      PushMessage message = new PushMessage(content);

      PushMessage result = chain.process(message);

      assertNotNull(result, "Pub/sub message type '" + type + "' should be propagated");
      assertEquals(type, result.getType());
    }
  }

  /**
   * Test PUBSUB_CONSUMER does not propagate non-pub/sub messages.
   */
  @Test
  public void testPubSubConsumerDoesNotPropagateNonPubSubMessages() {
    // Create chain with only PUBSUB_CONSUMER
    PushConsumerChainImpl chain = PushConsumerChainImpl.of(PushConsumerChainImpl.PUBSUB_CONSUMER);

    // Test non-pub/sub message types that should NOT be propagated
    String[] nonPubSubTypes = { "invalidate", "arbitrary", "some-other-type" };

    for (String type : nonPubSubTypes) {
      List<Object> content = new ArrayList<>();
      content.add(SafeEncoder.encode(type));
      content.add(SafeEncoder.encode("data"));
      PushMessage message = new PushMessage(content);

      PushMessage result = chain.process(message);

      assertNull(result, "Non-pub/sub message type '" + type + "' should be consumed (null)");
    }
  }

  /**
   * Test PROPAGATE_ALL_CONSUMER propagates all messages.
   */
  @Test
  public void testPropagateAllConsumerPropagatesAllMessages() {
    // Create chain with PROPAGATE_ALL_CONSUMER
    PushConsumerChain chain = PushConsumerChainImpl.PROPAGATE_ALL_CONSUMER_CHAIN;

    // Test various message types - all should be propagated
    String[] types = { "invalidate", "message", "arbitrary", "custom-type" };

    for (String type : types) {
      List<Object> content = new ArrayList<>();
      content.add(SafeEncoder.encode(type));
      content.add(SafeEncoder.encode("data"));
      PushMessage message = new PushMessage(content);

      PushMessage result = chain.process(message);

      assertNotNull(result, "PROPAGATE_ALL_CONSUMER should propagate message type '" + type + "'");
      assertEquals(type, result.getType());
    }
  }

  /**
   * Test that propagate takes precedence over drop if both are called.
   */
  @Test
  public void testPropagateTakesPrecedenceOverDrop() {
    // Consumer that calls both drop() and propagate()
    PushConsumer consumer = context -> {
      context.drop(); // Call drop first
      context.propagate(); // Then propagate
      return context;
    };

    PushConsumerChainImpl chain = PushConsumerChainImpl.of(consumer);

    List<Object> content = new ArrayList<>();
    content.add(SafeEncoder.encode("test-type"));
    PushMessage message = new PushMessage(content);

    PushMessage result = chain.process(message);

    // Propagate is checked first in the implementation, so it should take precedence
    assertNotNull(result, "Message should be propagated when both propagate and drop are called");
  }
}
