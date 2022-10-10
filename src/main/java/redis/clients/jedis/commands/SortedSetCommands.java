package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.KeyedZSetElement;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

public interface SortedSetCommands {

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
   * @return 1 if the new element was added, 0 if the element was already a member of the sorted
   * set and the score was updated
   */
  long zadd(String key, double score, String member);

  /**
   * Similar to {@link SortedSetCommands#zadd(String, double, String) ZADD} but can be used with optional params.
   * @see SortedSetCommands#zadd(String, double, String)
   * @param key
   * @param score
   * @param member
   * @param params {@link ZAddParams}
   * @return 1 if the new element was added, 0 if the element was already a member of the sorted
   * set and the score was updated
   */
  long zadd(String key, double score, String member, ZAddParams params);

  /**
   * Similar to {@link SortedSetCommands#zadd(String, double, String) ZADD} but for multiple members.
   * @see SortedSetCommands#zadd(String, double, String)
   * @param key
   * @param scoreMembers
   * @return The number of elements added to the sorted set (excluding score updates).
   */
  long zadd(String key, Map<String, Double> scoreMembers);

  /**
   * Similar to {@link SortedSetCommands#zadd(String, double, String) ZADD} but can be used with optional params,
   * and fits for multiple members.
   * @see SortedSetCommands#zadd(String, double, String)
   * @param key
   * @param scoreMembers
   * @param params {@link ZAddParams}
   * @return The number of elements added to the sorted set (excluding score updates).
   */
  long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

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
   * @return 1 if the new element was added, 0 if the element was already a member of the sorted
   * set and the score was updated
   */
  Double zaddIncr(String key, double score, String member, ZAddParams params);

  /**
   * Remove the specified member from the sorted set value stored at key. If member was not a member
   * of the set no operation is performed. If key does not hold a set value an error is returned.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param members
   * @return 1 if the new element was removed, 0 if the new element was not a member of the set
   */
  long zrem(String key, String... members);

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
  double zincrby(String key, double increment, String member);

  /**
   * Similar to {@link SortedSetCommands#zincrby(String, double, String) ZINCRBY} but can be used with optionals params.
   * @see SortedSetCommands#zincrby(String, double, String)
   * @param key
   * @param increment
   * @param member
   * @param params {@link ZIncrByParams}
   * @return The new score for key
   */
  Double zincrby(String key, double increment, String member, ZIncrByParams params);

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
   * @return The rank of the element as an integer reply if the element exists. A nil bulk reply
   * if there is no such element
   */
  Long zrank(String key, String member);

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
   * @return The rank of the element as an integer reply if the element exists. A nil bulk reply
   * if there is no such element
   */
  Long zrevrank(String key, String member);

  /**
   * Returns the specified range of elements in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the
   * number of elements returned.
   * @param key the key to query
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Strings in the specified range
   */
  List<String> zrange(String key, long start, long stop);

  /**
   * Returns the specified range of elements in the sorted set stored at key. The elements are
   * considered to be ordered from the highest to the lowest score. Descending lexicographical
   * order is used for elements with equal score.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the
   * number of elements returned.
   * @param key the key to query
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Strings in the specified range
   */
  List<String> zrevrange(String key, long start, long stop);

  /**
   * Returns the specified range of elements in the sorted set stored at key with the scores.
   * @param key the key to query
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Tuple in the specified range (elements names and their scores)
   */
  List<Tuple> zrangeWithScores(String key, long start, long stop);

  /**
   * Similar to {@link SortedSetCommands#zrevrange(String, long, long) ZREVRANGE} but the reply will
   * include the scores of the returned elements.
   * @see SortedSetCommands#zrevrange(String, long, long)
   * @param key the key to query
   * @param start the minimum index
   * @param stop the maximum index
   * @return A List of Tuple in the specified range (elements names and their scores)
   */
  List<Tuple> zrevrangeWithScores(String key, long start, long stop);

