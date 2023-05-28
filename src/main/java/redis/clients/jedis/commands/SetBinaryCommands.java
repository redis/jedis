package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

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

  long smove(byte[] srckey, byte[] dstkey, byte[] member);

}
