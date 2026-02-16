package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.MSetExParams;

import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface StringBinaryCommands extends BitBinaryCommands {

  String set(byte[] key, byte[] value);

  String set(byte[] key, byte[] value, SetParams params);

  byte[] get(byte[] key);

  byte[] setGet(byte[] key, byte[] value);

  byte[] setGet(byte[] key, byte[] value, SetParams params);

  byte[] getDel(byte[] key);

  byte[] getEx(byte[] key, GetExParams params);

  long setrange(byte[] key, long offset, byte[] value);

  byte[] getrange(byte[] key, long startOffset, long endOffset);

  /**
   * @deprecated Use {@link StringBinaryCommands#setGet(byte[], byte[])}.
   */
  @Deprecated
  byte[] getSet(byte[] key, byte[] value);

  /**
   * @deprecated Use {@link StringBinaryCommands#set(byte[], byte[], SetParams)} with {@link SetParams#nx()}.
   * Deprecated in Jedis 8.0.0. Mirrors Redis deprecation since 2.6.12.
   */
  @Deprecated
  long setnx(byte[] key, byte[] value);

  /**
   * @deprecated Use {@link StringBinaryCommands#set(byte[], byte[], SetParams)} with {@link SetParams#ex(long)}.
   * Deprecated in Jedis 8.0.0. Mirrors Redis deprecation since 2.6.12.
   */
  @Deprecated
  String setex(byte[] key, long seconds, byte[] value);

  /**
   * @deprecated Use {@link StringBinaryCommands#set(byte[], byte[], SetParams)} with {@link SetParams#px(long)}.
   * Deprecated in Jedis 8.0.0. Mirrors Redis deprecation since 2.6.12.
   */
  @Deprecated
  String psetex(byte[] key, long milliseconds, byte[] value);

  List<byte[]> mget(byte[]... keys);

  String mset(byte[]... keysvalues);

  long msetnx(byte[]... keysvalues);

  /**
   * Multi-set with optional condition and expiration.
   * <p>
   * Sets the respective keys to the respective values, similar to {@link #mset(byte[]...) MSET},
   * but allows conditional set (NX|XX) and expiration options via {@link MSetExParams}.
   * If the condition is not met for any key, no key is set.
   * <p>
   * Both MSET and MSETEX are atomic operations. This means that if multiple keys are provided,
   * another client will either see the changes for all keys at once, or no changes at all.
   * <p>
   * Options (in {@link MSetExParams}): NX or XX, and expiration: EX seconds | PX milliseconds |
   * EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL.
   * <p>
   * Time complexity: O(N) where N is the number of keys to set.
   * @param params condition and expiration parameters
   * @param keysvalues pairs of keys and their values, e.g. {@code msetex(params, "foo".getBytes(), "foovalue".getBytes(), "bar".getBytes(), "barvalue".getBytes())}
   * @return {@code true} if all the keys were set, {@code false} if none were set (condition not satisfied)
   * @see #mset(byte[]...)
   * @see #msetnx(byte[]...)
   */
  boolean msetex(MSetExParams params, byte[]... keysvalues);

  long incr(byte[] key);

  long incrBy(byte[] key, long increment);

  double incrByFloat(byte[] key, double increment);

  long decr(byte[] key);

  long decrBy(byte[] key, long decrement);

  long append(byte[] key, byte[] value);

  /**
   * @deprecated Use {@link StringBinaryCommands#getrange(byte[], long, long)}.
   * Deprecated in Jedis 8.0.0. Mirrors Redis deprecation since 2.0.0.
   */
  @Deprecated
  byte[] substr(byte[] key, int start, int end);

  long strlen(byte[] key);

  /**
   * Calculate the longest common subsequence of keyA and keyB.
   * @param keyA
   * @param keyB
   * @param params
   * @return According to LCSParams to decide to return content to fill LCSMatchResult.
   */
  LCSMatchResult lcs(byte[] keyA, byte[] keyB, LCSParams params);
}
