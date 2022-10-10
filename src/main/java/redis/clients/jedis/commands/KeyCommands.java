package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;

public interface KeyCommands {

  /**
   * <b><a href="http://redis.io/commands/exists">Exists Command</a></b>
   * Test if the specified key exist.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return {@code true} if the key exists, {@code false} otherwise
   */
  boolean exists(String key);

  /**
   * <b><a href="http://redis.io/commands/exists">Exists Command</a></b>
   * Test if the specified keys exist.
   * <p>
   * Time complexity: O(N)
   * @param keys
   * @return The number of keys that exist from those specified as {@code keys}.
   */
  long exists(String... keys);

  /**
   * <b><a href="http://redis.io/commands/persist">Persist Command</a></b>
   * Undo a {@link KeyCommands#expire(String, long) expire} at turning the expire key into a normal key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return 1 if the key is now persist. 0 otherwise (only happens when key not set)
   */
  long persist(String key);

  /**
   * <b><a href="http://redis.io/commands/type">Type Command</a></b>
   * Return the type of the value stored at key in form of a string. The type can be one of "none",
   * "string", "list", "set". "none" is returned if the key does not exist.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return "none" if the key does not exist, "string" if the key contains a String value, "list"
   * if the key contains a List value, "set" if the key contains a Set value, "zset" if the key
   * contains a Sorted Set value, "hash" if the key contains a Hash value
   */
  String type(String key);

  /**
   * <b><a href="http://redis.io/commands/dump">Dump Command</a></b>
   * Serialize the value stored at key in a Redis-specific format and return it to the user.
   * <p>
   * Time complexity: O(1) to access the key and additional O(N*M) to serialize it where N is
   * the number of Redis objects composing the value and M their average size.
   * @param key
   * @return The serialized value
   */
  byte[] dump(String key);

  /**
   * <b><a href="http://redis.io/commands/restore">Restore Command</a></b>
   * Create a key associated with a value that is obtained by deserializing the provided serialized
   * value (obtained via {@link KeyCommands#dump(String) DUMP}).
   * <p>
   * Time complexity: O(1) to access the key and additional O(N*M) to serialize it where N is
   * the number of Redis objects composing the value and M their average size.
   * @param key
   * @param ttl If ttl is 0 the key is created without any expire, otherwise the specified expire
   *           time (in milliseconds) is set.
   * @param serializedValue
   * @return OK
   */
  String restore(String key, long ttl, byte[] serializedValue);

  /**
   * <b><a href="http://redis.io/commands/restore">Restore Command</a></b>
   * Create a key associated with a value that is obtained by deserializing the provided serialized
   * value (obtained via {@link KeyCommands#dump(String) DUMP}).
   * <p>
   * Time complexity: O(1) to access the key and additional O(N*M) to serialize it where N is
   * the number of Redis objects composing the value and M their average size.
   * @param key
   * @param ttl If ttl is 0 the key is created without any expire, otherwise the specified expire
   *           time (in milliseconds) is set.
   * @param serializedValue
   * @param params {@link RestoreParams}
   * @return OK
   */
  String restore(String key, long ttl, byte[] serializedValue, RestoreParams params);

  /**
   * <b><a href="http://redis.io/commands/expire">Expire Command</a></b>
   * Set a timeout on the specified key. After the timeout the key will be automatically deleted by
   * the server. A key with an associated timeout is said to be volatile in Redis terminology.
   * <p>
   * Volatile keys are stored on disk like the other keys, the timeout is persistent too like all
   * the other aspects of the dataset. Saving a dataset containing expires and stopping the server
   * does not stop the flow of time as Redis stores on disk the time when the key will no longer be
   * available as Unix time, and not the remaining seconds.
   * <p>
   * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
   * set. It is also possible to undo the expire at all turning the key into a normal key using the
   * {@link KeyCommands#persist(String) PERSIST} command.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param seconds time to expire
   * @return 1 if the timeout was set, 0 otherwise. Since the key already has an associated timeout
   * (this may happen only in Redis versions &lt; 2.1.3, Redis &gt;= 2.1.3 will happily update the timeout),
   * or the key does not exist.
   */
  long expire(String key, long seconds);

