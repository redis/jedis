package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface StringBinaryCommands {

  /**
   * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1 GB).
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/set">Set Command</a>
   * @param key
   * @param value
   * @return Status code reply
   */
  String set(byte[] key, byte[] value);

  /**
   * Similar to {@link StringBinaryCommands#set(byte[], byte[]) SET} but with optional params.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/set">Set Command</a>
   * @param key
   * @param value
   * @param params {@link SetParams}
   * @return Status code reply
   */
  String set(byte[] key, byte[] value, SetParams params);

  /**
   * Get the value of the specified key. If the key does not exist the special value 'nil' is
   * returned. If the value stored at key is not a string an error is returned because GET can only
   * handle string values.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/get">Get Command</a>
   * @param key
   * @return Bulk reply
   */
  byte[] get(byte[] key);

  /**
   * Get the value of key and delete the key. This command is similar to GET, except for the fact
   * that it also deletes the key on success (if and only if the key's value type is a string).
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/getdel">GetDel Command</a>
   * @param key
   * @return the value of key
   * @since Redis 6.2
   */
  byte[] getDel(byte[] key);

  /**
   * Get the value of key and optionally set its expiration. GETEX is similar to {@link StringBinaryCommands#get(byte[]) GET},
   * but is a write command with additional options:
   * EX seconds -- Set the specified expire time, in seconds.
   * PX milliseconds -- Set the specified expire time, in milliseconds.
   * EXAT timestamp-seconds -- Set the specified Unix time at which the key will expire, in seconds.
   * PXAT timestamp-milliseconds -- Set the specified Unix time at which the key will expire, in milliseconds.
   * PERSIST -- Remove the time to live associated with the key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/getex">GetEx Command</a>
   * @param key
   * @param params {@link GetExParams}
   * @return The original bit value stored at offset
   */
  byte[] getEx(byte[] key, GetExParams params);

  /**
   * Sets or clears the bit at offset in the string value stored at key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/setbit">SetBit Command</a>
   * @param key
   * @param offset
   * @param value
   * @return The original bit value stored at offset
   */
  boolean setbit(byte[] key, long offset, boolean value);

  /**
   * Returns the bit value at offset in the string value stored at key.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/getbit">GetBit Command</a>
   * @param key
   * @param offset
   * @return The bit value stored at offset
   */
  boolean getbit(byte[] key, long offset);

  /**
   * GETRANGE overwrite part of the string stored at key, starting at the specified offset, for the entire
   * length of value. If the offset is larger than the current length of the string at key, the string is
   * padded with zero-bytes to make offset fit. Non-existing keys are considered as empty strings, so this
   * command will make sure it holds a string large enough to be able to set value at offset.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/setrange">SetRange Command</a>
   * @param key
   * @param offset
   * @param value
   * @return Integer reply, specifically the length of the string after it was modified by the command
   */
  long setrange(byte[] key, long offset, byte[] value);

  /**
   * GETRANGE return the substring of the string value stored at key, determined by the offsets start
   * and end (both are inclusive). Negative offsets can be used in order to provide an offset starting
   * from the end of the string. So -1 means the last character, -2 the penultimate and so forth.
   * <p>
   * Time complexity: O(N) where N is the length of the returned string
   * @see <a href="http://redis.io/commands/getrange">GetRange Command</a>
   * @param key
   * @param startOffset
   * @param endOffset
   * @return Bulk reply
   */
  byte[] getrange(byte[] key, long startOffset, long endOffset);

  /**
   * GETSET is an atomic set this value and return the old value command. Set key to the string
   * value and return the old value stored at key. The string can't be longer than 1073741824 byte (1 GB).
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/getset">GetSet Command</a>
   * @param key
   * @param value
   * @return Bulk reply
   */
  byte[] getSet(byte[] key, byte[] value);

  /**
   * SETNX works exactly like {@link StringBinaryCommands#set(byte[], byte[]) SET} with the only difference that if
   * the key already exists no operation is performed. SETNX actually means "SET if Not Exists".
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/setnx">SetNE Command</a>
   * @param key
   * @param value
   * @return Integer reply, specifically: 1 if the key was set 0 if the key was not set
   */
  long setnx(byte[] key, byte[] value);

  /**
   * The command is exactly equivalent to the following group of commands:
   * {@link StringBinaryCommands#set(byte[], byte[]) SET} + {@link KeyBinaryCommands#expire(byte[], long) EXPIRE}.
   * The operation is atomic.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/setex">SetEx Command</a>
   * @param key
   * @param seconds
   * @param value
   * @return Status code reply
   */
  String setex(byte[] key, long seconds, byte[] value);

  /**
   * PSETEX works exactly like {@link StringBinaryCommands#setex(byte[], long, byte[])} with the sole difference
   * that the expire time is specified in milliseconds instead of seconds.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/psetex">PSetEx Command</a>
   * @param key
   * @param milliseconds
   * @param value
   * @return Status code reply
   */
  String psetex(byte[] key, long milliseconds, byte[] value);

  /**
   * Get the values of all the specified keys. If one or more keys don't exist or is not of type
   * String, a 'nil' value is returned instead of the value of the specified key, but the operation
   * never fails.
   * <p>
   * Time complexity: O(1) for every key
   * @see <a href="http://redis.io/commands/mget">MGet Command</a>
   * @param keys
   * @return Multi bulk reply
   */
  List<byte[]> mget(byte[]... keys);

  /**
   * Set the the respective keys to the respective values. MSET will replace old values with new
   * values, while {@link StringBinaryCommands#msetnx(byte[][]) MSETNX} will not perform any operation at all even
   * if just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
   * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
   * are modified, another connection talking to Redis can either see the changes to both A and B at
   * once, or no modification at all.
   * @see StringBinaryCommands#msetnx(byte[][])
   * @see <a href="http://redis.io/commands/mset">MSet Command</a>
   * @param keysvalues
   * @return Status code reply, Basically always OK as MSET can't fail
   */
  String mset(byte[]... keysvalues);

  /**
   * Set the respective keys to the respective values. {@link StringBinaryCommands#mset(byte[][]) MSET} will
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
   * @see StringBinaryCommands#mset(byte[][])
   * @see <a href="http://redis.io/commands/msetnx">MSetNX Command</a>
   * @param keysvalues
   * @return Integer reply, specifically: 1 if the all the keys were set 0 if no key was set (at
   *         least one key already existed)
   */
  long msetnx(byte[]... keysvalues);

  /**
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
   * @see <a href="http://redis.io/commands/incr">Incr Command</a>
   * @param key
   * @return Integer reply, the value of the key after the increment
   */
  long incr(byte[] key);

  /**
   * INCRBY work just like {@link StringBinaryCommands#incr(byte[]) INCR} but instead to increment by 1 the
   * increment is integer.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/incrby">IncrBy Command</a>
   * @param key
   * @param increment
   * @return Integer reply, the value of the key after the increment
   */
  long incrBy(byte[] key, long increment);

  /**
   * INCRBYFLOAT work just like {@link StringBinaryCommands#incrBy(byte[], long)} INCRBY} but increments by floats
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
   * @see <a href="http://redis.io/commands/incrbyfloat">IncrByFloat Command</a>
   * @param key the key to increment
   * @param increment the value to increment by
   * @return Integer reply, the value of the key after the increment
   */
  double incrByFloat(byte[] key, double increment);

  /**
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
   * @see <a href="http://redis.io/commands/decr">Decr Command</a>
   * @param key
   * @return Integer reply, the value of the key after the decrement
   */
  long decr(byte[] key);

  /**
   * DECRBY work just like {@link StringBinaryCommands#decr(byte[]) DECR} but instead to decrement by 1 the
   * decrement is integer.
   * <p>
   * DECRBY commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/decrby">DecrBy Command</a>
   * @param key
   * @param decrement
   * @return Integer reply, the value of the key after the decrement
   */
  long decrBy(byte[] key, long decrement);

  /**
   * If the key already exists and is a string, this command appends the provided value at the end
   * of the string. If the key does not exist it is created and set as an empty string, so APPEND
   * will be very similar to SET in this special case.
   * <p>
   * Time complexity: O(1). The amortized time complexity is O(1) assuming the appended value is
   * small and the already present value is of any size, since the dynamic string library used by
   * Redis will double the free space available on every reallocation.
   * @see <a href="http://redis.io/commands/append">Append Command</a>
   * @param key
   * @param value
   * @return Integer reply, specifically the total length of the string after the append operation.
   */
  long append(byte[] key, byte[] value);

  /**
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
   * @see <a href="http://redis.io/commands/substr">SubStr Command</a>
   * @param key
   * @param start
   * @param end
   * @return Bulk reply
   */
  byte[] substr(byte[] key, int start, int end);

  /**
   * Return the length of the string value stored at key.
   * <p>
   * @see <a href="http://redis.io/commands/strlen">StrLen Command</a>
   * @param key
   * @return Integer reply, specifically the length of the string at key, or 0 when key does not exist
   */
  long strlen(byte[] key);

  /**
   * Count the number of set bits (population counting) in a string.
   * <p>
   * @see <a href="http://redis.io/commands/bitcount">Bitcount Command</a>
   * @param key
   * @return Integer reply, specifically the number of bits set to 1
   */
  long bitcount(byte[] key);

  /**
   * @see #bitcount(byte[]) BITCOUNT
   * @param key
   * @param start byte start index
   * @param end byte end index
   * @return Integer reply, specifically the number of bits set to 1
   */
  long bitcount(byte[] key, long start, long end);

  /**
   * @see #bitcount(byte[]) BITCOUNT
   * @param key
   * @param start byte start index
   * @param end byte end index
   * @param option {@link BitCountOption}
   * @return Integer reply, specifically the number of bits set to 1
   */
  long bitcount(byte[] key, long start, long end, BitCountOption option);

  /**
   * Return the position of the first bit set to 1 or 0 in a string.
   * <p>
   * @see <a href="http://redis.io/commands/bitpos">Bitpos Command</a>
   * @param key
   * @param value the bit value
   * @return The position of the first bit set to 1 or 0 according to the request
   */
  long bitpos(byte[] key, boolean value);

  /**
   * @see #bitpos(byte[], boolean) BITPOS
   * @param key
   * @param value the bit value
   * @param params {@link BitPosParams}
   * @return The position of the first bit set to 1 or 0 according to the request
   */
  long bitpos(byte[] key, boolean value, BitPosParams params);

  /**
   * The command treats a Redis string as an array of bits, and is capable of addressing specific integer
   * fields of varying bit widths and arbitrary non (necessary) aligned offset.
   * <p>
   * @see <a href="http://redis.io/commands/bitfield">Bitfield Command</a>
   * @param key
   * @param arguments
   * @return A List of results
   */
  List<Long> bitfield(byte[] key, byte[]... arguments);

  /**
   * The readonly version of {@link #bitfield(byte[], byte[]...) BITFIELD}
   */
  List<Long> bitfieldReadonly(byte[] key, byte[]... arguments);

  /**
   * Perform a bitwise operation between multiple keys (containing string values) and store the result in the destKey.
   * <p>
   * @see <a href="http://redis.io/commands/bitop">Bitop Command</a>
   * @param op {@link BitOP}
   * @param destKey
   * @param srcKeys
   * @return Integer reply, The size of the string stored in the destKey
   */
  long bitop(BitOP op, byte[] destKey, byte[]... srcKeys);

  /**
   * Calculate the longest common subsequence of keyA and keyB.
   * <p>
   * @param keyA keyA
   * @param keyB keyB
   * @param params the params
   * @return According to StrAlgoLCSParams to decide to return content to fill LCSMatchResult.
   */
  LCSMatchResult strAlgoLCSKeys(final byte[] keyA, final byte[] keyB, final StrAlgoLCSParams params);
}
