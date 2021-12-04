package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashBinaryCommands {

  long hset(byte[] key, byte[] field, byte[] value);

  long hset(byte[] key, Map<byte[], byte[]> hash);

  byte[] hget(byte[] key, byte[] field);

  long hsetnx(byte[] key, byte[] field, byte[] value);

  String hmset(byte[] key, Map<byte[], byte[]> hash);

  List<byte[]> hmget(byte[] key, byte[]... fields);

  long hincrBy(byte[] key, byte[] field, long value);

  double hincrByFloat(byte[] key, byte[] field, double value);

  boolean hexists(byte[] key, byte[] field);

  long hdel(byte[] key, byte[]... field);

  long hlen(byte[] key);

  Set<byte[]> hkeys(byte[] key);

  List<byte[]> hvals(byte[] key);

  Map<byte[], byte[]> hgetAll(byte[] key);

  byte[] hrandfield(byte[] key);

  List<byte[]> hrandfield(byte[] key, long count);

  Map<byte[], byte[]> hrandfieldWithValues(byte[] key, long count);

  default ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params);

  long hstrlen(byte[] key, byte[] field);

}