  /**
   * Similar to {@link KeyCommands#expire(String, long) EXPIRE} but with optional expiry setting.
   * @see KeyCommands#expire(String, long)
   * @param key
   * @param seconds time to expire
   * @param expiryOption can be NX, XX, GT or LT
   * @return 1 if the timeout was set, 0 otherwise. Since the key already has an associated timeout
   * (this may happen only in Redis versions &lt; 2.1.3, Redis &gt;= 2.1.3 will happily update the timeout),
   * or the key does not exist.
   */
  long expire(String key, long seconds, ExpiryOption expiryOption);

  /**
   * <b><a href="http://redis.io/commands/pexpire">PExpire Command</a></b>
   * This command works exactly like {@link KeyCommands#expire(String, long) EXPIRE} but the time
   * to live of the key is specified in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param milliseconds time to expire
   * @return 1 if the timeout was set, 0 otherwise.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long pexpire(String key, long milliseconds);

  /**
   * Similar to {@link KeyCommands#pexpire(String, long) EXPIRE} but with optional expiry setting.
   * @see KeyCommands#pexpire(String, long)
   * @param key
   * @param milliseconds time to expire
   * @param expiryOption can be NX, XX, GT or LT
   * @return 1 if the timeout was set, 0 otherwise.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long pexpire(String key, long milliseconds, ExpiryOption expiryOption);


  /**
   * <b><a href="http://redis.io/commands/expireTime">ExpireTime Command</a></b>
   * Returns the absolute Unix timestamp (since January 1, 1970) in seconds at which the given key will expire.
   * <p>
   * The command returns -1 if the key exists but has no associated expiration time, and -2 if the key does not exist.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return Expiration Unix timestamp in seconds, or a negative value in order to signal an error:
   * -1 if the key exists but has no associated expiration time, and -2 if the key does not exist.
   */
  long expireTime(String key);

  /**
   * <b><a href="http://redis.io/commands/pexpireTime">PExpireTime Command</a></b>
   * Similar to {@link KeyCommands#expireTime(String) EXPIRETIME} but returns the absolute Unix expiration
   * timestamp in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @see KeyCommands#expireTime(String)
   * @param key
   * @return Expiration Unix timestamp in milliseconds, or a negative value in order to signal an error:
   * -1 if the key exists but has no associated expiration time, and -2 if the key does not exist.
   */
  long pexpireTime(String key);

  /**
   * <b><a href="http://redis.io/commands/expireat">ExpireAt Command</a></b>
   * EXPIREAT works exactly like {@link KeyCommands#expire(String, long) EXPIRE} but instead to get the
   * number of seconds representing the Time To Live of the key as a second argument (that is a
   * relative way of specifying the TTL), it takes an absolute one in the form of a UNIX timestamp
   * (Number of seconds elapsed since 1 Gen 1970).
   * <p>
   * EXPIREAT was introduced in order to implement the Append Only File persistence mode so that
   * EXPIRE commands are automatically translated into EXPIREAT commands for the append only file.
   * Of course EXPIREAT can also used by programmers that need a way to simply specify that a given
   * key should expire at a given time in the future.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param unixTime time to expire
   * @return 1 if the timeout was set, 0 otherwise.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long expireAt(String key, long unixTime);

  /**
   * <b><a href="http://redis.io/commands/expireat">ExpireAt Command</a></b>
   * Similar to {@link KeyCommands#expireAt(String, long) EXPIREAT} but with {@code ExpiryOption}.
   * @see KeyCommands#expireAt(String, long)
   * @param key
   * @param unixTime time to expire
   * @param expiryOption can be NX, XX, GT or LT
   * @return 1 if the timeout was set, 0 otherwise.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long expireAt(String key, long unixTime, ExpiryOption expiryOption);

  /**
   * <b><a href="http://redis.io/commands/pexpireat">PExpireAt Command</a></b>
   * This command works exactly like {@link KeyCommands#expireAt(String, long) EXPIREAT} but
   * Unix time at which the key will expire is specified in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param millisecondsTimestamp time to expire
   * @return 1 if the timeout was set, 0 otherwise.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long pexpireAt(String key, long millisecondsTimestamp);

  /**
   * <b><a href="http://redis.io/commands/expireat">ExpireAt Command</a></b>
   * Similar to {@link KeyCommands#pexpireAt(String, long) PEXPIREAT} but with {@code ExpiryOption}.
   * @see KeyCommands#pexpireAt(String, long)
   * @param key
   * @param millisecondsTimestamp time to expire
   * @param expiryOption can be NX, XX, GT or LT
   * @return 1 if the timeout was set, 0 otherwise.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long pexpireAt(String key, long millisecondsTimestamp, ExpiryOption expiryOption);

  /**
   * <b><a href="http://redis.io/commands/ttl">TTL Command</a></b>
   * The TTL command returns the remaining time to live in seconds of a key that has an
   * {@link KeyCommands#expire(String, long) EXPIRE} set. This introspection capability allows a Redis
   * connection to check how many seconds a given key will continue to be part of the dataset.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return TTL in seconds, or a negative value in order to signal an error
   */
  long ttl(String key);

