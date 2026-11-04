package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.SAddExParams;
import redis.clients.jedis.params.SDiffCardParams;
import redis.clients.jedis.params.SUnionCardParams;
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
   * <p>
   * Since Redis 8.12, members stored in dstkey keep the member expiration they have in the first
   * set, if any (see {@link #sexpire(String, long, String...) SEXPIRE}).
   * @param dstkey
   * @param keys group of sets
   * @return The number of elements in the resulting set
   */
  long sdiffstore(String dstkey, String... keys);

  /**
   * <b><a href="https://redis.io/commands/sdiffcard">SDIFFCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sdiff(String...) SDIFF} but instead of returning
   * the result set, it returns just the cardinality of the result. The first key is the minuend set;
   * the remaining keys are subtracted from it. Non existing keys are considered like empty sets, so
   * if the first key does not exist the result is {@code 0}. The command is read-only.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets; the first key is the minuend set
   * @return The cardinality of the set which would result from the difference of the given sets
   * @since 8.0
   */
  long sdiffcard(String... keys);

  /**
   * <b><a href="https://redis.io/commands/sdiffcard">SDIFFCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sdiffcard(String...) SDIFFCARD}, taking the
   * keys as a list.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets; the first key is the minuend set
   * @return The cardinality of the set which would result from the difference of the given sets
   * @since 8.0
   */
  long sdiffcard(List<String> keys);

  /**
   * <b><a href="https://redis.io/commands/sdiffcard">SDIFFCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sdiffcard(String...) SDIFFCARD} with
   * additional options, for the common two-key case.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param key1 the minuend set
   * @param key2 the set subtracted from the first one
   * @param params additional options, e.g. {@code LIMIT}
   * @return The cardinality of the set which would result from the difference of the given sets
   * @since 8.0
   */
  long sdiffcard(String key1, String key2, SDiffCardParams params);

  /**
   * <b><a href="https://redis.io/commands/sdiffcard">SDIFFCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sdiffcard(String...) SDIFFCARD} with
   * additional options. With {@code LIMIT}, the returned cardinality is capped at the limit and the
   * server may stop the computation early; {@code LIMIT 0} means no limit.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets; the first key is the minuend set
   * @param params additional options, e.g. {@code LIMIT}
   * @return The cardinality of the set which would result from the difference of the given sets
   * @since 8.0
   */
  long sdiffcard(List<String> keys, SDiffCardParams params);

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
   * <p>
   * Since Redis 8.12, a member stored in dstkey keeps the nearest member expiration it has in any
   * of the source sets; sources where it has no expiration are ignored.
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
   * <p>
   * Since Redis 8.12, a member stored in dstkey keeps the farthest member expiration it has in any
   * of the source sets; sources where it has no expiration are ignored.
   * @param dstkey
   * @param keys group of sets
   * @return The number of elements in the resulting set
   */
  long sunionstore(String dstkey, String... keys);

  /**
   * <b><a href="https://redis.io/commands/sunioncard">SUNIONCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sunion(String...) SUNION} but instead of
   * returning the result set, it returns just the cardinality of the result. Elements appearing in
   * multiple input sets are counted once. Non existing keys are considered like empty sets. The
   * command is read-only.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets
   * @return The cardinality of the set which would result from the union of all the given sets
   * @since 8.0
   */
  long sunioncard(String... keys);

  /**
   * <b><a href="https://redis.io/commands/sunioncard">SUNIONCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sunioncard(String...) SUNIONCARD}, taking
   * the keys as a list.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets
   * @return The cardinality of the set which would result from the union of all the given sets
   * @since 8.0
   */
  long sunioncard(List<String> keys);

  /**
   * <b><a href="https://redis.io/commands/sunioncard">SUNIONCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sunioncard(String...) SUNIONCARD} with
   * additional options, for the common two-key case.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param key1 the first set
   * @param key2 the second set
   * @param params additional options, e.g. {@code APPROX} and {@code LIMIT}
   * @return The cardinality of the set which would result from the union of the given sets
   * @since 8.0
   */
  long sunioncard(String key1, String key2, SUnionCardParams params);

  /**
   * <b><a href="https://redis.io/commands/sunioncard">SUNIONCARD Command</a></b>
   * This command works exactly like {@link SetCommands#sunioncard(String...) SUNIONCARD} with
   * additional options. With {@code APPROX}, the cardinality is estimated using HyperLogLog
   * (standard error ~0.81%) with fixed, small server-memory overhead. With {@code LIMIT}, the
   * returned cardinality is capped at the limit and the server may stop the computation early;
   * {@code LIMIT 0} means no limit.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys group of sets
   * @param params additional options, e.g. {@code APPROX} and {@code LIMIT}
   * @return The cardinality of the set which would result from the union of all the given sets
   * @since 8.0
   */
  long sunioncard(List<String> keys, SUnionCardParams params);

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
   * <p>
   * Since Redis 8.12, the member moves with its member expiration, if any, replacing whatever
   * expiration it had in the destination set.
   * @param srckey
   * @param dstkey
   * @param member
   * @return 1 if the element was moved, 0 if no operation was performed
   */
  long smove(String srckey, String dstkey, String member);

  /**
   * <b><a href="https://redis.io/commands/sexpire">SEXPIRE Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as time to live in seconds, relative to now.
   * Members without an expiration never expire on their own. A member whose expiration already
   * passed is reported as missing.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param seconds time to live in seconds, relative to now
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 1 if the expiration was set or updated; 2 if the member was deleted because the given
   * time is in the past
   * @since 8.1
   */
  List<Long> sexpire(String key, long seconds, String... members);

  /**
   * <b><a href="https://redis.io/commands/sexpire">SEXPIRE Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as time to live in seconds, relative to now, when the
   * condition holds for that member. A member without an expiration counts as never expiring:
   * NX and LT hold for it, XX and GT do not.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param seconds time to live in seconds, relative to now
   * @param condition NX, XX, GT or LT, evaluated per member
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 0 if the condition was not met; 1 if the expiration was set or updated; 2 if the
   * member was deleted because the given time is in the past
   * @since 8.1
   */
  List<Long> sexpire(String key, long seconds, ExpiryOption condition, String... members);

  /**
   * <b><a href="https://redis.io/commands/spexpire">SPEXPIRE Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as time to live in milliseconds, relative to now.
   * Members without an expiration never expire on their own. A member whose expiration already
   * passed is reported as missing.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param milliseconds time to live in milliseconds, relative to now
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 1 if the expiration was set or updated; 2 if the member was deleted because the given
   * time is in the past
   * @since 8.1
   */
  List<Long> spexpire(String key, long milliseconds, String... members);

  /**
   * <b><a href="https://redis.io/commands/spexpire">SPEXPIRE Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as time to live in milliseconds, relative to now, when the
   * condition holds for that member. A member without an expiration counts as never expiring:
   * NX and LT hold for it, XX and GT do not.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param milliseconds time to live in milliseconds, relative to now
   * @param condition NX, XX, GT or LT, evaluated per member
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 0 if the condition was not met; 1 if the expiration was set or updated; 2 if the
   * member was deleted because the given time is in the past
   * @since 8.1
   */
  List<Long> spexpire(String key, long milliseconds, ExpiryOption condition, String... members);

  /**
   * <b><a href="https://redis.io/commands/sexpireat">SEXPIREAT Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as absolute Unix time in seconds at which the members expire.
   * Members without an expiration never expire on their own. A member whose expiration already
   * passed is reported as missing.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param unixTimeSeconds absolute Unix time in seconds at which the members expire
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 1 if the expiration was set or updated; 2 if the member was deleted because the given
   * time is in the past
   * @since 8.1
   */
  List<Long> sexpireAt(String key, long unixTimeSeconds, String... members);

  /**
   * <b><a href="https://redis.io/commands/sexpireat">SEXPIREAT Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as absolute Unix time in seconds at which the members expire, when the
   * condition holds for that member. A member without an expiration counts as never expiring:
   * NX and LT hold for it, XX and GT do not.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param unixTimeSeconds absolute Unix time in seconds at which the members expire
   * @param condition NX, XX, GT or LT, evaluated per member
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 0 if the condition was not met; 1 if the expiration was set or updated; 2 if the
   * member was deleted because the given time is in the past
   * @since 8.1
   */
  List<Long> sexpireAt(String key, long unixTimeSeconds, ExpiryOption condition, String... members);

  /**
   * <b><a href="https://redis.io/commands/spexpireat">SPEXPIREAT Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as absolute Unix time in milliseconds at which the members expire.
   * Members without an expiration never expire on their own. A member whose expiration already
   * passed is reported as missing.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param unixTimeMillis absolute Unix time in milliseconds at which the members expire
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 1 if the expiration was set or updated; 2 if the member was deleted because the given
   * time is in the past
   * @since 8.1
   */
  List<Long> spexpireAt(String key, long unixTimeMillis, String... members);

  /**
   * <b><a href="https://redis.io/commands/spexpireat">SPEXPIREAT Command</a></b>
   * Set an expiration on one or more members of the set stored at key, given as absolute Unix time in milliseconds at which the members expire, when the
   * condition holds for that member. A member without an expiration counts as never expiring:
   * NX and LT hold for it, XX and GT do not.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param unixTimeMillis absolute Unix time in milliseconds at which the members expire
   * @param condition NX, XX, GT or LT, evaluated per member
   * @param members the members to expire
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; 0 if the condition was not met; 1 if the expiration was set or updated; 2 if the
   * member was deleted because the given time is in the past
   * @since 8.1
   */
  List<Long> spexpireAt(String key, long unixTimeMillis, ExpiryOption condition, String... members);

  /**
   * <b><a href="https://redis.io/commands/sttl">STTL Command</a></b>
   * Remaining time to live, in seconds rounded up, of one or more members of the set stored at key. The command only reads: it fires no notification and removes nothing.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param members the members to query
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; -1 if the member has no expiration; otherwise the remaining time to live in seconds (never 0, it is rounded up)
   * @since 8.1
   */
  List<Long> sttl(String key, String... members);

  /**
   * <b><a href="https://redis.io/commands/spttl">SPTTL Command</a></b>
   * Remaining time to live, in exact milliseconds, of one or more members of the set stored at key. The command only reads: it fires no notification and removes nothing.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param members the members to query
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; -1 if the member has no expiration; otherwise the remaining time to live in milliseconds (0 while the member is expiring now)
   * @since 8.1
   */
  List<Long> spttl(String key, String... members);

  /**
   * <b><a href="https://redis.io/commands/sexpiretime">SEXPIRETIME Command</a></b>
   * Absolute Unix time, in seconds rounded up, at which each of the given members of the set stored at key expires.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param members the members to query
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; -1 if the member has no expiration; otherwise the absolute Unix expiration time in seconds
   * @since 8.1
   */
  List<Long> sexpireTime(String key, String... members);

  /**
   * <b><a href="https://redis.io/commands/spexpiretime">SPEXPIRETIME Command</a></b>
   * Absolute Unix time, in exact milliseconds, at which each of the given members of the set stored at key expires.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param members the members to query
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; -1 if the member has no expiration; otherwise the absolute Unix expiration time in milliseconds
   * @since 8.1
   */
  List<Long> spexpireTime(String key, String... members);

  /**
   * <b><a href="https://redis.io/commands/spersist">SPERSIST Command</a></b>
   * Remove the expiration of one or more members of the set stored at key.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param members the members to persist
   * @return one integer per member, in request order: -2 if the member (or the key) does not
   * exist; -1 if the member has no expiration; 1 if the expiration was removed
   * @since 8.1
   */
  List<Long> spersist(String key, String... members);

  /**
   * <b><a href="https://redis.io/commands/saddex">SADDEX Command</a></b>
   * Add one or more members to the set stored at key, creating it if needed, and optionally set
   * their expiration. Unlike SADD, a member that already exists is not skipped: its expiration is
   * set to the given one, or discarded when no expiration option is given, unless KEEPTTL. With
   * MNX the command applies only if none of the members exists; with MXX only if all of them
   * exist. An expiration in the past deletes the members that exist and adds none.
   * <p>
   * Time complexity O(N) where N is the number of specified members
   * @param key the set
   * @param params condition (MNX or MXX) and expiration (EX, PX, EXAT, PXAT or KEEPTTL)
   * @param members the members to add
   * @return 1 if at least one member was added, updated or deleted; 0 if nothing was modified,
   * that is when MNX or MXX did not hold, when every member already existed with the requested
   * expiration, or when a past time was given and none of the members existed
   * @see SAddExParams
   * @since 8.1
   */
  long saddex(String key, SAddExParams params, String... members);

}
