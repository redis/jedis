package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

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
@Experimental
public final class PushConsumerChainImpl implements PushConsumerChain {
  /**
   * PushConsumer that marks all push events to be propagated to the caller.
   */
  static final PushConsumer PROPAGATE_ALL_CONSUMER = new PushConsumer() {
    @Override
    public PushConsumerContext handle(PushConsumerContext context) {
      context.propagate();

      return context;
    }
  };

  static final PushConsumerChain PROPAGATE_ALL_CONSUMER_CHAIN = of(PROPAGATE_ALL_CONSUMER);

  /**
   * PushConsumer that marks pub/sub related events to be propagated to the caller
   */
  public static final PushConsumer PUBSUB_CONSUMER = new PushConsumer() {
    final Set<String> pubSubCommands = new HashSet<>();

    {
      // Pub/Sub message types
      pubSubCommands.add(PushMessageTypes.MESSAGE);
      pubSubCommands.add(PushMessageTypes.PMESSAGE);
      pubSubCommands.add(PushMessageTypes.SMESSAGE);

      // Pub/Sub subscription confirmations
      pubSubCommands.add(PushMessageTypes.SUBSCRIBE);
      pubSubCommands.add(PushMessageTypes.PSUBSCRIBE);
      pubSubCommands.add(PushMessageTypes.SSUBSCRIBE);

      // Pub/Sub unsubscription confirmations
      pubSubCommands.add(PushMessageTypes.UNSUBSCRIBE);
      pubSubCommands.add(PushMessageTypes.PUNSUBSCRIBE);
      pubSubCommands.add(PushMessageTypes.SUNSUBSCRIBE);
    }

    @Override
    public PushConsumerContext handle(PushConsumerContext context) {
      if (pubSubCommands.contains(context.getMessage().getType())) {
        // Ensure pub/sub events are propagated to application
        context.propagate();
      }

      return context;
    }
  };

  private final List<PushConsumer> consumers;

  /**
   * Create a chain with the specified consumers.
   * @param consumers The consumers to add to the chain
   */
  PushConsumerChainImpl(PushConsumer... consumers) {
    this.consumers = new ArrayList<>(Arrays.asList(consumers));
  }

  /**
   * Create a chain with the specified consumers.
   * @param consumers The consumers to add to the chain
   * @return A new consumer chain with the specified consumers
   */
  public static PushConsumerChainImpl of(PushConsumer... consumers) {
    return new PushConsumerChainImpl(consumers);
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
   * Return an unmodifiable list of consumers in the chain.
   */
  public List<PushConsumer> getConsumers() {

    return Collections.unmodifiableList(consumers);
  }

  public PushMessage process(PushMessage message) {

    PushConsumerContext context = new PushConsumerContext(message);

    for (PushConsumer consumer : consumers) {
      context = consumer.handle(context);

      // propagate → return to caller and skip the rest of consumers
      if (context.shouldPropagate()) {
        return context.getMessage();
      }

      // drop → consume and skip rest of consumers
      if (context.shouldDrop()) {
        return null;
      }
    }

    // end of chain → default: consume
    return null;
  }

}