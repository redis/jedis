package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface StringCommands {

  /**
   * <b><a href="http://redis.io/commands/set">Set Command</a></b>
   * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1 GB).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return OK
   */
  String set(String key, String value);

  /**
   * <b><a href="http://redis.io/commands/set">Set Command</a></b>
   * Set the string value as value of the key. Can be used with optional params.
   * <p>
   * Time complexity: O(1)<
   * @param key
   * @param value
   * @param params {@link SetParams}
   * @return simple-string-reply {@code OK} if {@code SET} was executed correctly, or {@code null}
   * if the {@code SET} operation was not performed because the user specified the NX or XX option
   * but the condition was not met.
   */
  String set(String key, String value, SetParams params);

  /**
   * <b><a href="http://redis.io/commands/get">Get Command</a></b>
   * Get the value of the specified key. If the key does not exist the special value 'nil' is
   * returned. If the value stored at key is not a string an error is returned because GET can only
   * handle string values.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The value stored in key
   */
  String get(String key);

  /**
   * WARNING: {@link SetParams#get()} MUST NOT be used with this method.
   */
  String setGet(String key, String value, SetParams params);

  /**
   * <b><a href="http://redis.io/commands/getdel">GetDel Command</a></b>
   * Get the value of key and delete the key. This command is similar to GET, except for the fact
   * that it also deletes the key on success (if and only if the key's value type is a string).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The value of key
   */
  String getDel(String key);

  /**
   * <b><a href="http://redis.io/commands/getex">GetEx Command</a></b>
   * Get the value of key and optionally set its expiration. GETEX is similar to {@link StringCommands#get(String) GET},
   * but is a write command with additional options:
   * EX seconds -- Set the specified expire time, in seconds.
   * PX milliseconds -- Set the specified expire time, in milliseconds.
   * EXAT timestamp-seconds -- Set the specified Unix time at which the key will expire, in seconds.
   * PXAT timestamp-milliseconds -- Set the specified Unix time at which the key will expire, in milliseconds.
   * PERSIST -- Remove the time to live associated with the key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param params {@link GetExParams}
   * @return The original bit value stored at offset
   */
  String getEx(String key, GetExParams params);

  /**
   * <b><a href="http://redis.io/commands/setbit">SetBit Command</a></b>
   * Sets or clears the bit at offset in the string value stored at key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param offset
   * @param value
   * @return The original bit value stored at offset
   */
  boolean setbit(String key, long offset, boolean value);

  /**
   * <b><a href="http://redis.io/commands/getbit">GetBit Command</a></b>
   * Returns the bit value at offset in the string value stored at key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param offset
   * @return The bit value stored at offset
   */
  boolean getbit(String key, long offset);

  /**
   * <b><a href="http://redis.io/commands/setrange">SetRange Command</a></b>
   * GETRANGE overwrite part of the string stored at key, starting at the specified offset, for the entire
   * length of value. If the offset is larger than the current length of the string at key, the string is
   * padded with zero-bytes to make offset fit. Non-existing keys are considered as empty strings, so this
   * command will make sure it holds a string large enough to be able to set value at offset.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param offset
   * @param value
   * @return The length of the string after it was modified by the command
   */
  long setrange(String key, long offset, String value);

  /**
   * <b><a href="http://redis.io/commands/getrange">GetRange Command</a></b>
   * Return the substring of the string value stored at key, determined by the offsets start
   * and end (both are inclusive). Negative offsets can be used in order to provide an offset starting
   * from the end of the string. So -1 means the last character, -2 the penultimate and so forth.
   * <p>
   * Time complexity: O(N) where N is the length of the returned string
   * @param key
   * @param startOffset
   * @param endOffset
   * @return The substring
   */
  String getrange(String key, long startOffset, long endOffset);

  /**
   * <b><a href="http://redis.io/commands/getset">GetSet Command</a></b>
   * GETSET is an atomic set this value and return the old value command. Set key to the string
   * value and return the old value stored at key. The string can't be longer than 1073741824 byte (1 GB).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return The old value that was stored in key
   */
  String getSet(String key, String value);

  /**
   * <b><a href="http://redis.io/commands/setnx">SetNE Command</a></b>
   * SETNX works exactly like {@link StringCommands#set(String, String) SET} with the only difference that if
   * the key already exists no operation is performed. SETNX actually means "SET if Not Exists".
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return 1 if the key was set, 0 otherwise
   */
  long setnx(String key, String value);

  /**
   * <b><a href="http://redis.io/commands/setex">SetEx Command</a></b>
   * The command is exactly equivalent to the following group of commands:
   * {@link StringCommands#set(String, String) SET} + {@link KeyBinaryCommands#expire(byte[], long) EXPIRE}.
   * The operation is atomic.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param seconds
   * @param value
   * @return OK
   */
  String setex(String key, long seconds, String value);

  /**
   * <b><a href="http://redis.io/commands/psetex">PSetEx Command</a></b>
   * PSETEX works exactly like {@link StringCommands#setex(String, long, String) SETEX} with the sole difference
   * that the expire time is specified in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param milliseconds
   * @param value
   * @return OK
   */
  String psetex(String key, long milliseconds, String value);

  /**
   * <b><a href="http://redis.io/commands/mget">MGet Command</a></b>
   * Get the values of all the specified keys. If one or more keys don't exist or is not of type
   * String, a 'nil' value is returned instead of the value of the specified key, but the operation
   * never fails.
   * <p>
   * Time complexity: O(1) for every key
   * @param keys
   * @return Multi bulk reply
   */
  List<String> mget(String... keys);

  /**
   * <b><a href="http://redis.io/commands/mset">MSet Command</a></b>
   * Set the the respective keys to the respective values. MSET will replace old values with new
   * values, while {@link StringCommands#msetnx(String...) MSETNX} will not perform any operation at all even
   * if just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
   * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
   * are modified, another connection talking to Redis can either see the changes to both A and B at
   * once, or no modification at all.
   * @param keysvalues pairs of keys and their values
   *                   e.g mset("foo", "foovalue", "bar", "barvalue")
   * @return OK
   */
  String mset(String... keysvalues);

  /**
   * <b><a href="http://redis.io/commands/msetnx">MSetNX Command</a></b>
   * Set the respective keys to the respective values. {@link StringCommands#mset(String...) MSET} will
   * replace old values with new values, while MSETNX will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
   * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
   * are modified, another connection talking to Redis can either see the changes to both A and B at
   * once, or no modification at all.
   * @param keysvalues pairs of keys and their values
   *                   e.g msetnx("foo", "foovalue", "bar", "barvalue")
   * @return 1 if the all the keys were set, 0 if no key was set (at least one key already existed)
   */
  long msetnx(String... keysvalues);

  /**
   * <b><a href="http://redis.io/commands/incr">Incr Command</a></b>
   * Increment the number stored at key by one. If the key does not exist or contains a value of a
   * wrong type, set the key to the value of "0" before to perform the increment operation.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @param key the key to increment
   * @return The value of the key after the increment
   */
  long incr(String key);

  /**
   * <b><a href="http://redis.io/commands/incrby">IncrBy Command</a></b>
   * INCRBY work just like {@link StringCommands#incr(String) INCR} but instead to increment by 1 the
   * increment is integer.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @param key the key to increment
   * @param increment the value to increment by
   * @return The value of the key after the increment
   */
  long incrBy(String key, long increment);

  /**
   * <b><a href="http://redis.io/commands/incrbyfloat">IncrByFloat Command</a></b>
   * INCRBYFLOAT work just like {@link StringCommands#incrBy(String, long)} INCRBY} but increments by floats
   * instead of integers.
   * <p>
   * INCRBYFLOAT commands are limited to double precision floating point values.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "double" types.
   * Simply the string stored at the key is parsed as a base double precision floating point value,
   * incremented, and then converted back as a string. There is no DECRYBYFLOAT but providing a
   * negative value will work as expected.
   * <p>
   * Time complexity: O(1)
   * @param key the key to increment
   * @param increment the value to increment by
   * @return The value of the key after the increment
   */
  double incrByFloat(String key, double increment);

  /**
   * <b><a href="http://redis.io/commands/decr">Decr Command</a></b>
   * Decrement the number stored at key by one. If the key does not exist or contains a value of a
   * wrong type, set the key to the value of "0" before to perform the decrement operation.
   * <p>
   * DECR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @param key the key to decrement
   * @return The value of the key after the decrement
   */
  long decr(String key);

  /**
   * <b><a href="http://redis.io/commands/decrby">DecrBy Command</a></b>
   * DECRBY work just like {@link StringCommands#decr(String) DECR} but instead to decrement by 1 the
   * decrement is integer.
   * <p>
   * DECRBY commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @param key the key to decrement
   * @param decrement the value to decrement by
   * @return The value of the key after the decrement
   */
  long decrBy(String key, long decrement);

  /**
   * <b><a href="http://redis.io/commands/append">Append Command</a></b>
   * If the key already exists and is a string, this command appends the provided value at the end
   * of the string. If the key does not exist it is created and set as an empty string, so APPEND
   * will be very similar to SET in this special case.
   * <p>
   * Time complexity: O(1). The amortized time complexity is O(1) assuming the appended value is
   * small and the already present value is of any size, since the dynamic string library used by
   * Redis will double the free space available on every reallocation.
   * @param key the key to append to
   * @param value the value to append
   * @return The total length of the string after the append operation.
   */
  long append(String key, String value);

  /**
   * <b><a href="http://redis.io/commands/substr">SubStr Command</a></b>
   * Return a subset of the string from offset start to offset end (both offsets are inclusive).
   * Negative offsets can be used in order to provide an offset starting from the end of the string.
   * So -1 means the last char, -2 the penultimate and so forth.
   * <p>
   * The function handles out of range requests without raising an error, but just limiting the
   * resulting range to the actual length of the string.
   * <p>
   * Time complexity: O(start+n) (with start being the start index and n the total length of the
   * requested range). Note that the lookup part of this command is O(1) so for small strings this
   * is actually an O(1) command.
   * @param key
   * @param start
   * @param end
   * @return The substring
   */
  String substr(String key, int start, int end);

  /**
   * <b><a href="http://redis.io/commands/strlen">StrLen Command</a></b>
   * Return the length of the string value stored at key.
   * @param key
   * @return The length of the string at key, or 0 when key does not exist
   */
  long strlen(String key);

  /**
   * <b><a href="http://redis.io/commands/bitcount">Bitcount Command</a></b>
   * Count the number of set bits (population counting) in a string.
   * @param key
   * @return The number of bits set to 1
   */
  long bitcount(String key);

  /**
   * <b><a href="http://redis.io/commands/bitcount">Bitcount Command</a></b>
   * Count the number of set bits (population counting) in a string only in an interval start and end.
   * <p>
   * Like for the GETRANGE command start and end can contain negative values in order to index bytes
   * starting from the end of the string, where -1 is the last byte, -2 is the penultimate, and so forth.
   * @param key
   * @param start byte start index
   * @param end byte end index
   * @return The number of bits set to 1
   */
  long bitcount(String key, long start, long end);

  /**
   * @see StringCommands#bitcount(String, long, long)
   * @param key
   * @param start byte start index
   * @param end byte end index
   * @param option indicate BYTE or BIT
   * @return The number of bits set to 1
   */
  long bitcount(String key, long start, long end, BitCountOption option);

  /**
   * <b><a href="http://redis.io/commands/bitpos">Bitpos Command</a></b>
   * Return the position of the first bit set to 1 or 0 in a string.
   * @param key
   * @param value the bit value
   * @return The position of the first bit set to 1 or 0 according to the request
   */
  long bitpos(String key, boolean value);

  /**
   * <b><a href="http://redis.io/commands/bitpos">Bitpos Command</a></b>
   * Return the position of the first bit set to 1 or 0 in a string.
   * @param key
   * @param value the bit value
   * @param params {@link BitPosParams}
   * @return The position of the first bit set to 1 or 0 according to the request
   */
  long bitpos(String key, boolean value, BitPosParams params);

  /**
   * <b><a href="http://redis.io/commands/bitfield">Bitfield Command</a></b>
   * The command treats a Redis string as an array of bits, and is capable of addressing specific integer
   * fields of varying bit widths and arbitrary non (necessary) aligned offset.
   * @param key
   * @param arguments may be used with optional arguments
   * @return A List of results
   */
  List<Long> bitfield(String key, String...arguments);

  /**
   * The readonly version of {@link StringCommands#bitfield(String, String...) BITFIELD}
   */
  List<Long> bitfieldReadonly(String key, String...arguments);

  /**
   * <b><a href="http://redis.io/commands/bitop">Bitop Command</a></b>
   * Perform a bitwise operation between multiple keys (containing string values) and store the result in the destKey.
   * @param op can be AND, OR, XOR or NOT
   * @param destKey
   * @param srcKeys
   * @return The size of the string stored in the destKey
   */
  long bitop(BitOP op, String destKey, String... srcKeys);

  /**
   * Calculate the longest common subsequence of keyA and keyB.
   * @deprecated STRALGO LCS command will be removed from Redis 7.
   * {@link StringCommands#lcs(String, String, LCSParams) LCS} can be used instead of this method.
   * @param keyA
   * @param keyB
   * @param params
   * @return According to StrAlgoLCSParams to decide to return content to fill LCSMatchResult.
   */
  @Deprecated
  LCSMatchResult strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params);

  /**
   * Calculate the longest common subsequence of keyA and keyB.
   * @param keyA
   * @param keyB
   * @param params
   * @return According to LCSParams to decide to return content to fill LCSMatchResult.
   */
  LCSMatchResult lcs(String keyA, String keyB, LCSParams params);

}
