package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.BitRangeIntArrayParams;

import java.util.List;

public interface BitPipelineCommands {

  Response<Boolean> setbit(String key, long offset, boolean value);

  Response<Boolean> getbit(String key, long offset);

  Response<Long> bitcount(String key);

  Response<Long> bitcount(String key, long start, long end);

  Response<Long> bitcount(String key, long start, long end, BitCountOption option);

  Response<Long> bitpos(String key, boolean value);

  Response<Long> bitpos(String key, boolean value, BitPosParams params);

  /**
   * Pipeline variant of {@link BitCommands#bitrangeintarray(String, long, long)}.
   * @since 8.1
   */
  Response<List<Long>> bitrangeintarray(String key, long start, long end);

  /**
   * Pipeline variant of
   * {@link BitCommands#bitrangeintarray(String, long, long, BitRangeIntArrayParams)}.
   * @since 8.1
   */
  Response<List<Long>> bitrangeintarray(String key, long start, long end, BitRangeIntArrayParams params);

  Response<List<Long>> bitfield(String key, String...arguments);

  Response<List<Long>> bitfieldReadonly(String key, String...arguments);

  Response<Long> bitop(BitOP op, String destKey, String... srcKeys);
}
