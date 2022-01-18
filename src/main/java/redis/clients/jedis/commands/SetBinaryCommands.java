package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetBinaryCommands {

  /**
   * Add the specified member to the set value stored at key. If member is already a member of the
   * set no operation is performed. If key does not exist a new set with the specified member as
   * sole member is created. If the key exists but does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param members
   * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
   *         already a member of the set
   */
  long sadd(byte[] key, byte[]... members);

  /**
   * Return all the members (elements) of the set value stored at key. This is just syntax glue for
   * {@link SetBinaryCommands#sinter(byte[]...) SINTER}.
   * <p>
   * Time complexity O(N)
   * @param key
   * @return Multi bulk reply
   */
  Set<byte[]> smembers(byte[] key);

  /**
   * Remove the specified member from the set value stored at key. If member was not a member of the
   * set no operation is performed. If key does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param members
   * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
   *         not a member of the set
   */
  long srem(byte[] key, byte[]... members);

  /**
   * Remove a random element from a Set returning it as return value. If the Set is empty or the key
   * does not exist, a nil object is returned.
   * <p>
   * The {@link SetBinaryCommands#srandmember(byte[])} command does a similar work but the returned element is
   * not removed from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return Bulk reply
   */
  byte[] spop(byte[] key);

  /**
   * By default, the command {@link SetBinaryCommands#spop(byte[])} pops a single member from the set.
   * In this command, the reply will consist of up to count members, depending on the set's cardinality.
   * <p>
   * The {@link SetBinaryCommands#srandmember(byte[])} command does a similar work but the returned element is
   * not removed from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param count
   * @return Bulk reply
   */
  Set<byte[]> spop(byte[] key, long count);

  /**
   * Return the set cardinality (number of elements). If the key does not exist 0 is returned, like
   * for empty sets.
   * @param key
   * @return Integer reply, specifically: the cardinality (number of elements) of the set as an
   *         integer.
   */
  long scard(byte[] key);

  /**
   * Return true if member is a member of the set stored at key, otherwise false is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param member
   * @return Boolean reply, specifically: true if the element is a member of the set false if the
   *         element is not a member of the set OR if the key does not exist
   */
  boolean sismember(byte[] key, byte[] member);

  /**
   * Returns whether each member is a member of the set stored at key.
   * <p>
   * Time complexity O(N) where N is the number of elements being checked for membership
   * @param key
   * @param members
   * @return List representing the membership of the given elements, in the same order as they are
   *         requested.
   */
  List<Boolean> smismember(byte[] key, byte[]... members);

  /**
   * Return a random element from a Set, without removing the element. If the Set is empty or the
   * key does not exist, a nil object is returned.
   * <p>
   * The {@link SetBinaryCommands#spop(byte[]) SPOP} command does a similar work but the returned element
   * is popped (removed) from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return Bulk reply
   */
  byte[] srandmember(byte[] key);

  /**
   * Return a random elements from a Set, without removing the elements. If the Set is empty or the
   * key does not exist, an empty list is returned.
   * <p>
   * The {@link SetBinaryCommands#spop(byte[]) SPOP} command does a similar work but the returned element
   * is popped (removed) from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param count if positive, return an array of distinct elements.
   *        If negative the behavior changes and the command is allowed to
   *        return the same element multiple times
   * @return list of elements
   */
  List<byte[]> srandmember(byte[] key, int count);

  default ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params);

  /**
   * Return the difference between the Set stored at key1 and all the Sets key2, ..., keyN
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
   * <b>Time complexity:</b>
   * <p>
   * O(N) with N being the total number of elements of all the sets
   * @param keys
   * @return Return the members of a set resulting from the difference between the first set
   *         provided and all the successive sets.
   */
  Set<byte[]> sdiff(byte[]... keys);

  /**
   * This command works exactly like {@link SetBinaryCommands#sdiff(byte[][]) SDIFF} but instead of being
   * returned the resulting set is stored in dstkey.
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  long sdiffstore(byte[] dstkey, byte[]... keys);

  /**
   * Return the members of a set resulting from the intersection of all the sets hold at the
   * specified keys. Like in {@link ListBinaryCommands#lrange(byte[], long, long) LRANGE} the result is sent to
   * the connection as a multi-bulk reply (see the protocol specification for more information). If
   * just a single key is specified, then this command produces the same result as
   * {@link SetBinaryCommands#smembers(byte[]) SMEMBERS}. Actually SMEMBERS is just syntax sugar for SINTER.
   * <p>
   * Non existing keys are considered like empty sets, so if one of the keys is missing an empty set
   * is returned (since the intersection with an empty set always is an empty set).
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param keys
   * @return Multi bulk reply, specifically the list of common elements.
   */
  Set<byte[]> sinter(byte[]... keys);

  /**
   * This command works exactly like {@link SetBinaryCommands#sinter(byte[][]) SINTER} but instead of being
   * returned the resulting set is stored as dstkey.
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
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

  /**
   * Return the members of a set resulting from the union of all the sets hold at the specified
   * keys. Like in {@link ListBinaryCommands#lrange(byte[], long, long) LRANGE} the result is sent to the
   * connection as a multi-bulk reply (see the protocol specification for more information). If just
   * a single key is specified, then this command produces the same result as
   * {@link SetBinaryCommands#smembers(byte[]) SMEMBERS}.
   * <p>
   * Non existing keys are considered like empty sets.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys
   * @return Multi bulk reply, specifically the list of common elements.
   */  
  Set<byte[]> sunion(byte[]... keys);

  /**
   * This command works exactly like {@link SetBinaryCommands#sunion(byte[][]) SUNION} but instead of being
   * returned the resulting set is stored as dstkey. Any existing value in dstkey will be
   * over-written.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  long sunionstore(byte[] dstkey, byte[]... keys);

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
   * @return Integer reply, specifically: 1 if the element was moved 0 if the element was not found
   *         on the first set and no operation was performed
   */
  long smove(byte[] srckey, byte[] dstkey, byte[] member);

}