  /**
   * <b><a href="http://redis.io/commands/pttl">PTTL Command</a></b>
   * The PTTL command returns the remaining time to live in milliseconds of a key that has an
   * {@link KeyCommands#expire(String, long) EXPIRE} set.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return TTL in milliseconds, or a negative value in order to signal an error
   */
  long pttl(String key);

  /**
   * <b><a href="http://redis.io/commands/touch">Touch Command</a></b>
   * Alters the last access time of a key. A key is ignored if it does not exist.
   * <p>
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @param key
   * @return The number of keys that were touched
   */
  long touch(String key);

  /**
   * <b><a href="http://redis.io/commands/touch">Touch Command</a></b>
   * Alters the last access time of a key(s). A key is ignored if it does not exist.
   * <p>
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @param keys
   * @return The number of keys that were touched
   */
  long touch(String... keys);

  /**
   * <b><a href="http://redis.io/commands/sort">Sort Command</a></b>
   * Sort a Set or a List.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set values at key. By default, sorting is
   * numeric with elements being compared as double precision floating point numbers. This is the
   * simplest form of SORT.
   * @see KeyCommands#sort(String, SortingParams)
   * @param key
   * @return Assuming the Set/List at key contains a list of numbers, the return value will be the
   *         list of numbers ordered from the smallest to the biggest number.
   */
  List<String> sort(String key);

  /**
   * Similar to {@link KeyCommands#sort(String) SORT} but store the result in {@code dstkey}.
   * @see KeyCommands#sort(String)
   * @param key
   * @param dstkey
   * @return The number of elements stored at dstkey.
   */
  long sort(String key, String dstkey);

  /**
   * Sort a Set or a List accordingly to the specified parameters.
   * <p>
   * <b>examples:</b>
   * <p>
   * Given are the following sets and key/values:
   *
   * <pre>
   * x = [1, 2, 3]
   * y = [a, b, c]
   *
   * k1 = z
   * k2 = y
   * k3 = x
   *
   * w1 = 9
   * w2 = 8
   * w3 = 7
   * </pre>
   *
   * Sort Order:
   *
   * <pre>
   * sort(x) or sort(x, sp.asc())
   * -&gt; [1, 2, 3]
   *
   * sort(x, sp.desc())
   * -&gt; [3, 2, 1]
   *
   * sort(y)
   * -&gt; [c, a, b]
   *
   * sort(y, sp.alpha())
   * -&gt; [a, b, c]
   *
   * sort(y, sp.alpha().desc())
   * -&gt; [c, a, b]
   * </pre>
   *
   * Limit (e.g. for Pagination):
   *
   * <pre>
   * sort(x, sp.limit(0, 2))
   * -&gt; [1, 2]
   *
   * sort(y, sp.alpha().desc().limit(1, 2))
   * -&gt; [b, a]
   * </pre>
   *
   * Sorting by external keys:
   *
   * <pre>
   * sort(x, sb.by(w*))
   * -&gt; [3, 2, 1]
   *
   * sort(x, sb.by(w*).desc())
   * -&gt; [1, 2, 3]
   * </pre>
   *
   * Getting external keys:
   *
   * <pre>
   * sort(x, sp.by(w*).get(k*))
   * -&gt; [x, y, z]
   *
   * sort(x, sp.by(w*).get(#).get(k*))
   * -&gt; [3, x, 2, y, 1, z]
   * </pre>
   * @param key
   * @param sortingParameters {@link SortingParams}
   * @return A list of sorted elements
   */
  List<String> sort(String key, SortingParams sortingParameters);