  /**
   * Similar to {@link SortedSetCommands#zrange(String, long, long) ZRANGE} but can be used with additional params.
   * @see SortedSetCommands#zrange(String, long, long)
   * @param key the key to query
   * @param zRangeParams {@link ZRangeParams}
   * @return A List of Strings in the specified range
   */
  List<String> zrange(String key, ZRangeParams zRangeParams);

  /**
   * Similar to {@link SortedSetCommands#zrangeWithScores(String, long, long) ZRANGE} but can be used with additional params.
   * @see SortedSetCommands#zrangeWithScores(String, long, long)
   * @param key the key to query
   * @param zRangeParams {@link ZRangeParams}
   * @return A List of Tuple in the specified range (elements names and their scores)
   */
  List<Tuple> zrangeWithScores(String key, ZRangeParams zRangeParams);

  /**
   * Similar to {@link SortedSetCommands#zrange(String, ZRangeParams) ZRANGE} but stores the result in {@code dest}.
   * @see SortedSetCommands#zrange(String, ZRangeParams)
   * @param dest the storing key
   * @param src the key to query
   * @param zRangeParams {@link ZRangeParams}
   * @return The number of elements in the resulting sorted set
   */
  long zrangestore(String dest, String src, ZRangeParams zRangeParams);

  /**
   * Return a random element from the sorted set value stored at key.
   * <p>
   * Time complexity O(N) where N is the number of elements returned
   * @param key
   * @return Random String from the set
   */
  String zrandmember(String key);

  /**
   * Return an array of distinct elements. The array's length is either count or the sorted set's
   * cardinality ({@link SortedSetCommands#zcard(String) ZCARD}), whichever is lower.
   * <p>
   * Time complexity O(N) where N is the number of elements returned
   * @param key
   * @param count choose up to count elements
   * @return A list of distinct Strings from the set
   */
  List<String> zrandmember(String key, long count);

  /**
   * Similar to {@link SortedSetCommands#zrandmember(String, long) ZRANDMEMBER} but the replay will
   * include the scores with the result.
   * @see SortedSetCommands#zrandmember(String, long)
   * @param key
   * @param count choose up to count elements
   * @return A List of distinct Strings with their scores
   */
  List<Tuple> zrandmemberWithScores(String key, long count);

  /**
   * Return the sorted set cardinality (number of elements). If the key does not exist 0 is
   * returned, like for empty sorted sets.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return The cardinality (number of elements) of the set as an integer
   */
  long zcard(String key);

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
  Double zscore(String key, String member);

  /**
   * Return the scores associated with the specified members in the sorted set stored at key.
   * For every member that does not exist in the sorted set, a nil value is returned.
   * <p>
   * Time complexity O(N) where N is the number of members being requested
   * @param key
   * @param members
   * @return The scores
   */
  List<Double> zmscore(String key, String... members);

  /**
   * Remove and return the member with the highest score in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @return The popped element and the score
   */
  Tuple zpopmax(String key);

  /**
   * Remove and return up to count members with the highest scores in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)*M) with N being the number of elements in the sorted set, and M being
   * the number of elements popped.
   * @param key
   * @param count the number of elements to pop
   * @return A List of popped elements and scores
   */
  List<Tuple> zpopmax(String key, int count);

  /**
   * Remove and return the member with the lowest score in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @return The popped element and the score
   */
  Tuple zpopmin(String key);

  /**
   * Remove and return up to count members with the lowest scores in the sorted set stored at key.
   * <p>
   * Time complexity O(log(N)*M) with N being the number of elements in the sorted set, and M being
   * the number of elements popped.
   * @param key
   * @param count the number of elements to pop
   * @return A List of popped elements and scores
   */
  List<Tuple> zpopmin(String key, int count);

  /**
   * Return the number of elements in the sorted set at key with a score between min and max.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set.
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @return The number of elements in the specified score range.
   */
  long zcount(String key, double min, double max);

