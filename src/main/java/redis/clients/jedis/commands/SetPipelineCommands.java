package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Response;
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

}