  /**
   * Similar to {@link KeyCommands#sort(String, SortingParams) SORT} but store the result in {@code dstkey}.
   * @see KeyCommands#sort(String, SortingParams)
   * @param key
   * @param sortingParameters {@link SortingParams}
   * @param dstkey
   * @return The number of elements stored at dstkey
   */
  long sort(String key, SortingParams sortingParameters, String dstkey);

  /**
   * Read-only variant of the {@link KeyCommands#sort(String, SortingParams) SORT} command.
   * It is exactly like the original SORT but refuses the STORE option and can safely be used in read-only replicas.
   * @param key the key to sort
   * @param sortingParams {@link SortingParams}
   * @return list of sorted elements.
   */
  List<String> sortReadonly(String key, SortingParams sortingParams);

  /**
   * <b><a href="http://redis.io/commands/del">Del Command</a></b>
   * Remove the specified key. If a given key does not exist, no operation is performed.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return 1 if the key was removed, 0 if the key does not exist
   */
  long del(String key);

  /**
   * Remove the specified keys. If a given key does not exist, no operation is performed.
   * <p>
   * Time complexity: O(N)
   * @param keys
   * @return An integer greater than 0 if one or more keys were removed, 0 if none of the specified keys existed
   */
  long del(String... keys);

  /**
   * <b><a href="http://redis.io/commands/unlink">Unlink Command</a></b>
   * This command is very similar to {@link KeyCommands#del(String) DEL}: it removes the specified key.
   * Just like DEL a key is ignored if it does not exist. However, the command performs the actual
   * memory reclaiming in a different thread, so it is not blocking, while DEL is. This is where the
   * command name comes from: the command just unlinks the keys from the keyspace. The actual removal
   * will happen later asynchronously.
   * <p>
   * Time complexity: O(1) for each key removed regardless of its size. Then the command does O(N)
   * work in a different thread in order to reclaim memory, where N is the number of allocations the
   * deleted objects where composed of.
   * @param key
   * @return The number of keys that were unlinked
   */
  long unlink(String key);

  /**
   * Similar to {@link KeyCommands#unlink(String) SORT} but can be used with multiple keys.
   * @see KeyCommands#unlink(String)
   * @param keys
   * @return The number of keys that were unlinked
   */
  long unlink(String... keys);

  /**
   * <b><a href="http://redis.io/commands/copy">Copy Command</a></b>
   * Copy the value stored at the source key to the destination key.
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param replace removes the destination key before copying the value to it, in order to avoid error.
   * @return {@code true} if source was copied, {@code false} otherwise
   */
  boolean copy(String srcKey, String dstKey, boolean replace);

  /**
   * <b><a href="http://redis.io/commands/rename">Rename Command</a></b>
   * Atomically renames the key {@code oldkey} to {@code newkey}. If the source and destination name are the same an
   * error is returned. If {@code newkey} already exists it is overwritten.
   * <p>
   * Time complexity: O(1)
   * @param oldkey
   * @param newkey
   * @return OK
   */
  String rename(String oldkey, String newkey);

  /**
   * <b><a href="http://redis.io/commands/renamenx">RenameNX Command</a></b>
   * Rename oldkey into newkey but fails if the destination key newkey already exists.
   * <p>
   * Time complexity: O(1)
   * @param oldkey
   * @param newkey
   * @return 1 if the key was renamed, 0 if the target key already exist
   */
  long renamenx(String oldkey, String newkey);

  /**
   * <b><a href="http://redis.io/commands/memory-usage">Memory Usage Command</a></b>
   * Report the number of bytes that a key and its value require to be stored in RAM.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The memory usage in bytes
   */
  Long memoryUsage(String key);

