
package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
   * PushConsumer that marks pub/sub related events to be propagated to the caller.
   * <p>
   * NOTE: If a new pub/sub push type is added to {@link PushMessageTypes}, the {@code switch} in
   * {@link #isPubSubType(byte[])} must be updated. {@code PushConsumerChainImplTest} discovers
   * pub/sub constants reflectively and will fail until the new type is handled here.
   */
  public static final PushConsumer PUBSUB_CONSUMER = context -> {
    if (isPubSubType(context.getMessage().getType())) {
      context.propagate();
    }
    return context;
  };

  /**
   * Returns {@code true} iff {@code t} is one of the pub/sub push message types declared in
   * {@link PushMessageTypes}.
   * <p>
   * Dispatch on length first — tableswitch over the dense range 7..12. {@code (length, firstByte)}
   * uniquely identifies each of the 9 pub/sub types, so each bucket needs at most two intrinsified
   * {@link Arrays#equals(byte[], byte[])} calls. Zero allocation.
   * @param t the message type byte array, may be null
   * @return true if t is a pub/sub type, false if t is null or not a pub/sub type
   */
  static boolean isPubSubType(byte[] t) {
    if (t == null) {
      return false;
    }
    switch (t.length) {
      case 7:
        return Arrays.equals(t, PushMessageTypes.MESSAGE_BYTES);
      case 8:
        return Arrays.equals(t, PushMessageTypes.PMESSAGE_BYTES)
            || Arrays.equals(t, PushMessageTypes.SMESSAGE_BYTES);
      case 9:
        return Arrays.equals(t, PushMessageTypes.SUBSCRIBE_BYTES);
      case 10:
        return Arrays.equals(t, PushMessageTypes.PSUBSCRIBE_BYTES)
            || Arrays.equals(t, PushMessageTypes.SSUBSCRIBE_BYTES);
      case 11:
        return Arrays.equals(t, PushMessageTypes.UNSUBSCRIBE_BYTES);
      case 12:
        return Arrays.equals(t, PushMessageTypes.PUNSUBSCRIBE_BYTES)
            || Arrays.equals(t, PushMessageTypes.SUNSUBSCRIBE_BYTES);
      default:
        return false;
    }
  }

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