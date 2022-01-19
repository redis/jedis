package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

public interface SortedSetBinaryCommands {

  /**
   * Add the specified member having the specified score to the sorted set stored at key. If member
   * is already a member of the sorted set the score is updated, and the element reinserted in the
   * right position to ensure sorting. If key does not exist a new sorted set with the specified
   * member as sole member is created. If the key exists but does not hold a sorted set value an
   * error is returned.
   * <p>
   * The score value can be the string representation of a double precision floating point number.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param score
   * @param member
   * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
   *         already a member of the sorted set and the score was updated
   */
  long zadd(byte[] key, double score, byte[] member);

  /**
   * @see #zadd(byte[], double, byte[])
   * @param key
   * @param score
   * @param member
   * @param params {@link ZAddParams}
   */
  long zadd(byte[] key, double score, byte[] member, ZAddParams params);

  /**
   * @see #zadd(byte[], double, byte[])
   * @param key
   * @param scoreMembers
   */
  long zadd(byte[] key, Map<byte[], Double> scoreMembers);

  /**
   * @see #zadd(byte[], double, byte[])
   * @param key
   * @param scoreMembers
   * @param params {@link ZAddParams}
   */
  long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params);

  /**
   * Increments the score of member in the sorted set stored at key by increment. If member does not
   * exist in the sorted set, it is added with increment as its score (as if its previous score was 0.0).
   * If key does not exist, a new sorted set with the specified member as its sole member is created.
   * <p>
   * The score value should be the string representation of a numeric value, and accepts double precision
   * floating point numbers. It is possible to provide a negative value to decrement the score.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param score
   * @param member
   * @param params {@link ZAddParams}
   * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
   *         already a member of the sorted set and the score was updated
   */
  Double zaddIncr(byte[] key, double score, byte[] member, ZAddParams params);

  /**
   * Remove the specified member from the sorted set value stored at key. If member was not a member
   * of the set no operation is performed. If key does not hold a set value an error is returned.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param members
   * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
   *         not a member of the set
   */
  long zrem(byte[] key, byte[]... members);

  /**
   * If member already exists in the sorted set adds the increment to its score and updates the
   * position of the element in the sorted set accordingly. If member does not already exist in the
   * sorted set it is added with increment as score (that is, like if the previous score was
   * virtually zero). If key does not exist a new sorted set with the specified member as sole
   * member is created. If the key exists but does not hold a sorted set value an error is returned.
   * <p>
   * The score value can be the string representation of a double precision floating point number.
   * It's possible to provide a negative value to perform a decrement.
   * <p>
   * For an introduction to sorted sets check the Introduction to Redis data types page.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param increment
   * @param member
   * @return The new score
   */
  double zincrby(byte[] key, double increment, byte[] member);

  /**
   * Similar to {@link SortedSetBinaryCommands#zincrby(byte[], double, byte[]) ZINCEBY} with optionals params.
   * @param key
   * @param increment
   * @param member
   * @param params {@link ZIncrByParams}
   * @return The new score
   */
  Double zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params);

  /**
   * Return the rank (or index) of member in the sorted set at key, with scores being ordered from
   * low to high.
   * <p>
   * When the given member does not exist in the sorted set, the special value 'nil' is returned.
   * The returned rank (or index) of the member is 0-based for both commands.
   * <p>
   * Time complexity O(log(N))
   * @param key
   * @param member
   * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
   *         reply if the element exists. A nil bulk reply if there is no such element.
   */
  Long zrank(byte[] key, byte[] member);

  /**
   * Return the rank (or index) of member in the sorted set at key, with scores being ordered from
   * high to low.
   * <p>
   * When the given member does not exist in the sorted set, the special value 'nil' is returned.
   * The returned rank (or index) of the member is 0-based for both commands.
   * <p>
   * Time complexity O(log(N))
   * @param key
   * @param member
   * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
   *         reply if the element exists. A nil bulk reply if there is no such element.
   */
  Long zrevrank(byte[] key, byte[] member);

  /**
   * Returns the specified range of elements in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the
   * number of elements returned.
   * @param key
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Strings in the specified range
   */
  List<byte[]> zrange(byte[] key, long start, long stop);

  /**
   * Returns the specified range of elements in the sorted set stored at key. The elements are
   * considered to be ordered from the highest to the lowest score. Descending lexicographical
   * order is used for elements with equal score.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the
   * number of elements returned.
   * @param key
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Strings in the specified range
   */
  List<byte[]> zrevrange(byte[] key, long start, long stop);

  /**
   * Similar to {@link SortedSetBinaryCommands#zrange(byte[], long, long) ZRANGE}, but the reply will
   * include the scores of the returned elements.
   * @param key
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Tuple in the specified range (elements names and their scores)
   */
  List<Tuple> zrangeWithScores(byte[] key, long start, long stop);

  /**
   * Similar to {@link SortedSetBinaryCommands#zrevrange(byte[], long, long) ZREVRANGE}, but the reply will
   * include the scores of the returned elements.
   * @param key
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Tuple in the specified range (elements names and their scores)
   */
  List<Tuple> zrevrangeWithScores(byte[] key, long start, long stop);

  /**
   * @see #zrange(byte[], long, long)
   * @param key
   * @param zRangeParams {@link ZRangeParams}
   * @return A List of Strings in the specified range
   */
  List<byte[]> zrange(byte[] key, ZRangeParams zRangeParams);

  /**
   * @see #zrangeWithScores(byte[], long, long)
   * @param key
   * @param zRangeParams {@link ZRangeParams}
   * @return A List of Tuple in the specified range (elements names and their scores)
   */
  List<Tuple> zrangeWithScores(byte[] key, ZRangeParams zRangeParams);

  /**
   * Similar to {@link SortedSetBinaryCommands#zrange(byte[], ZRangeParams) ZRANGE}, but stores
   * the result in the dest.
   * <p>
   * @param dest
   * @param src
   * @param zRangeParams {@link ZRangeParams}
   * @return The number of elements in the resulting sorted set
   */
  long zrangestore(byte[] dest, byte[] src, ZRangeParams zRangeParams);

  /**
   * Return a random element from the sorted set value stored at key.
   * <p>
   * Time complexity O(N) where N is the number of elements returned
   * @param key
   * @return Random String from the set
   */
  byte[] zrandmember(byte[] key);

  /**
   * Return an array of distinct elements. The array's length is either count or the sorted set's
   * cardinality ({@link SortedSetBinaryCommands#zcard(byte[]) ZCARD}), whichever is lower.
   * <p>
   * Time complexity O(N) where N is the number of elements returned
   * @param key
   * @param count
   * @return A list of distinct Strings from the set
   */
  List<byte[]> zrandmember(byte[] key, long count);

  /**
   * Similar to {@link SortedSetBinaryCommands#zrandmember(byte[], long) ZRANDMEMBER}, but the reply will
   * include the scores of the returned elements.
   * <p>
   * @see #zrandmember(byte[], long)
   * @param key
   * @param count
   * @return A List of distinct Strings with their scores
   */
  List<Tuple> zrandmemberWithScores(byte[] key, long count);

  /**
   * Return the sorted set cardinality (number of elements). If the key does not exist 0 is
   * returned, like for empty sorted sets.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return The cardinality (number of elements) of the set as an integer.
   */
  long zcard(byte[] key);

  /**
   * Return the score of the specified element of the sorted set at key. If the specified element
   * does not exist in the sorted set, or the key does not exist at all, a special 'nil' value is
   * returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param member
   * @return The score
   */
  Double zscore(byte[] key, byte[] member);

  /**
   * Return the scores associated with the specified members in the sorted set stored at key.
   * For every member that does not exist in the sorted set, a nil value is returned.
   * <p>
   * Time complexity O(N) where N is the number of members being requested
   * @param key
   * @param members
   * @return The scores
   */
  List<Double> zmscore(byte[] key, byte[]... members);

  /**
   * Remove and return the member with the highest score in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @return The popped element and the score
   */
  Tuple zpopmax(byte[] key);

  /**
   * Remove and return up to count members with the highest scores in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)*M) with N being the number of elements in the sorted set, and M being
   * the number of elements popped.
   * @param key
   * @param count the number of elements to pop
   * @return A List of popped elements and scores
   */
  List<Tuple> zpopmax(byte[] key, int count);

  /**
   * Remove and return the member with the lowest score in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @return The popped element and the score
   */
  Tuple zpopmin(byte[] key);

  /**
   * Remove and return up to count members with the lowest scores in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)*M) with N being the number of elements in the sorted set, and M being
   * the number of elements popped.
   * @param key
   * @param count the number of elements to pop
   * @return A List of popped elements and scores
   */
  List<Tuple> zpopmin(byte[] key, int count);

  /**
   * Return the number of elements in the sorted set at key with a score between min and max.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set.
   * @param key
   * @param min
   * @param max
   * @return The number of elements in the specified score range.
   */
  long zcount(byte[] key, double min, double max);

  /**
   * @see #zcount(byte[], double, double)
   * @param key
   * @param min as String
   * @param max as String
   * @return The number of elements in the specified score range.
   */
  long zcount(byte[] key, byte[] min, byte[] max);

  /**
   * Return all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max). The elements are considered to
   * be ordered from low to high scores.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set
   * and M the number of elements being returned.
   * @param key
   * @param min
   * @param max
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrangeByScore(byte[] key, double min, double max);

  /**
   * @see #zrangeByScore(byte[], double, double)
   */
  List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max);

  /**
   * Return all the elements in the sorted set at key with a score between max and min
   * (including elements with score equal to max or min). In contrary to the default
   * ordering of sorted sets, for this command the elements are considered to be ordered
   * from high to low scores.
   * <p>
   * The elements having the same score are returned in reverse lexicographical order.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set
   * and M the number of elements being returned.
   * @param key
   * @param min
   * @param max
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrevrangeByScore(byte[] key, double max, double min);

  /**
   * @see #zrangeByScore(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count);

  /**
   * @see #zrevrangeByScore(byte[], double, double)
   */
  List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

  /**
   * @see #zrangeByScore(byte[], double, double, int, int)
   */
  List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count);

  /**
   * @see #zrevrangeByScore(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count);

  /**
   * Similar to {@link SortedSetBinaryCommands#zrangeByScore(byte[], double, double) ZRANGEBYSCORE},
   * but return both the element and its score, instead of the element alone.
   * <p>
   * @see #zrangeByScore(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @return A List of elements with scores in the specified score range
   */
  List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max);

  /**
   * Similar to {@link SortedSetBinaryCommands#zrevrangeByScore(byte[], double, double) ZREVRANGEBYSCORE},
   * but return both the element and its score, instead of the element alone.
   * <p>
   * @see #zrevrangeByScore(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @return A List of elements with scores in the specified score range
   */
  List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min);

  /**
   * @see #zrangeByScoreWithScores(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return A List of elements with scores in the specified score range
   */
  List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count);

  /**
   * @see #zrevrangeByScore(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

  /**
   * @see #zrangeByScoreWithScores(byte[], double, double)
   */
  List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

  /**
   * @see #zrevrangeByScoreWithScores(byte[], double, double)
   */
  List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

  /**
   * @see #zrangeByScoreWithScores(byte[], double, double)
   */
  List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count);

  /**
   * @see #zrangeByScoreWithScores(byte[], double, double)
   */
  List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count);

  /**
   * @see #zrevrangeByScoreWithScores(byte[], double, double)
   */
  List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count);

  /**
   * Remove all elements in the sorted set at key with rank between start and end. Start and end are
   * 0-based with rank 0 being the element with the lowest score. Both start and end can be negative
   * numbers, where they indicate offsets starting at the element with the highest rank. For
   * example: -1 is the element with the highest score, -2 the element with the second highest score
   * and so forth.
   * <p>
   * Time complexity O(log(N))+O(M) with N being the number of elements in the sorted set and M the
   * number of elements removed by the operation.
   * @param key
   * @param start
   * @param stop
   * @return Integer reply, specifically the number of elements removed
   */
  long zremrangeByRank(byte[] key, long start, long stop);

  /**
   * Remove all the elements in the sorted set at key with a score between min and max (including
   * elements with score equal to min or max).
   * <p>
   * Time complexity O(log(N))+O(M) with N being the number of elements in the sorted set and M the
   * number of elements removed by the operation.
   * @param key
   * @param min
   * @param max
   * @return Integer reply, specifically the number of elements removed
   */
  long zremrangeByScore(byte[] key, double min, double max);

  /**
   * @see #zremrangeByScore(byte[], double, double)
   */
  long zremrangeByScore(byte[] key, byte[] min, byte[] max);

  /**
   * Return the number of elements in the sorted set at key with a value between min and max, when all
   * the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set.
   * @param key
   * @param min
   * @param max
   * @return Integer reply, specifically the number of elements in the specified score range
   */
  long zlexcount(byte[] key, byte[] min, byte[] max);

  /**
   * Return all the elements in the sorted set at key with a value between min and max, when all
   * the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the number of
   * elements being returned.
   * @param key
   * @param min
   * @param max
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max);

  /**
   * @see #zrangeByLex(byte[], byte[], byte[])
   */
  List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count);

  /**
   * Return all the elements in the sorted set at key with a value between max and min, when all
   * the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the number of
   * elements being returned.
   * @param key
   * @param min
   * @param max
   * @return A List of elements in the specified score range
   */
  List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min);

  /**
   * @see #zrevrangeByLex(byte[], byte[], byte[])
   */
  List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count);

  /**
   * Remove all elements in the sorted set stored at key between the lexicographical range specified by min and max,
   * when all the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the number of elements
   * removed by the operation.
   * @param key
   * @param min
   * @param max
   * @return Integer reply, specifically the number of elements removed
   */
  long zremrangeByLex(byte[] key, byte[] min, byte[] max);

  default ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params);

  /**
   * The blocking version of {@link SortedSetBinaryCommands#zpopmax(byte[]) ZPOPMAX}
   * @param timeout specifying the maximum number of seconds to block. A timeout of zero can
   *               be used to block indefinitely.
   * @param keys
   */
  List<byte[]> bzpopmax(double timeout, byte[]... keys);

  /**
   * The blocking version of {@link SortedSetBinaryCommands#zpopmin(byte[]) ZPOPMIN}
   * @param timeout specifying the maximum number of seconds to block. A timeout of zero can
   *               be used to block indefinitely.
   * @param keys
   */
  List<byte[]> bzpopmin(double timeout, byte[]... keys);

  /**
   * Compute the difference between all the sets in the given keys.
   * <p>
   * Time complexity O(L + (N-K)log(N)) worst case where L is the total number of elements in
   * all the sets, N is the size of the first set, and K is the size of the result set.
   * @param keys
   * @return The result of the difference
   */
  Set<byte[]> zdiff(byte[]... keys);

  /**
   * @see #zdiff(byte[]...)
   * @param keys
   * @return The result of the difference with their scores
   */
  Set<Tuple> zdiffWithScores(byte[]... keys);

  /**
   * Similar to {@link #zdiff(byte[]...) ZDIFF}, but store the result in dstkey.
   * @param dstkey
   * @param keys
   * @return Integer reply, specifically the number of elements in the resulting sorted set at dstkey.
   */
  long zdiffStore(byte[] dstkey, byte[]... keys);

  /**
   * Compute the intersection between all the sets in the given keys.
   * <p>
   * Time complexity O(N*K)+O(M*log(M)) worst case with N being the smallest input sorted set, K being
   * the number of input sorted sets and M being the number of elements in the resulting sorted set.
   * @param params {@link ZParams}
   * @param keys
   * @return The result of the intersection
   */
  Set<byte[]> zinter(ZParams params, byte[]... keys);

  /**
   * @see #zinter(ZParams, byte[]...)
   * @param params {@link ZParams}
   * @param keys
   * @return The result of the intersection with their scores
   */
  Set<Tuple> zinterWithScores(ZParams params, byte[]... keys);

  /**
   * @see #zinterstore(byte[], ZParams, byte[]...)
   */
  long zinterstore(byte[] dstkey, byte[]... sets);

  /**
   * Similar to {@link #zinter(ZParams, byte[]...) ZINTER}, but store the result in dstkey.
   * <p>
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the resulting sorted set at dstkey.
   */
  long zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  /**
   * Compute the union between all the sets in the given keys.
   * <p>
   * Time complexity O(N)+O(M log(M)) with N being the sum of the sizes of the input sorted sets,
   * and M being the number of elements in the resulting sorted set.
   * @param params {@link ZParams}
   * @param keys
   * @return The result of the union
   */
  Set<byte[]> zunion(ZParams params, byte[]... keys);

  /**
   * @see #zunion(ZParams, byte[]...)
   * @param params {@link ZParams}
   * @param keys
   * @return The result of the union with their scores
   */
  Set<Tuple> zunionWithScores(ZParams params, byte[]... keys);

  /**
   * @see #zunionstore(byte[], ZParams, byte[]...)
   */
  long zunionstore(byte[] dstkey, byte[]... sets);

  /**
   * Similar to {@link #zunion(ZParams, byte[]...) ZUNION}, but store the result in dstkey.
   * <p>
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the resulting sorted set at dstkey.
   */
  long zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

}