  /**
   * Similar to {@link SortedSetCommands#zcount(String, double, double) ZCOUNT} but with <i>exclusive</i> range.
   * @see SortedSetCommands#zcount(String, double, double)
   */
  long zcount(String key, String min, String max);

  /**
   * Return all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max). The elements are considered to
   * be ordered from low to high scores.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set
   * and M the number of elements being returned.
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @return A List of elements in the specified score range
   */
  List<String> zrangeByScore(String key, double min, double max);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but with <i>exclusive</i> range.
   * @see SortedSetCommands#zrangeByScore(String, double, double)
   */
  List<String> zrangeByScore(String key, String min, String max);

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
   * @param key the key to query
   * @param max maximum score
   * @param min minimum score
   * @return A List of elements in the specified score range
   */
  List<String> zrevrangeByScore(String key, double max, double min);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but with <i>exclusive</i> range.
   * @see SortedSetCommands#zrangeByScore(String, double, double)
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<String> zrangeByScore(String key, double min, double max, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZREVRANGE} but with <i>exclusive</i> range.
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   */
  List<String> zrevrangeByScore(String key, String max, String min);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but with <i>limit</i> option,
   * @see SortedSetCommands#zrangeByScore(String, double, double)
   * and with <i>exclusive</i> range.
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<String> zrangeByScore(String key, String min, String max, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZRANGE} but with <i>limit</i> option,
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   * @param key the key to query
   * @param max maximum score
   * @param min minimum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<String> zrevrangeByScore(String key, double max, double min, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but return with scores.
   * @see SortedSetCommands#zrangeByScore(String, double, double)
   * return both the element and its score, instead of the element alone.
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @return A List of elements with scores in the specified score range
   */
  List<Tuple> zrangeByScoreWithScores(String key, double min, double max);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZREVRANGE} but return with scores.
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   * return both the element and its score, instead of the element alone.
   * @param key the key to query
   * @param max maximum score
   * @param min minimum score
   * @return A List of elements with scores in the specified score range
   */
  List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but with <i>limit</i> option,
   * and return with scores.
   * @see SortedSetCommands#zrangeByScore(String, double, double)
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZREVRANGE} but with <i>limit</i> option,
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   * and with <i>exclusive</i> range.
   * @param key the key to query
   * @param max maximum score
   * @param min minimum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<String> zrevrangeByScore(String key, String max, String min, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but with <i>exclusive</i> range,
   * and return with scores.
   * @see SortedSetCommands#zrangeByScore(String, double, double)
   */
  List<Tuple> zrangeByScoreWithScores(String key, String min, String max);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZREVRANGE} but with <i>exclusive</i> range,
   * and return with scores.
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   */
  List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min);

  /**
   * Similar to {@link SortedSetCommands#zrangeByScore(String, double, double) ZRANGE} but with <i>exclusive</i> range,
   * with <i>limit</i> options and return with scores.
   * @see SortedSetCommands#zrangeByScore(String, String, String)
   * @param key the key to query
   * @param min minimum score
   * @param max maximum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZREVRANGE} but with
   * <i>limit</i> options and return with scores.
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   * @param key the key to query
   * @param max maximum score
   * @param min minimum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByScore(String, double, double) ZREVRANGE} but with
   * <i>exclusive</i> range, with <i>limit</i> options and return with scores.
   * @see SortedSetCommands#zrevrangeByScore(String, double, double)
   * @param key the key to query
   * @param max maximum score
   * @param min minimum score
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

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
   * @return The number of elements removed
   */
  long zremrangeByRank(String key, long start, long stop);

  /**
   * Remove all the elements in the sorted set at key with a score between min and max (including
   * elements with score equal to min or max).
   * <p>
   * Time complexity O(log(N))+O(M) with N being the number of elements in the sorted set and M the
   * number of elements removed by the operation.
   * @param key
   * @param min minimum score to remove
   * @param max maximum score to remove
   * @return The number of elements removed
   */
  long zremrangeByScore(String key, double min, double max);

  /**
   * Similar to {@link SortedSetCommands#zremrangeByScore(String, double, double) ZREMRANGE} but with <i>limit</i> option.
   * @see SortedSetCommands#zremrangeByScore(String, double, double)
   */
  long zremrangeByScore(String key, String min, String max);

  /**
   * Return the number of elements in the sorted set at key with a value between min and max, when all
   * the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set.
   * @param key
   * @param min minimum value
   * @param max maximum value
   * @return The number of elements in the specified score range
   */
  long zlexcount(String key, String min, String max);

  /**
   * Return all the elements in the sorted set at key with a value between min and max, when all
   * the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the number of
   * elements being returned.
   * @param key
   * @param min minimum value
   * @param max maximum value
   * @return A List of elements in the specified score range
   */
  List<String> zrangeByLex(String key, String min, String max);

  /**
   * Similar to {@link SortedSetCommands#zrangeByLex(String, String, String) ZRANGE} but with <i>limit</i> option.
   * @see SortedSetCommands#zrangeByLex(String, String, String)
   * @param key
   * @param min minimum value
   * @param max maximum value
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<String> zrangeByLex(String key, String min, String max, int offset, int count);

  /**
   * Return all the elements in the sorted set at key with a value between max and min, when all
   * the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the number of
   * elements being returned.
   * @param key
   * @param max maximum value
   * @param min minimum value
   * @return A List of elements in the specified score range
   */
  List<String> zrevrangeByLex(String key, String max, String min);

  /**
   * Similar to {@link SortedSetCommands#zrevrangeByLex(String, String, String) ZRANGE} but with <i>limit</i> option.
   * @see SortedSetCommands#zrevrangeByLex(String, String, String)
   * @param key
   * @param max maximum value
   * @param min minimum value
   * @param offset the first index of the sub-range
   * @param count count of the sub-range. A negative count returns all elements from the offset
   * @return A List of elements in the specified score range
   */
  List<String> zrevrangeByLex(String key, String max, String min, int offset, int count);

  /**
   * Remove all elements in the sorted set stored at key between the lexicographical range specified by min and max,
   * when all the elements in a sorted set are inserted with the same score, in order to force lexicographical ordering.
   * <p>
   * Time complexity O(log(N)+M) with N being the number of elements in the sorted set and M the number of elements
   * removed by the operation.
   * @param key
   * @param min minimum value to remove
   * @param max maximum value to remove
   * @return The number of elements removed
   */
  long zremrangeByLex(String key, String min, String max);

  default ScanResult<Tuple> zscan(String key, String cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

  /**
   * The blocking version of {@link SortedSetCommands#zpopmax(String) ZPOPMAX}
   * @param timeout specifying the maximum number of seconds to block. A timeout of zero can
   *               be used to block indefinitely.
   * @param keys
   */
  KeyedZSetElement bzpopmax(double timeout, String... keys);

  /**
   * The blocking version of {@link SortedSetCommands#zpopmin(String) ZPOPMIN}
   * @param timeout specifying the maximum number of seconds to block. A timeout of zero can
   *               be used to block indefinitely.
   * @param keys
   */
  KeyedZSetElement bzpopmin(double timeout, String... keys);

  /**
   * Compute the difference between all the sets in the given keys.
   * <p>
   * Time complexity O(L + (N-K)log(N)) worst case where L is the total number of elements in
   * all the sets, N is the size of the first set, and K is the size of the result set.
   * @param keys group of sets
   * @return The result of the difference
   */
  Set<String> zdiff(String... keys);

  /**
   * Compute the difference between all the sets in the given keys. Return the result with scores.
   * @param keys group of sets
   * @return The result of the difference with their scores
   */
  Set<Tuple> zdiffWithScores(String... keys);

  /**
   * Compute the difference between all the sets in the given keys. Store the result in dstkey.
   * @param dstkey
   * @param keys group of sets
   * @return The number of elements in the resulting sorted set at dstkey.
   */
  long zdiffStore(String dstkey, String... keys);

  /**
   * Compute the intersection between all the sets in the given keys.
   * <p>
   * Time complexity O(N*K)+O(M*log(M)) worst case with N being the smallest input sorted set, K being
   * the number of input sorted sets and M being the number of elements in the resulting sorted set.
   * @param params {@link ZParams}
   * @param keys group of sets
   * @return The result of the intersection
   */
  Set<String> zinter(ZParams params, String... keys);

  /**
   * Compute the intersection between all the sets in the given keys. Return the result with scores.
   * @param params {@link ZParams}
   * @param keys group of sets
   * @return The result of the intersection with their scores
   */
  Set<Tuple> zinterWithScores(ZParams params, String... keys);

  /**
   * Compute the intersection between all the sets in the given keys. Store the result in dstkey.
   * @param dstkey
   * @param sets group of sets
   * @return The number of elements in the resulting sorted set at dstkey
   */
  long zinterstore(String dstkey, String... sets);

  /**
   * Compute the intersection between all the sets in the given keys. Store the result in dstkey.
   * @param dstkey
   * @param params {@link ZParams}
   * @param sets group of sets
   * @return The number of elements in the resulting sorted set at dstkey
   */
  long zinterstore(String dstkey, ZParams params, String... sets);

  /**
   * Similar to {@link SortedSetCommands#zinter(ZParams, String...) ZINTER}, but
   * instead of returning the result set, it returns just the cardinality of the result.
   * <p>
   * Time complexity O(N*K) worst case with N being the smallest input sorted set, K
   * being the number of input sorted sets
   * @see SortedSetCommands#zinter(ZParams, String...)
   * @param keys group of sets
   * @return The number of elements in the resulting intersection
   */
  long zintercard(String... keys);

  /**
   * Similar to {@link SortedSetCommands#zinter(ZParams, String...) ZINTER}, but
   * instead of returning the result set, it returns just the cardinality of the result.
   * <p>
   * Time complexity O(N*K) worst case with N being the smallest input sorted set, K
   * being the number of input sorted sets
   * @see SortedSetCommands#zinter(ZParams, String...)
   * @param limit If the intersection cardinality reaches limit partway through the computation,
   *              the algorithm will exit and yield limit as the cardinality
   * @param keys group of sets
   * @return The number of elements in the resulting intersection
   */
  long zintercard(long limit, String... keys);

  /**
   * Compute the union between all the sets in the given keys.
   * <p>
   * Time complexity O(N)+O(M log(M)) with N being the sum of the sizes of the input sorted sets,
   * and M being the number of elements in the resulting sorted set.
   * @param params {@link ZParams}
   * @param keys group of sets
   * @return The result of the union
   */
  Set<String> zunion(ZParams params, String... keys);

  /**
   * Compute the union between all the sets in the given keys. Return the result with scores.
   * @param params {@link ZParams}
   * @param keys group of sets
   * @return The result of the union with their scores
   */
  Set<Tuple> zunionWithScores(ZParams params, String... keys);

  /**
   * Compute the union between all the sets in the given keys. Store the result in dstkey.
   * @param dstkey
   * @param sets group of sets
   * @return The number of elements in the resulting sorted set at dstkey
   */
  long zunionstore(String dstkey, String... sets);

  /**
   * Compute the union between all the sets in the given keys. Store the result in dstkey.
   * @param dstkey
   * @param params {@link ZParams}
   * @param sets group of sets
   * @return The number of elements in the resulting sorted set at dstkey
   */
  long zunionstore(String dstkey, ZParams params, String... sets);

  KeyValue<String, List<Tuple>> zmpop(SortedSetOption option, String... keys);

  KeyValue<String, List<Tuple>> zmpop(SortedSetOption option, int count, String... keys);

  KeyValue<String, List<Tuple>> bzmpop(long timeout, SortedSetOption option, String... keys);

  KeyValue<String, List<Tuple>> bzmpop(long timeout, SortedSetOption option, int count, String... keys);
}
