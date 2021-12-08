package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashPipelineBinaryCommands {

  Response<Long> hset(byte[] key, byte[] field, byte[] value);

  Response<Long> hset(byte[] key, Map<byte[], byte[]> hash);

  Response<byte[]> hget(byte[] key, byte[] field);

  Response<Long> hsetnx(byte[] key, byte[] field, byte[] value);

  Response<String> hmset(byte[] key, Map<byte[], byte[]> hash);

  Response<List<byte[]>> hmget(byte[] key, byte[]... fields);

  Response<Long> hincrBy(byte[] key, byte[] field, long value);

  Response<Double> hincrByFloat(byte[] key, byte[] field, double value);

  Response<Boolean> hexists(byte[] key, byte[] field);

  Response<Long> hdel(byte[] key, byte[]... field);

  Response<Long> hlen(byte[] key);

  Response<Set<byte[]>> hkeys(byte[] key);

  Response<List<byte[]>> hvals(byte[] key);

  Response<Map<byte[], byte[]>> hgetAll(byte[] key);

  Response<byte[]> hrandfield(byte[] key);

  Response<List<byte[]>> hrandfield(byte[] key, long count);

  Response<Map<byte[], byte[]>> hrandfieldWithValues(byte[] key, long count);

  default Response<ScanResult<Map.Entry<byte[], byte[]>>> hscan(byte[] key, byte[] cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<Map.Entry<byte[], byte[]>>> hscan(byte[] key, byte[] cursor, ScanParams params);

  Response<Long> hstrlen(byte[] key, byte[] field);

}
