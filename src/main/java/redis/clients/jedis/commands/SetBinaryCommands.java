package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.SAddExParams;
import redis.clients.jedis.params.SDiffCardParams;
import redis.clients.jedis.params.SUnionCardParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetBinaryCommands {

  long sadd(byte[] key, byte[]... members);

  Set<byte[]> smembers(byte[] key);

  long srem(byte[] key, byte[]... members);

  byte[] spop(byte[] key);

  Set<byte[]> spop(byte[] key, long count);

  long scard(byte[] key);

  boolean sismember(byte[] key, byte[] member);

  List<Boolean> smismember(byte[] key, byte[]... members);

  byte[] srandmember(byte[] key);

  List<byte[]> srandmember(byte[] key, int count);

  default ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params);

  Set<byte[]> sdiff(byte[]... keys);

  long sdiffstore(byte[] dstkey, byte[]... keys);

  /**
   * @see SetCommands#sdiffcard(String...)
   * @since 8.0
   */
  long sdiffcard(byte[]... keys);

  /**
   * @see SetCommands#sdiffcard(String, String, SDiffCardParams)
   * @since 8.0
   */
  long sdiffcard(byte[] key1, byte[] key2, SDiffCardParams params);

  /**
   * @see SetCommands#sdiffcard(java.util.List, SDiffCardParams)
   * @since 8.0
   */
  long sdiffcard(byte[][] keys, SDiffCardParams params);

  Set<byte[]> sinter(byte[]... keys);

  long sinterstore(byte[] dstkey, byte[]... keys);

  /**
   * This command works exactly like {@link SetBinaryCommands#sinter(byte[][]) SINTER} but instead of returning
   * the result set, it returns just the cardinality of the result. LIMIT defaults to 0 and means unlimited
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest
   * @param keys
   * @return The cardinality of the set which would result from the intersection of all the given sets
   */
  long sintercard(byte[]... keys);

  /**
   * This command works exactly like {@link SetBinaryCommands#sinter(byte[][]) SINTER} but instead of returning
   * the result set, it returns just the cardinality of the result.
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest
   * @param limit If the intersection cardinality reaches limit partway through the computation,
   *              the algorithm will exit and yield limit as the cardinality.
   * @param keys
   * @return The cardinality of the set which would result from the intersection of all the given sets
   */
  long sintercard(int limit, byte[]... keys);

  Set<byte[]> sunion(byte[]... keys);

  long sunionstore(byte[] dstkey, byte[]... keys);

  /**
   * @see SetCommands#sunioncard(String...)
   * @since 8.0
   */
  long sunioncard(byte[]... keys);

  /**
   * @see SetCommands#sunioncard(String, String, SUnionCardParams)
   * @since 8.0
   */
  long sunioncard(byte[] key1, byte[] key2, SUnionCardParams params);

  /**
   * @see SetCommands#sunioncard(java.util.List, SUnionCardParams)
   * @since 8.0
   */
  long sunioncard(byte[][] keys, SUnionCardParams params);

  long smove(byte[] srckey, byte[] dstkey, byte[] member);

  /**
   * @see SetCommands#sexpire(String, long, String...)
   * @since 8.1
   */
  List<Long> sexpire(byte[] key, long seconds, byte[]... members);

  /**
   * @see SetCommands#sexpire(String, long, ExpiryOption, String...)
   * @since 8.1
   */
  List<Long> sexpire(byte[] key, long seconds, ExpiryOption condition, byte[]... members);

  /**
   * @see SetCommands#spexpire(String, long, String...)
   * @since 8.1
   */
  List<Long> spexpire(byte[] key, long milliseconds, byte[]... members);

  /**
   * @see SetCommands#spexpire(String, long, ExpiryOption, String...)
   * @since 8.1
   */
  List<Long> spexpire(byte[] key, long milliseconds, ExpiryOption condition, byte[]... members);

  /**
   * @see SetCommands#sexpireAt(String, long, String...)
   * @since 8.1
   */
  List<Long> sexpireAt(byte[] key, long unixTimeSeconds, byte[]... members);

  /**
   * @see SetCommands#sexpireAt(String, long, ExpiryOption, String...)
   * @since 8.1
   */
  List<Long> sexpireAt(byte[] key, long unixTimeSeconds, ExpiryOption condition, byte[]... members);

  /**
   * @see SetCommands#spexpireAt(String, long, String...)
   * @since 8.1
   */
  List<Long> spexpireAt(byte[] key, long unixTimeMillis, byte[]... members);

  /**
   * @see SetCommands#spexpireAt(String, long, ExpiryOption, String...)
   * @since 8.1
   */
  List<Long> spexpireAt(byte[] key, long unixTimeMillis, ExpiryOption condition, byte[]... members);

  /**
   * @see SetCommands#sttl(String, String...)
   * @since 8.1
   */
  List<Long> sttl(byte[] key, byte[]... members);

  /**
   * @see SetCommands#spttl(String, String...)
   * @since 8.1
   */
  List<Long> spttl(byte[] key, byte[]... members);

  /**
   * @see SetCommands#sexpireTime(String, String...)
   * @since 8.1
   */
  List<Long> sexpireTime(byte[] key, byte[]... members);

  /**
   * @see SetCommands#spexpireTime(String, String...)
   * @since 8.1
   */
  List<Long> spexpireTime(byte[] key, byte[]... members);

  /**
   * @see SetCommands#spersist(String, String...)
   * @since 8.1
   */
  List<Long> spersist(byte[] key, byte[]... members);

  /**
   * @see SetCommands#sexpire(String, long, String)
   * @since 8.1
   */
  long sexpire(byte[] key, long seconds, byte[] member);

  /**
   * @see SetCommands#sexpire(String, long, ExpiryOption, String)
   * @since 8.1
   */
  long sexpire(byte[] key, long seconds, ExpiryOption condition, byte[] member);

  /**
   * @see SetCommands#spexpire(String, long, String)
   * @since 8.1
   */
  long spexpire(byte[] key, long milliseconds, byte[] member);

  /**
   * @see SetCommands#spexpire(String, long, ExpiryOption, String)
   * @since 8.1
   */
  long spexpire(byte[] key, long milliseconds, ExpiryOption condition, byte[] member);

  /**
   * @see SetCommands#sexpireAt(String, long, String)
   * @since 8.1
   */
  long sexpireAt(byte[] key, long unixTimeSeconds, byte[] member);

  /**
   * @see SetCommands#sexpireAt(String, long, ExpiryOption, String)
   * @since 8.1
   */
  long sexpireAt(byte[] key, long unixTimeSeconds, ExpiryOption condition, byte[] member);

  /**
   * @see SetCommands#spexpireAt(String, long, String)
   * @since 8.1
   */
  long spexpireAt(byte[] key, long unixTimeMillis, byte[] member);

  /**
   * @see SetCommands#spexpireAt(String, long, ExpiryOption, String)
   * @since 8.1
   */
  long spexpireAt(byte[] key, long unixTimeMillis, ExpiryOption condition, byte[] member);

  /**
   * @see SetCommands#sttl(String, String)
   * @since 8.1
   */
  long sttl(byte[] key, byte[] member);

  /**
   * @see SetCommands#spttl(String, String)
   * @since 8.1
   */
  long spttl(byte[] key, byte[] member);

  /**
   * @see SetCommands#sexpireTime(String, String)
   * @since 8.1
   */
  long sexpireTime(byte[] key, byte[] member);

  /**
   * @see SetCommands#spexpireTime(String, String)
   * @since 8.1
   */
  long spexpireTime(byte[] key, byte[] member);

  /**
   * @see SetCommands#spersist(String, String)
   * @since 8.1
   */
  long spersist(byte[] key, byte[] member);

  /**
   * @see SetCommands#saddex(String, SAddExParams, String...)
   * @since 8.1
   */
  long saddex(byte[] key, SAddExParams params, byte[]... members);

  /**
   * @see SetCommands#saddex(String, SAddExParams, String)
   * @since 8.1
   */
  long saddex(byte[] key, SAddExParams params, byte[] member);

}
