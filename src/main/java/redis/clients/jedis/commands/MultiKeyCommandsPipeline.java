package redis.clients.jedis.commands;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.params.MigrateParams;

import java.util.List;
import java.util.Set;

/**
 * Multikey related commands (these are split out because they are non-shardable)
 */
public interface MultiKeyCommandsPipeline {
  Response<Long> del(String... keys);

  Response<Long> unlink(String... keys);

  Response<Long> exists(String... keys);

  Response<List<String>> blpop(String... args);

  Response<List<String>> brpop(String... args);

  Response<Set<String>> keys(String pattern);

  Response<List<String>> mget(String... keys);

  Response<String> mset(String... keysvalues);

  Response<Long> msetnx(String... keysvalues);

  Response<String> rename(String oldkey, String newkey);

  Response<Long> renamenx(String oldkey, String newkey);

  Response<String> rpoplpush(String srckey, String dstkey);

  Response<Set<String>> sdiff(String... keys);

  Response<Long> sdiffstore(String dstkey, String... keys);

  Response<Set<String>> sinter(String... keys);

  Response<Long> sinterstore(String dstkey, String... keys);

  Response<Long> smove(String srckey, String dstkey, String member);

  Response<Long> sort(String key, SortingParams sortingParameters, String dstkey);

  Response<Long> sort(String key, String dstkey);

  Response<Set<String>> sunion(String... keys);

  Response<Long> sunionstore(String dstkey, String... keys);

  Response<String> watch(String... keys);

  Response<String> unwatch();

  Response<Long> zinterstore(String dstkey, String... sets);

  Response<Long> zinterstore(String dstkey, ZParams params, String... sets);

  Response<Long> zunionstore(String dstkey, String... sets);

  Response<Long> zunionstore(String dstkey, ZParams params, String... sets);

  Response<String> brpoplpush(String source, String destination, int timeout);

  Response<Long> publish(String channel, String message);

  Response<String> randomKey();

  Response<Long> bitop(BitOP op, String destKey, String... srcKeys);

  Response<String> pfmerge(String destkey, String... sourcekeys);

  Response<Long> pfcount(String... keys);

  Response<Long> touch(String... keys);

  Response<String> migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, String... keys);
}
