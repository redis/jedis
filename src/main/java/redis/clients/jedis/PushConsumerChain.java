package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.RedisInputStream;

import java.util.List;

/**
 * Process push messages and decide weather to propagate them to the caller or consume them.
 * <p>
 * The {@link PushConsumerChain} is used by the
 * {@link Protocol#read(RedisInputStream, PushConsumerChain)} method to process out of band push
 * messages while reading the regular command replies.
 * </p>
 * @see Protocol#read(RedisInputStream, PushConsumerChain)
 */
@Experimental
public interface PushConsumerChain {

  /**
   * Process the push message
   * <p>
   * Returning {@code null} means the message was consumed.
   * </p>
   * @param message message to process
   * @return message, or null if the message was consumed
   */
  PushMessage process(PushMessage message);

  /**
   * Return a list of consumers in the chain.
   * @return list of consumers in the chain
   */
  List<PushConsumer> getConsumers();
}