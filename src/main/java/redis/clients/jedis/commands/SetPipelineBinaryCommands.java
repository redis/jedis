package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.SAddExParams;
import redis.clients.jedis.params.SDiffCardParams;
import redis.clients.jedis.params.SUnionCardParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetPipelineBinaryCommands {

  Response<Long> sadd(byte[] key, byte[]... members);

  Response<Set<byte[]>> smembers(byte[] key);

  Response<Long> srem(byte[] key, byte[]... members);

  Response<byte[]> spop(byte[] key);

  Response<Set<byte[]>> spop(byte[] key, long count);

  Response<Long> scard(byte[] key);

  Response<Boolean> sismember(byte[] key, byte[] member);

  Response<List<Boolean>> smismember(byte[] key, byte[]... members);

  Response<byte[]> srandmember(byte[] key);

  Response<List<byte[]>> srandmember(byte[] key, int count);

  default Response<ScanResult<byte[]>> sscan(byte[] key, byte[] cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<byte[]>> sscan(byte[] key, byte[] cursor, ScanParams params);

  Response<Set<byte[]>> sdiff(byte[]... keys);

  Response<Long> sdiffstore(byte[] dstkey, byte[]... keys);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sdiffcard(byte[]...)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(byte[]... keys);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sdiffcard(byte[], byte[], SDiffCardParams)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(byte[] key1, byte[] key2, SDiffCardParams params);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sdiffcard(byte[][], SDiffCardParams)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(byte[][] keys, SDiffCardParams params);

  Response<Set<byte[]>> sinter(byte[]... keys);

  Response<Long> sinterstore(byte[] dstkey, byte[]... keys);

  Response<Long> sintercard(byte[]... keys);

  Response<Long> sintercard(int limit, byte[]... keys);

  Response<Set<byte[]>> sunion(byte[]... keys);

  Response<Long> sunionstore(byte[] dstkey, byte[]... keys);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sunioncard(byte[]...)}.
   * @since 8.0
   */
  Response<Long> sunioncard(byte[]... keys);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sunioncard(byte[], byte[], SUnionCardParams)}.
   * @since 8.0
   */
  Response<Long> sunioncard(byte[] key1, byte[] key2, SUnionCardParams params);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sunioncard(byte[][], SUnionCardParams)}.
   * @since 8.0
   */
  Response<Long> sunioncard(byte[][] keys, SUnionCardParams params);

  Response<Long> smove(byte[] srckey, byte[] dstkey, byte[] member);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sexpire(byte[], long, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpire(byte[] key, long seconds, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sexpire(byte[], long, ExpiryOption, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpire(byte[] key, long seconds, ExpiryOption condition, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spexpire(byte[], long, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpire(byte[] key, long milliseconds, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spexpire(byte[], long, ExpiryOption, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpire(byte[] key, long milliseconds, ExpiryOption condition, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sexpireAt(byte[], long, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpireAt(byte[] key, long unixTimeSeconds, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sexpireAt(byte[], long, ExpiryOption, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpireAt(byte[] key, long unixTimeSeconds, ExpiryOption condition, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spexpireAt(byte[], long, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpireAt(byte[] key, long unixTimeMillis, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spexpireAt(byte[], long, ExpiryOption, byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpireAt(byte[] key, long unixTimeMillis, ExpiryOption condition, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sttl(byte[], byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> sttl(byte[] key, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spttl(byte[], byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spttl(byte[] key, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#sexpireTime(byte[], byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpireTime(byte[] key, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spexpireTime(byte[], byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpireTime(byte[] key, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#spersist(byte[], byte[]...)}.
   * @since 8.1
   */
  Response<List<Long>> spersist(byte[] key, byte[]... members);

  /**
   * Pipeline variant of {@link SetBinaryCommands#saddex(byte[], SAddExParams, byte[]...)}.
   * @since 8.1
   */
  Response<Long> saddex(byte[] key, SAddExParams params, byte[]... members);

}
