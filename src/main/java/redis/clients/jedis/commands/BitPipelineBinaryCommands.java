package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.BitRangeIntArrayParams;

public interface BitPipelineBinaryCommands {

  Response<Boolean> setbit(byte[] key, long offset, boolean value);

  Response<Boolean> getbit(byte[] key, long offset);

  Response<Long> bitcount(byte[] key);

  Response<Long> bitcount(byte[] key, long start, long end);

  Response<Long> bitcount(byte[] key, long start, long end, BitCountOption option);

  Response<Long> bitpos(byte[] key, boolean value);

  Response<Long> bitpos(byte[] key, boolean value, BitPosParams params);

  /**
   * Pipeline variant of {@link BitBinaryCommands#bitrangeintarray(byte[], long, long)}.
   * @since 8.1
   */
  Response<List<Long>> bitrangeintarray(byte[] key, long start, long end);

  /**
   * Pipeline variant of
   * {@link BitBinaryCommands#bitrangeintarray(byte[], long, long, BitRangeIntArrayParams)}.
   * @since 8.1
   */
  Response<List<Long>> bitrangeintarray(byte[] key, long start, long end, BitRangeIntArrayParams params);

  Response<List<Long>> bitfield(byte[] key, byte[]... arguments);

  Response<List<Long>> bitfieldReadonly(byte[] key, byte[]... arguments);

  Response<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys);
}
