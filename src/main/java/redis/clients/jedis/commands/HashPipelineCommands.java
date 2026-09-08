package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.HashImport;
import redis.clients.jedis.Response;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.HGetExParams;
import redis.clients.jedis.params.HSetExParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashPipelineCommands {

  Response<Long> hset(String key, String field, String value);

  Response<Long> hset(String key, Map<String, String> hash);

  Response<Long> hsetex(String key, HSetExParams params, String field, String value);

  Response<Long> hsetex(String key, HSetExParams params, Map<String, String> hash);

  Response<String> hget(String key, String field);
  
  Response<List<String>> hgetex(String key, HGetExParams params, String... fields);

  Response<List<String>> hgetdel(String key, String... fields);
  
  Response<Long> hsetnx(String key, String field, String value);

  Response<String> hmset(String key, Map<String, String> hash);

  Response<List<String>> hmget(String key, String... fields);

  Response<Long> hincrBy(String key, String field, long value);

  Response<Double> hincrByFloat(String key, String field, double value);

  Response<Boolean> hexists(String key, String field);

  Response<Long> hdel(String key, String... field);

  Response<Long> hlen(String key);

  Response<Set<String>> hkeys(String key);

  Response<List<String>> hvals(String key);

  Response<Map<String, String>> hgetAll(String key);

  Response<String> hrandfield(String key);

  Response<List<String>> hrandfield(String key, long count);

  Response<List<Map.Entry<String, String>>> hrandfieldWithValues(String key, long count);

  default Response<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor, ScanParams params);

  default Response<ScanResult<String>> hscanNoValues(String key, String cursor) {
    return hscanNoValues(key, cursor, new ScanParams());
  }

  Response<ScanResult<String>> hscanNoValues(String key, String cursor, ScanParams params);

  Response<Long> hstrlen(String key, String field);

  Response<List<Long>> hexpire(String key, long seconds, String... fields);

  Response<List<Long>> hexpire(String key, long seconds, ExpiryOption condition, String... fields);

  Response<List<Long>> hpexpire(String key, long milliseconds, String... fields);

  Response<List<Long>> hpexpire(String key, long milliseconds, ExpiryOption condition, String... fields);

  Response<List<Long>> hexpireAt(String key, long unixTimeSeconds, String... fields);

  Response<List<Long>> hexpireAt(String key, long unixTimeSeconds, ExpiryOption condition, String... fields);

  Response<List<Long>> hpexpireAt(String key, long unixTimeMillis, String... fields);

  Response<List<Long>> hpexpireAt(String key, long unixTimeMillis, ExpiryOption condition, String... fields);

  Response<List<Long>> hexpireTime(String key, String... fields);

  Response<List<Long>> hpexpireTime(String key, String... fields);

  Response<List<Long>> httl(String key, String... fields);

  Response<List<Long>> hpttl(String key, String... fields);

  Response<List<Long>> hpersist(String key, String... fields);

  /**
   * Pipelined {@link HashCommands#himportSet(String, HashImport, String...)} ({@code HIMPORT SET},
   * Redis 8.10). Each call is applied on its own and may be freely pipelined with unrelated
   * commands; the {@code HIMPORT PREPARE} for {@code fieldset} is buffered automatically ahead of
   * the first use on the pipeline's connection and is not part of the user-facing results.
   * <p>
   * Supported only on a single-connection pipeline; a transaction ({@code MULTI}) or a cluster
   * pipeline throws {@link UnsupportedOperationException} (the connection-local {@code PREPARE}
   * can neither be staged in {@code MULTI}/{@code EXEC} nor routed by slot).
   *
   * @param key the key of the hash to create; any existing hash at this key is replaced
   * @param fieldset the field-set template describing the ordered field names shared by imported
   *          hashes
   * @param values the field values for this hash, matched positionally against the template's
   *          fields (same count)
   * @return the {@code SET} response ({@code OK})
   * @throws IllegalArgumentException if the number of values differs from the template's field
   *           count
   * @throws IllegalStateException if the template has been {@linkplain HashImport#close() closed}
   * @see HashImport
   * @since 8.0
   */
  @Experimental
  Response<String> himportSet(String key, HashImport fieldset, String... values);
}
