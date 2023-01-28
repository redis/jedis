package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

import java.util.List;

public interface StringPipelineCommands extends BitPipelineCommands {

  Response<String> set(String key, String value);

  Response<String> set(String key, String value, SetParams params);

  Response<String> get(String key);

  /**
   * WARNING: {@link SetParams#get()} MUST NOT be used with this method.
   */
  Response<String> setGet(String key, String value, SetParams params);

  Response<String> getDel(String key);

  Response<String> getEx(String key, GetExParams params);

  Response<Long> setrange(String key, long offset, String value);

  Response<String> getrange(String key, long startOffset, long endOffset);

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

  /**
   * @deprecated STRALGO LCS command will be removed from Redis 7.
   * {@link StringPipelineCommands#lcs(String, String, LCSParams) LCS} can be used instead of this method.
   */
  @Deprecated
  Response<LCSMatchResult> strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params);

  Response<LCSMatchResult> lcs(String keyA, String keyB, LCSParams params);
}
