package redis.clients.jedis.commands;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;

import java.util.List;
import java.util.Set;

public interface MultiKeyJedisClusterCommands {
  Long del(String... keys);

  Long unlink(String... keys);

  Long exists(String... keys);

  List<String> blpop(int timeout, String... keys);

  List<String> brpop(int timeout, String... keys);

  List<String> mget(String... keys);

  String mset(String... keysvalues);

  Long msetnx(String... keysvalues);

  String rename(String oldkey, String newkey);

  Long renamenx(String oldkey, String newkey);

  String rpoplpush(String srckey, String dstkey);

  Set<String> sdiff(String... keys);

  Long sdiffstore(String dstkey, String... keys);

  Set<String> sinter(String... keys);

  Long sinterstore(String dstkey, String... keys);

  Long smove(String srckey, String dstkey, String member);

  Long sort(String key, SortingParams sortingParameters, String dstkey);

  Long sort(String key, String dstkey);

  Set<String> sunion(String... keys);

  Long sunionstore(String dstkey, String... keys);

  Long zinterstore(String dstkey, String... sets);

  Long zinterstore(String dstkey, ZParams params, String... sets);

  Long zunionstore(String dstkey, String... sets);

  Long zunionstore(String dstkey, ZParams params, String... sets);

  String brpoplpush(String source, String destination, int timeout);

  Long publish(String channel, String message);

  void subscribe(JedisPubSub jedisPubSub, String... channels);

  void psubscribe(JedisPubSub jedisPubSub, String... patterns);

  Long bitop(BitOP op, String destKey, String... srcKeys);

  String pfmerge(String destkey, String... sourcekeys);

  long pfcount(String... keys);

  Long touch(String... keys);

  ScanResult<String> scan(String cursor, ScanParams params);

  Set<String> keys(String pattern);
}
