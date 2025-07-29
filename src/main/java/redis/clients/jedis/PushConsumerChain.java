package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.annots.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A chain of PushHandlers that processes events in order.
 * <p>
 * Uses a context object for tracking the processed state.
 * </p>
 */
@Internal
public final class PushConsumerChain implements PushConsumer {
  /**
   * Handler that allows all push events to be propagated to the client.
   */
  public static final PushConsumer PROPAGATE_ALL_HANDLER = (context) -> {
    // mark as not-processed, always propagate
    context.setForwardToClient(true);
  };
  /**
   * Handler that allows only pub/sub related events to be propagated to the client
   * <p>
   * Marks non-pub/sub events as processed, preventing their propagation.
   * </p>
   */
  public static final PushConsumer PUBSUB_ONLY_HANDLER = new PushConsumer() {
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
        context.setForwardToClient(true);
      }
    }
  };
  private static final Logger log = LoggerFactory.getLogger(PushConsumerChain.class);
  private final List<PushConsumer> consumers;

  /**
   * Create a new empty handler chain.
   */
  public PushConsumerChain() {
    this.consumers = new ArrayList<>();
  }

  /**
   * Create a chain with the specified handlers.
   * @param consumers The handlers to add to the chain
   */
  public PushConsumerChain(PushConsumer... consumers) {
    this.consumers = new ArrayList<>(Arrays.asList(consumers));
  }

  /**
   * Create a chain with the specified handlers.
   * @param handlers The handlers to add to the chain
   * @return A new handler chain with the specified handlers
   */
  public static PushConsumerChain of(PushConsumer... handlers) {
    return new PushConsumerChain(handlers);
  }

  /**
   * Add a handler to the end of the chain.
   * @param handler The handler to add
   * @return this chain for method chaining
   */
  public PushConsumerChain add(PushConsumer handler) {
    if (handler != null) {
      consumers.add(handler);
    }
    return this;
  }

  /**
   * Insert a handler at the specified position.
   * @param index The position to insert at (0-based)
   * @param handler The handler to insert
   * @return this chain for method chaining
   */
  public PushConsumerChain insert(int index, PushConsumer handler) {
    if (handler != null) {
      consumers.add(index, handler);
    }
    return this;
  }

  /**
   * Remove a handler from the chain.
   * @param handler The handler to remove
   * @return true if the handler was removed
   */
  public boolean remove(PushConsumer handler) {
    return consumers.remove(handler);
  }

  /**
   * Get the number of handlers in the chain.
   * @return The number of handlers
   */
  public int size() {
    return consumers.size();
  }

  /**
   * Clear all handlers from the chain.
   */
  public void clear() {
    consumers.clear();
  }

  @Override
  public void accept(PushConsumerContext context) {
    if (consumers.isEmpty()) {
      return;
    }

    for (PushConsumer handler : consumers) {
      handler.accept(context);
    }
  }

}