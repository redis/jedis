package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetBinaryCommands {

  long sadd(byte[] key, byte[]... member);

  Set<byte[]> smembers(byte[] key);

  long srem(byte[] key, byte[]... member);

  byte[] spop(byte[] key);

  Set<byte[]> spop(byte[] key, long count);

  long scard(byte[] key);

  boolean sismember(byte[] key, byte[] member);

  List<Boolean> smismember(byte[] key, byte[]... members);

  byte[] srandmember(byte[] key);

  List<byte[]> srandmember(byte[] key, int count);

  default ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params);

}
