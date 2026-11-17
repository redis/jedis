package redis.clients.jedis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.KeyValue;

/**
 * Operations on raw protocol replies: the values {@link Protocol#read} returns before a
 * {@link Builder} converts them.
 */
@Internal
public final class ProtocolReplies {

  private ProtocolReplies() {
  }

  /**
   * Returns an independent copy of a raw reply. Byte arrays, lists, key-value pairs and error
   * replies are copied; numbers, booleans and {@code null} are shared. The empty-map marker is kept
   * as is, so builders still tell an empty map from an empty array.
   * @param reply a raw reply from {@link Protocol#read}
   * @return a copy that shares no mutable state with {@code reply}
   * @throws IllegalArgumentException if {@code reply}, or a value nested in it, is not a type the
   *           protocol reader produces
   */
  public static Object copy(Object reply) {
    if (reply instanceof byte[]) {
      return ((byte[]) reply).clone();
    }
    if (isImmutable(reply)) {
      return reply;
    }
    if (reply instanceof List) {
      return copyList((List<?>) reply);
    }
    if (reply instanceof KeyValue) {
      return copyKeyValue((KeyValue<?, ?>) reply);
    }
    if (reply instanceof JedisDataException) {
      return copyError((JedisDataException) reply);
    }
    throw new IllegalArgumentException("Unsupported protocol reply type: " + reply.getClass());
  }

  // The empty-map marker is immutable, and builders tell an empty map from an empty array by it.
  private static boolean isImmutable(Object reply) {
    return reply == null || reply == Protocol.PROTOCOL_EMPTY_MAP || reply instanceof Long
        || reply instanceof Double || reply instanceof Boolean || reply instanceof BigInteger;
  }

  private static List<Object> copyList(List<?> values) {
    List<Object> copies = new ArrayList<>(values.size());
    for (Object value : values) {
      copies.add(copy(value));
    }
    return copies;
  }

  private static KeyValue<Object, Object> copyKeyValue(KeyValue<?, ?> pair) {
    return new KeyValue<>(copy(pair.getKey()), copy(pair.getValue()));
  }

  // Rebuilt from the message so MOVED, ASK and other errors keep their subtype.
  private static JedisDataException copyError(JedisDataException error) {
    JedisDataException copy = Protocol.createError(error.getMessage());
    copy.setStackTrace(error.getStackTrace());
    return copy;
  }
}
