package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.conditions.ValueCondition;

public interface StringBinaryCommands extends BitBinaryCommands {

  /**
   * Experimental: SET with compare-and-* condition.
   */
  @Experimental
  String set(byte[] key, byte[] value, ValueCondition condition);

  /**
   * Experimental: SET+GET with compare-and-* condition.
   */
  @Experimental
  byte[] setGet(byte[] key, byte[] value, ValueCondition condition);

  /**
   * Experimental: SET with SetParams and compare-and-* condition.
   */
  @Experimental
  String set(byte[] key, byte[] value, SetParams params, ValueCondition condition);

  /**
   * Experimental: SET+GET with SetParams and compare-and-* condition.
   */
  @Experimental
  byte[] setGet(byte[] key, byte[] value, SetParams params, ValueCondition condition);


  String set(byte[] key, byte[] value);

  String set(byte[] key, byte[] value, SetParams params);

  byte[] get(byte[] key);

  byte[] setGet(byte[] key, byte[] value);

  byte[] setGet(byte[] key, byte[] value, SetParams params);

  byte[] getDel(byte[] key);

  byte[] getEx(byte[] key, GetExParams params);

  /** Returns the 64-bit XXH3 digest hex (ASCII bytes) of the string value stored at key, or null if missing. */
  @Experimental
  byte[] digest(byte[] key);

  long setrange(byte[] key, long offset, byte[] value);

  byte[] getrange(byte[] key, long startOffset, long endOffset);

  /**
   * @deprecated Use {@link StringBinaryCommands#setGet(byte[], byte[])}.
   */
  @Deprecated
  byte[] getSet(byte[] key, byte[] value);

  long setnx(byte[] key, byte[] value);

  String setex(byte[] key, long seconds, byte[] value);

  String psetex(byte[] key, long milliseconds, byte[] value);

  List<byte[]> mget(byte[]... keys);

  String mset(byte[]... keysvalues);

  long msetnx(byte[]... keysvalues);

  long incr(byte[] key);

  long incrBy(byte[] key, long increment);

  double incrByFloat(byte[] key, double increment);

  long decr(byte[] key);

  long decrBy(byte[] key, long decrement);

  long append(byte[] key, byte[] value);

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
