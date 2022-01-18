package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetCommands {

  long sadd(String key, String... member);

  Set<String> smembers(String key);

  long srem(String key, String... member);

  String spop(String key);

  Set<String> spop(String key, long count);

  long scard(String key);

  boolean sismember(String key, String member);

  List<Boolean> smismember(String key, String... members);

  String srandmember(String key);

  List<String> srandmember(String key, int count);

  default ScanResult<String> sscan(String key, String cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<String> sscan(String key, String cursor, ScanParams params);

  Set<String> sdiff(String... keys);

  long sdiffstore(String dstkey, String... keys);

  Set<String> sinter(String... keys);

  long sinterstore(String dstkey, String... keys);

  /**
   * This command works exactly like {@link SetCommands#sinter(String[]) SINTER} but instead of returning
   * the result set, it returns just the cardinality of the result. LIMIT defaults to 0 and means unlimited
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest
   * @param keys
   * @return The cardinality of the set which would result from the intersection of all the given sets
   */
  long sintercard(String... keys);

  /**
   * This command works exactly like {@link SetCommands#sinter(String[]) SINTER} but instead of returning
   * the result set, it returns just the cardinality of the result.
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest
   * @param limit If the intersection cardinality reaches limit partway through the computation,
   *              the algorithm will exit and yield limit as the cardinality.
   * @param keys
   * @return The cardinality of the set which would result from the intersection of all the given sets
   */
  long sintercard(int limit, String... keys);

  Set<String> sunion(String... keys);

  long sunionstore(String dstkey, String... keys);

  long smove(String srckey, String dstkey, String member);

}
