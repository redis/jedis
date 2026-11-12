package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.BitRangeIntArrayParams;

public interface BitCommands {

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
   * <b><a href="https://redis.io/commands/bitrangeintarray">BITRANGEINTARRAY Command</a></b>
   * Return the offsets of the bits set to 1 in a byte range of a bitmap, in ascending order. As in
   * BITCOUNT, start and end are inclusive and negative values count from the end.
   * @param key the bitmap key
   * @param start the first byte of the range
   * @param end the last byte of the range
   * @return the offsets of the set bits, or an empty list
   * @since 8.1
   */
  List<Long> bitrangeintarray(String key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/bitrangeintarray">BITRANGEINTARRAY Command</a></b>
   * Same as {@link #bitrangeintarray(String, long, long)}, with these options from {@code params}:
   * <ul>
   * <li>{@code unit(BIT)}: start and end are bit indexes instead of byte indexes</li>
   * <li>{@code limit(count)}: return at most {@code count} offsets</li>
   * <li>{@code zeros()}: return the offsets of the bits set to 0 instead of 1</li>
   * </ul>
   * @param key the bitmap key
   * @param start the first index of the range, in the unit of {@code params}
   * @param end the last index of the range, in the unit of {@code params}
   * @param params {@link BitRangeIntArrayParams}
   * @return the offsets in the range, or an empty list
   * @since 8.1
   */
  List<Long> bitrangeintarray(String key, long start, long end, BitRangeIntArrayParams params);

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
   * @param op can be AND, OR, XOR, NOT, DIFF, DIFF1, ANDOR and ONE
   * @param destKey
   * @param srcKeys
   * @return The size of the string stored in the destKey
   */
  long bitop(BitOP op, String destKey, String... srcKeys);

}
