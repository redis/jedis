package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;

public interface BitBinaryCommands {

  boolean setbit(byte[] key, long offset, boolean value);

  boolean getbit(byte[] key, long offset);

  long bitcount(byte[] key);

  long bitcount(byte[] key, long start, long end);

  long bitcount(byte[] key, long start, long end, BitCountOption option);

  long bitpos(byte[] key, boolean value);

  long bitpos(byte[] key, boolean value, BitPosParams params);

  List<Long> bitfield(byte[] key, byte[]... arguments);

  List<Long> bitfieldReadonly(byte[] key, byte[]... arguments);

  /**
   * <b><a href="http://redis.io/commands/bitop">Bitop Command</a></b> Perform a bitwise operation
   * between multiple keys and store the result in the destKey.
   * @param op can be AND, OR, XOR, NOT, DIFF, DIFF1, ANDOR and ONE
   * @param destKey
   * @param srcKeys
   * @return The size of the string stored in the destKey
   */
  long bitop(BitOP op, byte[] destKey, byte[]... srcKeys);
}
