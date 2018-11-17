package redis.clients.jedis;

import redis.clients.jedis.params.stream.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MultiKeyPipelineBase extends PipelineBase implements BasicRedisPipeline,
    MultiKeyBinaryRedisPipeline, MultiKeyCommandsPipeline, ClusterPipeline,
    BinaryScriptingCommandsPipeline, StreamCommandsPipline{

  protected Client client = null;

  @Override
  public Response<List<String>> brpop(String... args) {
    client.brpop(args);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<String>> brpop(int timeout, String... keys) {
    client.brpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<String>> blpop(String... args) {
    client.blpop(args);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<String>> blpop(int timeout, String... keys) {
    client.blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<Map<String, String>> blpopMap(int timeout, String... keys) {
    client.blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  @Override
  public Response<List<byte[]>> brpop(byte[]... args) {
    client.brpop(args);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<List<String>> brpop(int timeout, byte[]... keys) {
    client.brpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<Map<String, String>> brpopMap(int timeout, String... keys) {
    client.blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  @Override
  public Response<List<byte[]>> blpop(byte[]... args) {
    client.blpop(args);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<List<String>> blpop(int timeout, byte[]... keys) {
    client.blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<Long> del(String... keys) {
    client.del(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> del(byte[]... keys) {
    client.del(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> exists(String... keys) {
    client.exists(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> exists(byte[]... keys) {
    client.exists(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> keys(String pattern) {
    getClient(pattern).keys(pattern);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> keys(byte[] pattern) {
    getClient(pattern).keys(pattern);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<List<String>> mget(String... keys) {
    client.mget(keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> mget(byte[]... keys) {
    client.mget(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<String> mset(String... keysvalues) {
    client.mset(keysvalues);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> mset(byte[]... keysvalues) {
    client.mset(keysvalues);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> msetnx(String... keysvalues) {
    client.msetnx(keysvalues);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> msetnx(byte[]... keysvalues) {
    client.msetnx(keysvalues);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> rename(String oldkey, String newkey) {
    client.rename(oldkey, newkey);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> rename(byte[] oldkey, byte[] newkey) {
    client.rename(oldkey, newkey);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> renamenx(String oldkey, String newkey) {
    client.renamenx(oldkey, newkey);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> renamenx(byte[] oldkey, byte[] newkey) {
    client.renamenx(oldkey, newkey);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> rpoplpush(String srckey, String dstkey) {
    client.rpoplpush(srckey, dstkey);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> rpoplpush(byte[] srckey, byte[] dstkey) {
    client.rpoplpush(srckey, dstkey);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Set<String>> sdiff(String... keys) {
    client.sdiff(keys);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> sdiff(byte[]... keys) {
    client.sdiff(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Long> sdiffstore(String dstkey, String... keys) {
    client.sdiffstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sdiffstore(byte[] dstkey, byte[]... keys) {
    client.sdiffstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> sinter(String... keys) {
    client.sinter(keys);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> sinter(byte[]... keys) {
    client.sinter(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Long> sinterstore(String dstkey, String... keys) {
    client.sinterstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sinterstore(byte[] dstkey, byte[]... keys) {
    client.sinterstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> smove(String srckey, String dstkey, String member) {
    client.smove(srckey, dstkey, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> smove(byte[] srckey, byte[] dstkey, byte[] member) {
    client.smove(srckey, dstkey, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sort(String key, SortingParams sortingParameters, String dstkey) {
    client.sort(key, sortingParameters, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
    client.sort(key, sortingParameters, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sort(String key, String dstkey) {
    client.sort(key, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sort(byte[] key, byte[] dstkey) {
    client.sort(key, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> sunion(String... keys) {
    client.sunion(keys);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> sunion(byte[]... keys) {
    client.sunion(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Long> sunionstore(String dstkey, String... keys) {
    client.sunionstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sunionstore(byte[] dstkey, byte[]... keys) {
    client.sunionstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> watch(String... keys) {
    client.watch(keys);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> watch(byte[]... keys) {
    client.watch(keys);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> zinterstore(String dstkey, String... sets) {
    client.zinterstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zinterstore(byte[] dstkey, byte[]... sets) {
    client.zinterstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zinterstore(String dstkey, ZParams params, String... sets) {
    client.zinterstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    client.zinterstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zunionstore(String dstkey, String... sets) {
    client.zunionstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zunionstore(byte[] dstkey, byte[]... sets) {
    client.zunionstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zunionstore(String dstkey, ZParams params, String... sets) {
    client.zunionstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
    client.zunionstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> bgrewriteaof() {
    client.bgrewriteaof();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> bgsave() {
    client.bgsave();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<List<String>> configGet(String pattern) {
    client.configGet(pattern);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<String> configSet(String parameter, String value) {
    client.configSet(parameter, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> brpoplpush(String source, String destination, int timeout) {
    client.brpoplpush(source, destination, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout) {
    client.brpoplpush(source, destination, timeout);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<String> configResetStat() {
    client.configResetStat();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> save() {
    client.save();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> lastsave() {
    client.lastsave();
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> publish(String channel, String message) {
    client.publish(channel, message);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> publish(byte[] channel, byte[] message) {
    client.publish(channel, message);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> randomKey() {
    client.randomKey();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> randomKeyBinary() {
    client.randomKey();
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<String> flushDB() {
    client.flushDB();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> flushAll() {
    client.flushAll();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> info() {
    client.info();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> info(final String section) {
    client.info(section);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<List<String>> time() {
    client.time();
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<Long> dbSize() {
    client.dbSize();
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> shutdown() {
    client.shutdown();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> ping() {
    client.ping();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> select(int index) {
    client.select(index);
    Response<String> response = getResponse(BuilderFactory.STRING);
    client.setDb(index);

    return response;
  }

  @Override
  public Response<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    client.bitop(op, destKey, srcKeys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    client.bitop(op, destKey, srcKeys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> clusterNodes() {
    client.clusterNodes();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> clusterMeet(final String ip, final int port) {
    client.clusterMeet(ip, port);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> clusterAddSlots(final int... slots) {
    client.clusterAddSlots(slots);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> clusterDelSlots(final int... slots) {
    client.clusterDelSlots(slots);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> clusterInfo() {
    client.clusterInfo();
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<List<String>> clusterGetKeysInSlot(final int slot, final int count) {
    client.clusterGetKeysInSlot(slot, count);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<String> clusterSetSlotNode(final int slot, final String nodeId) {
    client.clusterSetSlotNode(slot, nodeId);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> clusterSetSlotMigrating(final int slot, final String nodeId) {
    client.clusterSetSlotMigrating(slot, nodeId);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> clusterSetSlotImporting(final int slot, final String nodeId) {
    client.clusterSetSlotImporting(slot, nodeId);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Object> eval(byte[] script) {
    return this.eval(script, 0);
  }

  @Override
  public Response<Object> eval(byte[] script, byte[] keyCount, byte[]... params) {
    getClient(script).eval(script, keyCount, params);
    return getResponse(BuilderFactory.EVAL_BINARY_RESULT);
  }

  @Override
  public Response<Object> eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
    byte[][] argv = BinaryJedis.getParamsWithBinary(keys, args);
    return this.eval(script, keys.size(), argv);
  }

  @Override
  public Response<Object> eval(byte[] script, int keyCount, byte[]... params) {
    getClient(script).eval(script, keyCount, params);
    return getResponse(BuilderFactory.EVAL_BINARY_RESULT);
  }

  @Override
  public Response<Object> evalsha(byte[] sha1) {
    return this.evalsha(sha1, 0);
  }

  @Override
  public Response<Object> evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    byte[][] argv = BinaryJedis.getParamsWithBinary(keys, args);
    return this.evalsha(sha1, keys.size(), argv);
  }

  @Override
  public Response<Object> evalsha(byte[] sha1, int keyCount, byte[]... params) {
    getClient(sha1).evalsha(sha1, keyCount, params);
    return getResponse(BuilderFactory.EVAL_BINARY_RESULT);
  }

  @Override
  public Response<String> pfmerge(byte[] destkey, byte[]... sourcekeys) {
    client.pfmerge(destkey, sourcekeys);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> pfmerge(String destkey, String... sourcekeys) {
    client.pfmerge(destkey, sourcekeys);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> pfcount(String... keys) {
    client.pfcount(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(final byte[]... keys) {
    client.pfcount(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> xaddDefault(String key, String... pairs) {
    return xadd(key,"*", pairs);
  }

  @Override
  public Response<String> xaddDefault(String key, Map<String, String> pairs) {
    return xadd(key,"*", pairs);
  }

  @Override
  public Response<String> xadd(String key, String entryId, String... pairs) {
    client.xadd(key, entryId, pairs);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> xadd(String key, String entryId, Map<String, String> pairs) {
    client.xadd(key, entryId, pairs);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> xaddWithMaxlen(String key, long maxLen, String entryId, String... pairs) {
    return xaddWithMaxlen(key,false, maxLen, entryId, pairs);
  }

  @Override
  public Response<String> xaddWithMaxlen(String key, long maxLen, String entryId, Map<String, String> pairs) {
    return xaddWithMaxlen(key, false, maxLen, entryId, pairs);
  }

  @Override
  public Response<String> xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, String... pairs) {
    client.xadd(key, approx, maxLen, entryId, pairs);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, Map<String, String> pairs) {
    client.xadd(key, approx, maxLen, entryId, pairs);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> xlen(String key) {
    client.xlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<StreamParams>> xrange(String key, String startEntryId, String endEntryId) {
    return xrange(key, startEntryId, endEntryId, 0);
  }

  @Override
  public Response<List<StreamParams>> xrange(String key, String startEntryId, String endEntryId, long count) {
    client.xrange(key, startEntryId, endEntryId, count);
    return getResponse(BuilderFactory.STREAM_PARAMS_LIST);
  }

  @Override
  public Response<List<StreamParams>> xrevrange(String key, String startEntryId, String endEntryId) {
    return xrevrange(key, startEntryId, endEntryId,0);
  }

  @Override
  public Response<List<StreamParams>> xrevrange(String key, String startEntryId, String endEntryId, long count) {
    client.xrevrange(key, startEntryId, endEntryId, count);
    return getResponse(BuilderFactory.STREAM_PARAMS_LIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xread(String... params) {
    client.xread(params);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xread(Map<String, String> pairs) {
    client.xread(pairs);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xread(long count, String... params) {
    client.xread(count, params);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xread(long count, Map<String, String> pairs) {
    client.xread(count, pairs);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Long> xdel(String key, String entryId) {
    client.xdel(key, entryId);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> xtrimWithMaxlen(String key, long maxlen) {
    return xtrimWithMaxlen(key, false, maxlen);
  }

  @Override
  public Response<Long> xtrimWithMaxlen(String key, boolean approx, long maxlen) {
    client.xtrimWithMaxlen(key, approx, maxlen);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> xgroupcreate(String key, String group, String entryId) {
    return xgroupcreate(key, group, entryId, false);
  }

  @Override
  public Response<String> xgroupcreate(String key, String group, String entryId, boolean noack) {
    client.xgroupcreate(key, group, entryId, noack);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> xgroupsetid(String key, String group, String entryId) {
    client.xgroupsetid(key, group, entryId);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> xgroupdestroy(String key, String group) {
    client.xgroupdestroy(key, group);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> xgroupdelconsumer(String key, String group, String consumer) {
    client.xgroupdelconsumer(key, group, consumer);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<StreamInfo> xinfostream(String key) {
    client.xinfostream(key);
    return getResponse(BuilderFactory.STREAM_INFO);
  }

  @Override
  public Response<List<GroupInfo>> xinfogroups(String key) {
    client.xinfogroups(key);
    return getResponse(BuilderFactory.GROUP_INFO_LIST);
  }

  @Override
  public Response<List<ConsumerInfo>> xinfoconsumers(String key, String group) {
    client.xinfoconsumers(key, group);
    return getResponse(BuilderFactory.CONSUMER_INFO_LIST);
  }

  @Override
  public Response<GroupPendingInfo> xpending(String key, String group) {
    client.xpending(key, group);
    return getResponse(BuilderFactory.GROUP_PENDING_INFO);
  }

  @Override
  public Response<List<PendingInfo>> xpending(String key, String group, String startEntryId, String endEntryId, long count) {
    return xpending(key, group, startEntryId, endEntryId, count, null);
  }

  @Override
  public Response<List<PendingInfo>> xpending(String key, String group, String startEntryId, String endEntryId, long count, String consumer) {
    client.xpending(key, group, startEntryId, endEntryId, count, consumer);
    return getResponse(BuilderFactory.PENDING_INFO_LIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, String... params) {
    client.xreadgroup(group, consumer, params);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, Map<String, String> pairs) {
    client.xreadgroup(group, consumer, pairs);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, long count, String... params) {
    client.xreadgroup(group, consumer, count, params);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, long count, Map<String, String> pairs) {
    client.xreadgroup(group, consumer, count, pairs);
    return getResponse(BuilderFactory.STREAM_PARAMS_MAPLIST);
  }

  @Override
  public Response<Long> xack(String key, String group, String... entryIds) {
    client.xack(key, group, entryIds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, String... entryIds) {
    return xclaim(key, group, consumer, minIdleTime, 0, entryIds);
  }

  @Override
  public Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds) {
    return xclaim(key, group, consumer, minIdleTime, idleTime, -1, entryIds);
  }

  @Override
  public Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds) {
    client.xclaim(false, key, group, consumer, minIdleTime, idleTime, retryCount, entryIds);
    return getResponse(BuilderFactory.STREAM_PARAMS_LIST);
  }

  @Override
  public Response<List<StreamParams>> xclaimForce(String key, String group, String consumer, String... entryIds) {
    client.xclaimForce(false, key, group, consumer, entryIds);
    return getResponse(BuilderFactory.STREAM_PARAMS_LIST);
  }

  @Override
  public Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, String... entryIds) {
    return xclaimJustid(key, group, consumer, minIdleTime, 0, entryIds);
  }

  @Override
  public Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds) {
    return xclaimJustid(key, group, consumer, minIdleTime, idleTime, -1, entryIds);
  }

  @Override
  public Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds) {
    client.xclaim(true, key, group, consumer, minIdleTime, idleTime, retryCount, entryIds);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<String>> xclaimForceAndJustid(String key, String group, String consumer, String... entryIds) {
    client.xclaimForce(true, key, group, consumer, entryIds);
    return getResponse(BuilderFactory.STRING_LIST);
  }
}
