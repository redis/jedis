package redis.clients.jedis;

import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.util.SafeEncoder;

/**
 * PushConsumer that propagates pub/sub related events only while {@code activeSubscription} reports
 * {@code true} (i.e. a pub/sub read loop is driving the connection). A pub/sub push received
 * outside an active subscription — e.g. a message the server delivered after the unsubscribe
 * confirmation — must not be returned as a regular command's reply; it is logged and left to the
 * rest of the chain (consumed by default at end of chain).
 */
final class PubSubPushConsumer implements PushConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(PubSubPushConsumer.class);

  private final BooleanSupplier activeSubscription;

  /**
   * @param activeSubscription supplies whether the owning connection currently runs a pub/sub read
   *          loop
   */
  PubSubPushConsumer(BooleanSupplier activeSubscription) {
    this.activeSubscription = activeSubscription;
  }

  @Override
  public PushConsumerContext handle(PushConsumerContext context) {
    if (PushConsumerChainImpl.isPubSubType(context.getMessage().getType())) {
      if (activeSubscription.getAsBoolean()) {
        context.propagate();
      } else if (LOG.isDebugEnabled()) {
        LOG.debug("Ignoring pub/sub push message of type '{}' received without an active "
            + "subscription.",
          SafeEncoder.encode(context.getMessage().getType()));
      }
    }
    return context;
  }
}