  /**
   * <b><a href="http://redis.io/commands/memory-usage">Memory Usage Command</a></b>
   * Report the number of bytes that a key and its value require to be stored in RAM.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param samples the number of sampled nested values. By default, this option is set to 5.
   *               To sample the all the nested values, use 0.
   * @return The memory usage in bytes
   */
  Long memoryUsage(String key, int samples);

  /**
   * <b><a href="http://redis.io/commands/object-refcount">Object Refcount Command</a></b>
   * Return the reference count of the stored at key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The number of references
   */
  Long objectRefcount(String key);

  /**
   * <b><a href="http://redis.io/commands/object-encoding">Object Encoding Command</a></b>
   * Return the internal encoding for the Redis object stored at key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The encoding of the object
   */
  String objectEncoding(String key);

  /**
   * <b><a href="http://redis.io/commands/object-idletime">Object IdleTime Command</a></b>
   * Return the time in seconds since the last access to the value stored at key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The idle time in seconds
   */
  Long objectIdletime(String key);

  /**
   * <b><a href="http://redis.io/commands/object-freq">Object Freq Command</a></b>
   * Return the logarithmic access frequency counter of a Redis object stored at key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The counter's value
   */
  Long objectFreq(String key);

  /**
   * <b><a href="http://redis.io/commands/migrate">Migrate Command</a></b>
   * Atomically transfer a key from a source Redis instance to a destination Redis instance.
   * On success the key is deleted from the original instance and is guaranteed to exist in
   * the target instance.
   * @param host
   * @param port
   * @param key
   * @param timeout the maximum idle time in any moment of the communication with the
   *               destination instance in milliseconds.
   * @return OK on success, or NOKEY if no keys were found in the source instance.
   */
  String migrate(String host, int port, String key, int timeout);

  /**
   * <b><a href="http://redis.io/commands/migrate">Migrate Command</a></b>
   * Atomically transfer a key from a source Redis instance to a destination Redis instance.
   * On success the key is deleted from the original instance and is guaranteed to exist in
   * the target instance.
   * @param host
   * @param port
   * @param timeout the maximum idle time in any moment of the communication with the
   *               destination instance in milliseconds.
   * @param params {@link MigrateParams}
   * @param keys
   * @return OK on success, or NOKEY if no keys were found in the source instance.
   */
  String migrate(String host, int port, int timeout, MigrateParams params, String... keys);

  /**
   * <b><a href="http://redis.io/commands/keys">Keys Command</a></b>
   * Returns all the keys matching the glob-style pattern as space separated strings. For example if
   * you have in the database the keys "foo" and "foobar" the command "KEYS foo*" will return
   * "foo foobar".
   * <p>
   * Note that while the time complexity for this operation is O(n) the constant times are pretty
   * low. For example Redis running on an entry level laptop can scan a 1 million keys database in
   * 40 milliseconds. <b>Still it's better to consider this one of the slow commands that may ruin
   * the DB performance if not used with care.</b>
   * <p>
   * In other words this command is intended only for debugging and special operations like creating
   * a script to change the DB schema. Don't use it in your normal code. Use Redis Sets in order to
   * group together a subset of objects.
   * <p>
   * Glob style patterns examples:
   * <ul>
   * <li>h?llo will match hello hallo hhllo
   * <li>h*llo will match hllo heeeello
   * <li>h[ae]llo will match hello and hallo, but not hillo
   * </ul>
   * <p>
   * Use \ to escape special chars if you want to match them verbatim.
   * <p>
   * Time complexity: O(n) (with n being the number of keys in the DB, and assuming keys and pattern
   * of limited length)
   * @param pattern
   * @return List of keys matching the pattern.
   */
  Set<String> keys(String pattern);

  ScanResult<String> scan(String cursor);

  ScanResult<String> scan(String cursor, ScanParams params);

  ScanResult<String> scan(String cursor, ScanParams params, String type);

  /**
   * <b><a href="http://redis.io/commands/randomkey">RandomKey Command</a></b>
   * Return a randomly selected key from the currently selected DB.
   * <p>
   * Time complexity: O(1)
   * @return The random key, or {@code nil} when the database is empty
   */
  String randomKey();

}
