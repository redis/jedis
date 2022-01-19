package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.resps.ScanResult;

public interface KeyBinaryCommands {

  /**
   * Test if the specified key exist.
   * <p>
   * Time complexity: O(N)
   * @see <a href="http://redis.io/commands/exists">Exists Command</a>
   * @param key
   * @return True is the key exists, False otherwise
   */
  boolean exists(byte[] key);

  /**
   * Test if the specified keys exist. The command returns the number of keys exist.
   * <p>
   * Time complexity: O(N)
   * @see <a href="http://redis.io/commands/exists">Exists Command</a>
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys exist,
   *         0 if none of the specified keys exist.
   */
  long exists(byte[]... keys);

  /**
   * Undo a {@link KeyBinaryCommands#expire(byte[], long) expire} at turning the expire key into a normal key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/persist">Persist Command</a>
   * @param key
   * @return Integer reply, specifically: 1: the key is now persist. 0: the key is not persist (only
   *         happens when key not set).
   */
  long persist(byte[] key);

  /**
   * Return the type of the value stored at key in form of a string. The type can be one of "none",
   * "string", "list", "set". "none" is returned if the key does not exist.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/type">Type Command</a>
   * @param key
   * @return Status code reply, specifically: "none" if the key does not exist "string" if the key
   *         contains a String value "list" if the key contains a List value "set" if the key
   *         contains a Set value "zset" if the key contains a Sorted Set value "hash" if the key
   *         contains a Hash value
   */
  String type(byte[] key);

  /**
   * Serialize the value stored at key in a Redis-specific format and return it to the user.
   * <p>
   * Time complexity: O(1) to access the key and additional O(N*M) to serialize it where N is
   * the number of Redis objects composing the value and M their average size.
   * @see <a href="http://redis.io/commands/dump">Dump Command</a>
   * @param key
   * @return The serialized value.
   */
  byte[] dump(byte[] key);

  /**
   * Create a key associated with a value that is obtained by deserializing the provided serialized
   * value (obtained via {@link KeyBinaryCommands#dump(byte[]) DUMP}).
   * <p>
   * Time complexity: O(1) to access the key and additional O(N*M) to serialize it where N is
   * the number of Redis objects composing the value and M their average size.
   * @see <a href="http://redis.io/commands/restore">Restore Command</a>
   * @param key
   * @param ttl If ttl is 0 the key is created without any expire, otherwise the specified expire
   *           time (in milliseconds) is set.
   * @param serializedValue
   * @return Status code reply
   */
  String restore(byte[] key, long ttl, byte[] serializedValue);

