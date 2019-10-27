package redis.clients.jedis.commands;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MultiKeyBinaryJedisClusterCommands {
  Long del(byte[]... keys);

  Long unlink(byte[]... keys);

  Long exists(byte[]... keys);

  List<byte[]> blpop(int timeout, byte[]... keys);

  List<byte[]> brpop(int timeout, byte[]... keys);

  List<byte[]> mget(byte[]... keys);

  String mset(byte[]... keysvalues);

  Long msetnx(byte[]... keysvalues);

  String rename(byte[] oldkey, byte[] newkey);

  Long renamenx(byte[] oldkey, byte[] newkey);

  byte[] rpoplpush(byte[] srckey, byte[] dstkey);

  Set<byte[]> sdiff(byte[]... keys);

  Long sdiffstore(byte[] dstkey, byte[]... keys);

  Set<byte[]> sinter(byte[]... keys);

  Long sinterstore(byte[] dstkey, byte[]... keys);

  Long smove(byte[] srckey, byte[] dstkey, byte[] member);

  Long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey);

  Long sort(byte[] key, byte[] dstkey);

  Set<byte[]> sunion(byte[]... keys);

  Long sunionstore(byte[] dstkey, byte[]... keys);

  Long zinterstore(byte[] dstkey, byte[]... sets);

  Long zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  Long zunionstore(byte[] dstkey, byte[]... sets);

  Long zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

  byte[] brpoplpush(byte[] source, byte[] destination, int timeout);

  Long publish(byte[] channel, byte[] message);

  void subscribe(BinaryJedisPubSub jedisPubSub, byte[]... channels);

  void psubscribe(BinaryJedisPubSub jedisPubSub, byte[]... patterns);

  Long bitop(BitOP op, byte[] destKey, byte[]... srcKeys);

  String pfmerge(byte[] destkey, byte[]... sourcekeys);

  Long pfcount(byte[]... keys);

  Long touch(byte[]... keys);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params);

  Set<byte[]> keys(byte[] pattern);
  
  List<byte[]> xread(final int count, final long block, final Map<byte[], byte[]> streams);
  
  List<byte[]> xreadGroup(byte[] groupname, byte[] consumer, int count, long block, boolean noAck, Map<byte[], byte[]> streams);
}
