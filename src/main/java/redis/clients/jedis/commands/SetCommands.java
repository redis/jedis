package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetCommands {

  /**
   * Add the specified member to the set value stored at key. If member is already a member of the
   * set no operation is performed. If key does not exist a new set with the specified member as
   * sole member is created. If the key exists but does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param members
   * @return The number of elements that were added to the set, not including all the elements
   * already present in the set
   */
  long sadd(String key, String... members);

  /**
   * Return all the members (elements) of the set value stored at key. This is just syntax glue for
   * {@link SetCommands#sinter(String...) SINTER}.
   * <p>
   * Time complexity O(N)
   * @param key
   * @return All elements of the set
   */
  Set<String> smembers(String key);

  /**
   * Remove the specified member from the set value stored at key. If member was not a member of the
   * set no operation is performed. If key does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param members
   * @return The number of members that were removed from the set, not including non-existing members
   */
  long srem(String key, String... members);

  /**
   * Remove a random element from a Set returning it as return value. If the Set is empty or the key
   * does not exist, a nil object is returned.
   * <p>
   * The {@link SetCommands#srandmember(String)} command does a similar work but the returned element is
   * not removed from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return The removed member, or nil when key does not exist
   */
  String spop(String key);

  /**
   * By default, the command {@link SetCommands#spop(String)} pops a single member from the set.
   * In this command, the reply will consist of up to count members, depending on the set's cardinality.
   * <p>
   * The {@link SetCommands#srandmember(String)} command does a similar work but the returned element is
   * not removed from the Set.
   * <p>
   * Time complexity O(N), where N is the value of the passed count
   * @param key
   * @param count
   * @return The removed members
   */
  Set<String> spop(String key, long count);

  /**
   * Return the set cardinality (number of elements). If the key does not exist 0 is returned, like
   * for empty sets.
   * @param key
   * @return The cardinality (number of elements) of the set
   */
  long scard(String key);

  /**
   * Return true if member is a member of the set stored at key, otherwise false is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param member
   * @return {@code true} if the element is a member of the set, {@code false} otherwise
   */
  boolean sismember(String key, String member);

  /**
   * Returns whether each member is a member of the set stored at key.
   * <p>
   * Time complexity O(N) where N is the number of elements being checked for membership
   * @param key
   * @param members
   * @return List representing the membership of the given elements, in the same order as they are requested
   */
  List<Boolean> smismember(String key, String... members);

  /**
   * Return a random element from a Set, without removing the element. If the Set is empty or the
   * key does not exist, a nil object is returned.
   * <p>
   * The {@link SetCommands#spop(String) SPOP} command does a similar work but the returned element
   * is popped (removed) from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return The randomly selected element
   */
  String srandmember(String key);

  /**
   * Return a random elements from a Set, without removing the elements. If the Set is empty or the
   * key does not exist, an empty list is returned.
   * <p>
   * The {@link SetCommands#spop(String) SPOP} command does a similar work but the returned element
   * is popped (removed) from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param count if positive, return an array of distinct elements.
   *        If negative the behavior changes and the command is allowed to
   *        return the same element multiple times
   * @return A list of randomly selected elements
   */
  List<String> srandmember(String key, int count);

  default ScanResult<String> sscan(String key, String cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<String> sscan(String key, String cursor, ScanParams params);

  /**
   * Return the difference between the Sets stored at {@code keys}
   * <p>
   * <b>Example:</b>
   *
   * <pre>
   * key1 = [x, a, b, c]
   * key2 = [c]
   * key3 = [a, d]
   * SDIFF key1,key2,key3 =&gt; [x, b]
   * </pre>
   *
   * Non existing keys are considered like empty sets.
   * <p>
   * Time complexity O(N) with N being the total number of elements of all the sets
   * @param keys group of sets
   * @return The members of a set resulting from the difference between the sets
   */
  Set<String> sdiff(String... keys);

  /**
   * This command works exactly like {@link SetCommands#sdiff(String...) SDIFF} but instead of being
   * returned the resulting set is stored in dstkey.
   * @param dstkey
   * @param keys group of sets
   * @return The number of elements in the resulting set
   */
  long sdiffstore(String dstkey, String... keys);

  /**
   * Return the members of a set resulting from the intersection of all the sets hold at the
   * specified keys. Like in {@link ListCommands#lrange(String, long, long) LRANGE} the result is sent to
   * the connection as a multi-bulk reply (see the protocol specification for more information). If
   * just a single key is specified, then this command produces the same result as
   * {@link SetCommands#smembers(String) SMEMBERS}. Actually SMEMBERS is just syntax sugar for SINTER.
   * <p>
   * Non existing keys are considered like empty sets, so if one of the keys is missing an empty set
   * is returned (since the intersection with an empty set always is an empty set).
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param keys group of sets
   * @return A set with members of the resulting set
   */
  Set<String> sinter(String... keys);

  /**
   * This command works exactly like {@link SetCommands#sinter(String...) SINTER} but instead of being
   * returned the resulting set is stored as dstkey.
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param dstkey
   * @param keys group of sets
   * @return The number of elements in the resulting set
   */
  long sinterstore(String dstkey, String... keys);

  /**
   * This command works exactly like {@link SetCommands#sinter(String[]) SINTER} but instead of returning
   * the result set, it returns just the cardinality of the result. LIMIT defaults to 0 and means unlimited
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest
   * @param keys group of sets
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
   * @param keys group of sets
   * @return The cardinality of the set which would result from the intersection of all the given sets
   */
  long sintercard(int limit, String... keys);

  /**
   * Return the members of a set resulting from the union of all the sets hold at the specified
   * keys. Like in {@link ListCommands#lrange(String, long, long) LRANGE} the result is sent to the
   * connection as a multi-bulk reply (see the protocol specification for more information). If just
   * a single key is specified, then this command produces the same result as
   * {@link SetCommands#smembers(String) SMEMBERS}.
   * <p>
   * Non existing keys are considered like empty sets.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets
   * @return A set with members of the resulting set
   */
  Set<String> sunion(String... keys);

  /**
   * This command works exactly like {@link SetCommands#sunion(String...) SUNION} but instead of being
   * returned the resulting set is stored as dstkey. Any existing value in dstkey will be
   * over-written.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param dstkey
   * @param keys group of sets
   * @return The number of elements in the resulting set
   */
  long sunionstore(String dstkey, String... keys);

  /**
   * Move the specified member from the set at srckey to the set at dstkey. This operation is
   * atomic, in every given moment the element will appear to be in the source or destination set
   * for accessing clients.
   * <p>
   * If the source set does not exist or does not contain the specified element no operation is
   * performed and zero is returned, otherwise the element is removed from the source set and added
   * to the destination set. On success one is returned, even if the element was already present in
   * the destination set.
   * <p>
   * An error is raised if the source or destination keys contain a non Set value.
   * <p>
   * Time complexity O(1)
   * @param srckey
   * @param dstkey
   * @param member
   * @return 1 if the element was moved, 0 if no operation was performed
   */
  long smove(String srckey, String dstkey, String member);

}
