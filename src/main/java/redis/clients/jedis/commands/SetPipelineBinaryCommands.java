package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Response;
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

  Response<Set<byte[]>> sinter(byte[]... keys);

  Response<Long> sinterstore(byte[] dstkey, byte[]... keys);

  Response<Long> sintercard(byte[]... keys);

  Response<Long> sintercard(int limit, byte[]... keys);

  Response<Set<byte[]>> sunion(byte[]... keys);

  Response<Long> sunionstore(byte[] dstkey, byte[]... keys);

  Response<Long> smove(byte[] srckey, byte[] dstkey, byte[] member);

}
