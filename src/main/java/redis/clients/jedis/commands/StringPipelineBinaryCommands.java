package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface StringPipelineBinaryCommands extends BitPipelineBinaryCommands {

  Response<String> set(byte[] key, byte[] value);

  Response<String> set(byte[] key, byte[] value, SetParams params);

  Response<byte[]> get(byte[] key);

  Response<byte[]> setGet(byte[] key, byte[] value);

  Response<byte[]> setGet(byte[] key, byte[] value, SetParams params);

  Response<byte[]> getDel(byte[] key);

  Response<byte[]> getEx(byte[] key, GetExParams params);

  Response<Long> setrange(byte[] key, long offset, byte[] value);

  Response<byte[]> getrange(byte[] key, long startOffset, long endOffset);

  /**
   * @deprecated {@link StringPipelineBinaryCommands#setGet(byte[], byte[], redis.clients.jedis.params.SetParams)}.
   */
  @Deprecated
  Response<byte[]> getSet(byte[] key, byte[] value);

  Response<Long> setnx(byte[] key, byte[] value);

  Response<String> setex(byte[] key, long seconds, byte[] value);

  Response<String> psetex(byte[] key, long milliseconds, byte[] value);

  Response<List<byte[]>> mget(byte[]... keys);

  Response<String> mset(byte[]... keysvalues);

  Response<Long> msetnx(byte[]... keysvalues);

  /**
   * Multi-set with optional condition and expiration.
   * <p>
   * Sets the respective keys to the respective values, similar to {@link #mset(byte[]...) MSET},
   * but allows conditional set (NX|XX) and expiration options via {@link SetParams}.
   * If the condition is not met for any key, no key is set.
   * <p>
   * Both MSET and MSETEX are atomic operations. This means that if multiple keys are provided,
   * another client will either see the changes for all keys at once, or no changes at all.
   * <p>
   * Options (in {@link SetParams}): NX or XX, and expiration: EX seconds | PX milliseconds |
   * EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL.
   * <p>
   * Time complexity: O(N) where N is the number of keys to set.
   * @param params condition and expiration parameters
   * @param keysvalues pairs of keys and their values, e.g. {@code msetex(params, "foo".getBytes(), "foovalue".getBytes(), "bar".getBytes(), "barvalue".getBytes())}
   * @return {@code Response<Boolean>} that is {@code true} if all keys were set, {@code false} if none were set (condition not satisfied)
   * @see #mset(byte[]...)
   * @see #msetnx(byte[]...)
   */
  Response<Boolean> msetex(SetParams params, byte[]... keysvalues);

  Response<Long> incr(byte[] key);

  Response<Long> incrBy(byte[] key, long increment);

  Response<Double> incrByFloat(byte[] key, double increment);

  Response<Long> decr(byte[] key);

  Response<Long> decrBy(byte[] key, long decrement);

  Response<Long> append(byte[] key, byte[] value);

  Response<byte[]> substr(byte[] key, int start, int end);

  Response<Long> strlen(byte[] key);

  Response<LCSMatchResult> lcs(byte[] keyA, byte[] keyB, LCSParams params);
}
