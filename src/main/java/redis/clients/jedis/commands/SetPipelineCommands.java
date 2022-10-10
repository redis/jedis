package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Response;
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

  Response<Set<String>> sinter(String... keys);

  Response<Long> sinterstore(String dstKey, String... keys);

  Response<Long> sintercard(String... keys);

  Response<Long> sintercard(int limit, String... keys);

  Response<Set<String>> sunion(String... keys);

  Response<Long> sunionstore(String dstKey, String... keys);

  Response<Long> smove(String srckey, String dstKey, String member);

}
