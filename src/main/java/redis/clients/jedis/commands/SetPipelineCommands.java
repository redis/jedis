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

public interface SetPipelineCommands {

  Response<Long> sadd(String key, String... members);

  Response<Set<String>> smembers(String key);

  Response<Long> srem(String key, String... members);

  Response<String> spop(String key);

  Response<Set<String>> spop(String key, long count);

  Response<Long> scard(String key);

  Response<Boolean> sismember(String key, String member);

  Response<List<Boolean>> smismember(String key, String... members);

  Response<String> srandmember(String key);

  Response<List<String>> srandmember(String key, int count);

  default Response<ScanResult<String>> sscan(String key, String cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<String>> sscan(String key, String cursor, ScanParams params);

  Response<Set<String>> sdiff(String... keys);

  Response<Long> sdiffstore(String dstKey, String... keys);

  /**
   * @deprecated Use {@link SetPipelineCommands#sdiffstore(java.lang.String, java.lang.String...)}.
   */
  @Deprecated
  default Response<Long> sdiffStore(String dstKey, String... keys) {
    return sdiffstore(dstKey, keys);
  }

  /**
   * Pipeline variant of {@link SetCommands#sdiffcard(String...)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(String... keys);

  /**
   * Pipeline variant of {@link SetCommands#sdiffcard(List)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(List<String> keys);

  /**
   * Pipeline variant of {@link SetCommands#sdiffcard(String, String, SDiffCardParams)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(String key1, String key2, SDiffCardParams params);

  /**
   * Pipeline variant of {@link SetCommands#sdiffcard(List, SDiffCardParams)}.
   * @since 8.0
   */
  Response<Long> sdiffcard(List<String> keys, SDiffCardParams params);

  Response<Set<String>> sinter(String... keys);

  Response<Long> sinterstore(String dstKey, String... keys);

  Response<Long> sintercard(String... keys);

  Response<Long> sintercard(int limit, String... keys);

  Response<Set<String>> sunion(String... keys);

  Response<Long> sunionstore(String dstKey, String... keys);

  /**
   * Pipeline variant of {@link SetCommands#sunioncard(String...)}.
   * @since 8.0
   */
  Response<Long> sunioncard(String... keys);

  /**
   * Pipeline variant of {@link SetCommands#sunioncard(List)}.
   * @since 8.0
   */
  Response<Long> sunioncard(List<String> keys);

  /**
   * Pipeline variant of {@link SetCommands#sunioncard(String, String, SUnionCardParams)}.
   * @since 8.0
   */
  Response<Long> sunioncard(String key1, String key2, SUnionCardParams params);

  /**
   * Pipeline variant of {@link SetCommands#sunioncard(List, SUnionCardParams)}.
   * @since 8.0
   */
  Response<Long> sunioncard(List<String> keys, SUnionCardParams params);

  Response<Long> smove(String srckey, String dstKey, String member);

  /**
   * Pipeline variant of {@link SetCommands#sexpire(String, long, String...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpire(String key, long seconds, String... members);

  /**
   * Pipeline variant of {@link SetCommands#sexpire(String, long, ExpiryOption, String...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpire(String key, long seconds, ExpiryOption condition, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spexpire(String, long, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpire(String key, long milliseconds, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spexpire(String, long, ExpiryOption, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpire(String key, long milliseconds, ExpiryOption condition, String... members);

  /**
   * Pipeline variant of {@link SetCommands#sexpireAt(String, long, String...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpireAt(String key, long unixTimeSeconds, String... members);

  /**
   * Pipeline variant of {@link SetCommands#sexpireAt(String, long, ExpiryOption, String...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpireAt(String key, long unixTimeSeconds, ExpiryOption condition, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spexpireAt(String, long, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpireAt(String key, long unixTimeMillis, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spexpireAt(String, long, ExpiryOption, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpireAt(String key, long unixTimeMillis, ExpiryOption condition, String... members);

  /**
   * Pipeline variant of {@link SetCommands#sttl(String, String...)}.
   * @since 8.1
   */
  Response<List<Long>> sttl(String key, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spttl(String, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spttl(String key, String... members);

  /**
   * Pipeline variant of {@link SetCommands#sexpireTime(String, String...)}.
   * @since 8.1
   */
  Response<List<Long>> sexpireTime(String key, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spexpireTime(String, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spexpireTime(String key, String... members);

  /**
   * Pipeline variant of {@link SetCommands#spersist(String, String...)}.
   * @since 8.1
   */
  Response<List<Long>> spersist(String key, String... members);

  /**
   * Pipeline variant of {@link SetCommands#saddex(String, SAddExParams, String...)}.
   * @since 8.1
   */
  Response<Long> saddex(String key, SAddExParams params, String... members);

}
