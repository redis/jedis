package redis.clients.jedis.csc;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.PushConsumer;
import redis.clients.jedis.PushConsumerContext;
import redis.clients.jedis.PushMessageTypes;
import redis.clients.jedis.annots.Internal;

/**
 * {@link PushConsumer} that drops RESP3 {@code invalidate} push messages after evicting the
 * referenced Redis keys from the supplied {@link Cache}.
 * <p>
 * Internal API: exposed only so tests and benchmarks outside this package can construct it
 * directly. Not part of the public surface and subject to change.
 */
@Internal
public class PushInvalidateConsumer implements PushConsumer {

  private final Cache cache;

  public PushInvalidateConsumer(Cache cache) {
    this.cache = cache;
  }

  @Override
  public PushConsumerContext handle(PushConsumerContext context) {
    if (Arrays.equals(PushMessageTypes.INVALIDATE_BYTES, context.getMessage().getType())) {
      cache.deleteByRedisKeys((List) context.getMessage().getContent().get(1));
      context.drop();
    }
    return context;
  }
}
