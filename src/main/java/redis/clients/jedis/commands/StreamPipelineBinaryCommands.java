package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.*;

public interface StreamPipelineBinaryCommands {
//
//  byte[] xadd(byte[] key, byte[] id, Map<byte[], byte[]> hash, long maxLen, boolean approximateLength);

  default Response<byte[]> xadd(byte[] key, Map<byte[], byte[]> hash, XAddParams params) {
    return xadd(key, params, hash);
  }

  Response<byte[]> xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash);

  Response<Long> xlen(byte[] key);

  Response<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end);

  Response<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end, int count);

  Response<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start);

  Response<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start, int count);

  Response<Long> xack(byte[] key, byte[] group, byte[]... ids);

  Response<String> xgroupCreate(byte[] key, byte[] groupName, byte[] id, boolean makeStream);

  Response<String> xgroupSetID(byte[] key, byte[] groupName, byte[] id);

  Response<Long> xgroupDestroy(byte[] key, byte[] groupName);

  Response<Boolean> xgroupCreateConsumer(byte[] key, byte[] groupName, byte[] consumerName);

  Response<Long> xgroupDelConsumer(byte[] key, byte[] groupName, byte[] consumerName);

  Response<Long> xdel(byte[] key, byte[]... ids);

  Response<Long> xtrim(byte[] key, long maxLen, boolean approximateLength);

  Response<Long> xtrim(byte[] key, XTrimParams params);

  Response<Object> xpending(byte[] key, byte[] groupName);

  /**
   * @deprecated Use {@link StreamPipelineBinaryCommands#xpending(byte[], byte[], redis.clients.jedis.params.XPendingParams)}.
   */
  @Deprecated
  Response<List<Object>> xpending(byte[] key, byte[] groupName, byte[] start, byte[] end, int count, byte[] consumerName);

  Response<List<Object>> xpending(byte[] key, byte[] groupName, XPendingParams params);

  Response<List<byte[]>> xclaim(byte[] key, byte[] group, byte[] consumerName, long minIdleTime, XClaimParams params, byte[]... ids);

  Response<List<byte[]>> xclaimJustId(byte[] key, byte[] group, byte[] consumerName, long minIdleTime, XClaimParams params, byte[]... ids);

  Response<List<Object>> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params);

  Response<List<Object>> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params);

  Response<Object> xinfoStream(byte[] key);

  /**
   * Introspection command used in order to retrieve all information about the stream
   * @param key Stream name
   */
  Response<Object> xinfoStreamFull(byte[] key);

  /**
   * Introspection command used in order to retrieve all information about the stream
   * @param key Stream name
   * @param count stream info count
   */
  Response<Object> xinfoStreamFull(byte[] key, int count);

  /**
   * @deprecated Use {@link StreamPipelineBinaryCommands#xinfoGroups(byte[])}.
   */
  @Deprecated
  Response<List<Object>> xinfoGroup(byte[] key);

  Response<List<Object>> xinfoGroups(byte[] key);

  Response<List<Object>> xinfoConsumers(byte[] key, byte[] group);

  Response<List<byte[]>> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams);

  Response<List<byte[]>> xreadGroup(byte[] groupName, byte[] consumer, XReadGroupParams xReadGroupParams,
      Map.Entry<byte[], byte[]>... streams);

}
