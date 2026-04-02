package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A chain of PushConsumers that processes events in order.
 * <p>
 * Uses a {@link PushConsumerContext} object for tracking the processed state.
 * </p>
 */
@Internal
public final class PushConsumerChain implements PushConsumer {
  /**
   * PushConsumer that marks all push events to be propagated to the caller.
   */
  public static final PushConsumer PROPAGATE_ALL_CONSUMER = (context) -> {
    // mark as not-processed, always propagate
    context.setReturnToCaller(true);
  };
  /**
   * PushConsumer that marks only pub/sub related events to be propagated to the caller
   * <p>
   * Marks non-pub/sub events as returnToCaller=false, preventing their propagation.
   * </p>
   */
  public static final PushConsumer PUBSUB_ONLY_CONSUMER = new PushConsumer() {
    final Set<String> pubSubCommands = new HashSet<>();

    {
      pubSubCommands.add("message");
      pubSubCommands.add("pmessage");
      pubSubCommands.add("smessage");
      pubSubCommands.add("subscribe");
      pubSubCommands.add("ssubscribe");
      pubSubCommands.add("psubscribe");
      pubSubCommands.add("unsubscribe");
      pubSubCommands.add("sunsubscribe");
      pubSubCommands.add("punsubscribe");
    }

    @Override
    public void accept(PushConsumerContext context) {
      if (pubSubCommands.contains(context.getMessage().getType())) {
        // Ensure pub/sub events are propagated to application
        context.setReturnToCaller(true);
      }
    }
  };

  private final List<PushConsumer> consumers;

  /**
   * Create a new empty consumer chain.
   */
  public PushConsumerChain() {
    this.consumers = new ArrayList<>();
  }

  /**
   * Create a chain with the specified consumers.
   * @param consumers The consumers to add to the chain
   */
  public PushConsumerChain(PushConsumer... consumers) {
    this.consumers = new ArrayList<>(Arrays.asList(consumers));
  }

  /**
   * Create a chain with the specified consumers.
   * @param consumers The consumers to add to the chain
   * @return A new consumer chain with the specified consumers
   */
  public static PushConsumerChain of(PushConsumer... consumers) {
    return new PushConsumerChain(consumers);
  }

  /**
   * Add a consumer to the end of the chain.
   * @param consumer The consumer to add
   * @return this chain for method chaining
   */
  public PushConsumerChain add(PushConsumer consumer) {
    if (consumer != null) {
      consumers.add(consumer);
    }
    return this;
  }

  /**
   * Get the number of consumers in the chain.
   * @return The number of consumers
   */
  public int size() {
    return consumers.size();
  }

  /**
   * Clear all consumers from the chain.
   */
  public void clear() {
    consumers.clear();
  }

  /**
   * Return an unmodifiable list of consumers in the chain.
   * @return
   */
  public List<PushConsumer> getConsumers() {
    return Collections.unmodifiableList(consumers);
  }

  /**
   * Process a push message by passing it to all consumers in the chain.
   * @param context The context of the push message
   */
  @Override
  public void accept(PushConsumerContext context) {
    if (consumers.isEmpty()) {
      return;
    }

    for (PushConsumer consumer : consumers) {
      consumer.accept(context);
    }
  }

}