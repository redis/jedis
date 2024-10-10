package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

import java.util.List;

public interface StringPipelineCommands extends BitPipelineCommands {

  Response<String> set(String key, String value);

  Response<String> set(String key, String value, SetParams params);

  Response<String> get(String key);

  Response<String> setGet(String key, String value);

  Response<String> setGet(String key, String value, SetParams params);

  Response<String> getDel(String key);

  Response<String> getEx(String key, GetExParams params);

  Response<Long> setrange(String key, long offset, String value);

  Response<String> getrange(String key, long startOffset, long endOffset);

  /**
   * @deprecated Use {@link StringPipelineCommands#setGet(java.lang.String, java.lang.String)}.
   */
  @Deprecated
  Response<String> getSet(String key, String value);

  Response<Long> setnx(String key, String value);

  Response<String> setex(String key, long seconds, String value);

  Response<String> psetex(String key, long milliseconds, String value);

  Response<List<String>> mget(String... keys);

  Response<String> mset(String... keysvalues);

  Response<Long> msetnx(String... keysvalues);

  Response<Long> incr(String key);

  Response<Long> incrBy(String key, long increment);

  Response<Double> incrByFloat(String key, double increment);

  Response<Long> decr(String key);

  Response<Long> decrBy(String key, long decrement);

  Response<Long> append(String key, String value);

  Response<String> substr(String key, int start, int end);

  Response<Long> strlen(String key);

  Response<LCSMatchResult> lcs(String keyA, String keyB, LCSParams params);
}
