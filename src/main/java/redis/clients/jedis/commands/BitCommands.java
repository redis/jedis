package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;

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