  /**
   * Create a key associated with a value that is obtained by deserializing the provided serialized
   * value (obtained via {@link KeyBinaryCommands#dump(byte[]) DUMP}).
   * <p>
   * Time complexity: O(1) to access the key and additional O(N*M) to serialize it where N is
   * the number of Redis objects composing the value and M their average size.
   * @see <a href="http://redis.io/commands/restore">Restore Command</a>
   * @param key
   * @param ttl If ttl is 0 the key is created without any expire, otherwise the specified expire
   *           time (in milliseconds) is set.
   * @param serializedValue
   * @param params {@link RestoreParams}
   * @return Status code reply
   */
  String restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params);

  /**
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
   * {@link KeyBinaryCommands#persist(byte[]) PERSIST} command.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/expire">Expire Command</a>
   * @param key
   * @param seconds
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set since
   *         the key already has an associated timeout (this may happen only in Redis versions &lt;
   *         2.1.3, Redis &gt;= 2.1.3 will happily update the timeout), or the key does not exist.
   */
  long expire(byte[] key, long seconds);

  /**
   * This command works exactly like {@link KeyBinaryCommands#expire(byte[], long) EXPIRE} but the time
   * to live of the key is specified in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/pexpire">PExpire Command</a>
   * @param key
   * @param milliseconds
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long pexpire(byte[] key, long milliseconds);

  /**
   * EXPIREAT works exactly like {@link KeyBinaryCommands#expire(byte[], long) EXPIRE} but instead to get the
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
   * @see <a href="http://redis.io/commands/expireat">ExpireAt Command</a>
   * @param key
   * @param unixTime
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long expireAt(byte[] key, long unixTime);

  /**
   * This command works exactly like {@link KeyBinaryCommands#expireAt(byte[], long) EXPIREAT} but
   * Unix time at which the key will expire is specified in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/pexpireat">PExpireAt Command</a>
   * @param key
   * @param millisecondsTimestamp
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set.
   * e.g. key doesn't exist, or operation skipped due to the provided arguments.
   */
  long pexpireAt(byte[] key, long millisecondsTimestamp);

  /**
   * The TTL command returns the remaining time to live in seconds of a key that has an
   * {@link KeyBinaryCommands#expire(byte[], long) EXPIRE} set. This introspection capability allows a Redis
   * connection to check how many seconds a given key will continue to be part of the dataset.
   * @see <a href="http://redis.io/commands/ttl">TTL Command</a>
   * @param key
   * @return Integer reply, returns the remaining time to live in seconds of a key that has an
   *         EXPIRE. If the Key does not exists or does not have an associated expire, -1 is
   *         returned.
   */
  long ttl(byte[] key);

  /**
   * Similar to {@link KeyBinaryCommands#ttl(byte[]) TTL} returns in milliseconds.
   * @see <a href="http://redis.io/commands/pttl">PTTL Command</a>
   * @param key
   * @return Integer reply, returns the remaining time to live in milliseconds of a key that has an
   *         EXPIRE. If the Key does not exists or does not have an associated expire, -1 is
   *         returned.
   */
  long pttl(byte[] key);

  /**
   * Alters the last access time of a key. A key is ignored if it does not exist.
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @see <a href="http://redis.io/commands/touch">Touch Command</a>
   * @param key
   * @return Integer reply: The number of keys that were touched.
   */
  long touch(byte[] key);

  /**
   * Alters the last access time of a key(s). A key is ignored if it does not exist.
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @see <a href="http://redis.io/commands/touch">Touch Command</a>
   * @param keys
   * @return Integer reply: The number of keys that were touched.
   */
  long touch(byte[]... keys);

  /**
   * Sort a Set or a List.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set values at key. By default, sorting is
   * numeric with elements being compared as double precision floating point numbers. This is the
   * simplest form of SORT.
   * @see <a href="http://redis.io/commands/sort">Sort Command</a>
   * @param key
   * @return Assuming the Set/List at key contains a list of numbers, the return value will be the
   *         list of numbers ordered from the smallest to the biggest number.
   */
  List<byte[]> sort(byte[] key);

  /**
   * Sort a Set or a List and Store the Result at dstkey.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set values at key and store the result
   * at dstkey. By default, sorting is numeric with elements being compared as double precision
   * floating point numbers. This is the simplest form of SORT.
   * @see #sort(byte[])
   * @see <a href="http://redis.io/commands/sort">Sort Command</a>
   * @param key
   * @param dstkey
   * @return The number of elements of the list at dstkey.
   */
  long sort(byte[] key, byte[] dstkey);

  /**
   * Sort a Set or a List accordingly to the specified parameters.
   * @see KeyCommands#sort(String, SortingParams) for examples.
   * @see <a href="http://redis.io/commands/sort">Sort Command</a>
   * @param key
   * @param sortingParameters {@link SortingParams}
   * @return A list of sorted elements.
   */
  List<byte[]> sort(byte[] key, SortingParams sortingParameters);

  /**
   * Sort a Set or a List accordingly to the specified parameters and store the result at dstkey.
   * @see #sort(byte[], SortingParams)
   * @see <a href="http://redis.io/commands/sort">Sort Command</a>
   * @param key
   * @param sortingParameters {@link SortingParams}
   * @param dstkey
   * @return The number of elements stored at dstkey.
   */
  long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey);

  /**
   * Remove the specified key. If a given key does not exist, no operation is performed.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/del">Del Command</a>
   * @param key
   * @return Integer reply, specifically: 1 if the key was removed
   *         0 if the key does not exist
   */
  long del(byte[] key);

  /**
   * Remove the specified keys. If a given key does not exist, no operation is performed.
   * The command returns the number of keys removed.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/del">Del Command</a>
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys were removed
   *         0 if none of the specified key existed
   */
  long del(byte[]... keys);

  /**
   * This command is very similar to {@link KeyBinaryCommands#del(byte[]) DEL}: it removes the specified key.
   * Just like DEL a key is ignored if it does not exist. However, the command performs the actual
   * memory reclaiming in a different thread, so it is not blocking, while DEL is. This is where the
   * command name comes from: the command just unlinks the keys from the keyspace. The actual removal
   * will happen later asynchronously.
   * <p>
   * Time complexity: O(1) for each key removed regardless of its size. Then the command does O(N)
   * work in a different thread in order to reclaim memory, where N is the number of allocations the
   * deleted objects where composed of.
   * @see <a href="http://redis.io/commands/unlink">Unlink Command</a>
   * @param key
   * @return Integer reply: The number of keys that were unlinked
   */
  long unlink(byte[] key);

  /**
   * This command is very similar to {@link KeyBinaryCommands#del(byte[]...) DEL}: it removes the specified keys.
   * Just like DEL a key is ignored if it does not exist. However, the command performs the actual
   * memory reclaiming in a different thread, so it is not blocking, while DEL is. This is where the
   * command name comes from: the command just unlinks the keys from the keyspace. The actual removal
   * will happen later asynchronously.
   * <p>
   * Time complexity: O(1) for each key removed regardless of its size. Then the command does O(N)
   * work in a different thread in order to reclaim memory, where N is the number of allocations the
   * deleted objects where composed of.
   * @see <a href="http://redis.io/commands/unlink">Unlink Command</a>
   * @param keys
   * @return Integer reply: The number of keys that were unlinked
   */
  long unlink(byte[]... keys);

  /**
   * Copy the value stored at the source key to the destination key.
   * <p>
   * @see <a href="http://redis.io/commands/copy">Copy Command</a>
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param replace removes the destination key before copying the value to it, in order to avoid error.
   */
  boolean copy(byte[] srcKey, byte[] dstKey, boolean replace);

  /**
   * Atomically renames the key oldkey to newkey. If the source and destination name are the same an
   * error is returned. If newkey already exists it is overwritten.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/rename">Rename Command</a>
   * @param oldkey
   * @param newkey
   * @return Status code reply
   */
  String rename(byte[] oldkey, byte[] newkey);

  /**
   * Rename oldkey into newkey but fails if the destination key newkey already exists.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/renamenx">RenameNX Command</a>
   * @param oldkey
   * @param newkey
   * @return Integer reply, specifically: 1 if the key was renamed 0 if the target key already exist
   */
  long renamenx(byte[] oldkey, byte[] newkey);

  /**
   * Report the number of bytes that a key and its value require to be stored in RAM.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/memory-usage">Memory Usage Command</a>
   * @param key
   * @return Integer reply, The memory usage in bytes
   */
  Long memoryUsage(byte[] key);

  /**
   * Report the number of bytes that a key and its value require to be stored in RAM.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/memory-usage">Memory Usage Command</a>
   * @param key
   * @param samples the number of sampled nested values. By default, this option is set to 5.
   *               To sample the all the nested values, use 0.
   * @return Integer reply, The memory usage in bytes
   */
  Long memoryUsage(byte[] key, int samples);

  /**
   * Return the reference count of the stored at key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/object-refcount">Object Refcount Command</a>
   * @param key
   * @return Integer reply, The number of references
   */
  Long objectRefcount(byte[] key);

  /**
   * Return the internal encoding for the Redis object stored at key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/object-encoding">Object Encoding Command</a>
   * @param key
   * @return The encoding of the object
   */
  byte[] objectEncoding(byte[] key);

  /**
   * Return the time in seconds since the last access to the value stored at key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/object-idletime">Object IdleTime Command</a>
   * @param key
   * @return Integer reply, The idle time in seconds
   */
  Long objectIdletime(byte[] key);

  /**
   * Return the logarithmic access frequency counter of a Redis object stored at key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/object-freq">Object Freq Command</a>
   * @param key
   * @return Integer reply, The counter's value
   */
  Long objectFreq(byte[] key);

  /**
   * Atomically transfer a key from a source Redis instance to a destination Redis instance.
   * On success the key is deleted from the original instance and is guaranteed to exist in
   * the target instance.
   * <p>
   * @see <a href="http://redis.io/commands/migrate">Migrate Command</a>
   * @param host
   * @param port
   * @param key
   * @param timeout the maximum idle time in any moment of the communication with the
   *               destination instance in milliseconds.
   * @return OK on success, or NOKEY if no keys were found in the source instance.
   */
  String migrate(String host, int port, byte[] key, int timeout);

  /**
   * Atomically transfer a key from a source Redis instance to a destination Redis instance.
   * On success the key is deleted from the original instance and is guaranteed to exist in
   * the target instance.
   * <p>
   * @see <a href="http://redis.io/commands/migrate">Migrate Command</a>
   * @param host
   * @param port
   * @param timeout the maximum idle time in any moment of the communication with the
   *               destination instance in milliseconds.
   * @param params {@link MigrateParams}
   * @param keys
   * @return OK on success, or NOKEY if no keys were found in the source instance.
   */
  String migrate(String host, int port, int timeout, MigrateParams params, byte[]... keys);

  /**
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
   * @see <a href="http://redis.io/commands/keys">Keys Command</a>
   * @param pattern
   * @return Multi bulk reply, list of keys matching pattern.
   */
  Set<byte[]> keys(byte[] pattern);

  ScanResult<byte[]> scan(byte[] cursor);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params, byte[] type);

  /**
   * Return a randomly selected key from the currently selected DB.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/randomkey">RandomKey Command</a>
   * @return Singe line reply, specifically the randomly selected key or an empty string is the
   *         database is empty
   */
  byte[] randomBinaryKey();

}
