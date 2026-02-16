package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.Response;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.StreamDeletionPolicy;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.StreamEntryBinary;
import redis.clients.jedis.resps.StreamEntryDeletionResult;

public interface StreamPipelineBinaryCommands {

  default Response<byte[]> xadd(byte[] key, Map<byte[], byte[]> hash, XAddParams params) {
    return xadd(key, params, hash);
  }

  Response<byte[]> xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash);

  Response<Long> xlen(byte[] key);

  Response<List<Object>> xrange(byte[] key, byte[] start, byte[] end);

  Response<List<Object>> xrange(byte[] key, byte[] start, byte[] end, int count);

  Response<List<Object>> xrevrange(byte[] key, byte[] end, byte[] start);

  Response<List<Object>> xrevrange(byte[] key, byte[] end, byte[] start, int count);

  Response<Long> xack(byte[] key, byte[] group, byte[]... ids);

  Response<List<StreamEntryDeletionResult>> xackdel(byte[] key, byte[] group, byte[]... ids);

  Response<List<StreamEntryDeletionResult>> xackdel(byte[] key, byte[] group, StreamDeletionPolicy trimMode, byte[]... ids);

  Response<String> xgroupCreate(byte[] key, byte[] groupName, byte[] id, boolean makeStream);

  Response<String> xgroupSetID(byte[] key, byte[] groupName, byte[] id);

  Response<Long> xgroupDestroy(byte[] key, byte[] groupName);

  Response<Boolean> xgroupCreateConsumer(byte[] key, byte[] groupName, byte[] consumerName);

  Response<Long> xgroupDelConsumer(byte[] key, byte[] groupName, byte[] consumerName);

  Response<Long> xdel(byte[] key, byte[]... ids);

  Response<List<StreamEntryDeletionResult>> xdelex(byte[] key, byte[]... ids);

  Response<List<StreamEntryDeletionResult>> xdelex(byte[] key, StreamDeletionPolicy trimMode, byte[]... ids);

  Response<Long> xtrim(byte[] key, long maxLen, boolean approximateLength);

  Response<Long> xtrim(byte[] key, XTrimParams params);

  Response<Object> xpending(byte[] key, byte[] groupName);

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

  Response<List<Object>> xinfoGroups(byte[] key);

  Response<List<Object>> xinfoConsumers(byte[] key, byte[] group);

  /**
   * @deprecated As of Jedis 6.1.0, use {@link #xreadBinary(XReadParams, Map)} or
   *     {@link #xreadBinaryAsMap(XReadParams, Map)} for type safety and better stream entry
   *     parsing.
   */
  @Deprecated
  Response<List<Object>> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams);

  /**
   * @deprecated As of Jedis 6.1.0, use
   *     {@link #xreadGroupBinary(byte[], byte[], XReadGroupParams, Map)} or
   *     {@link #xreadGroupBinaryAsMap(byte[], byte[], XReadGroupParams, Map)} instead.
   */
  @Deprecated
  Response<List<Object>> xreadGroup(byte[] groupName, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams);


  /**
   * Read from one or more streams.
   *
   * @param xReadParams {@link XReadParams}
   * @param streams Map of stream name and ID to read from.
   * @return List of entries. Each entry in the list is a pair of stream name and the entries reported for that key.
   */
  Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> xreadBinary(XReadParams xReadParams,
      Map<byte[], StreamEntryID> streams);

  /**
   * Read from one or more streams and return a map of stream name to list of entries.
   *
   * @param xReadParams {@link XReadParams}
   * @param streams Map of stream name and ID to read from.
   * @return Map of stream name to list of entries.
   */
  Response<Map<byte[], List<StreamEntryBinary>>> xreadBinaryAsMap(XReadParams xReadParams,
      Map<byte[], StreamEntryID> streams);

  /**
   * Read from one or more streams using a consumer group.
   *
   * @param groupName Consumer group name
   * @param consumer Consumer name
   * @param xReadGroupParams {@link XReadGroupParams}
   * @param streams Map of stream name and ID to read from.
   * @return List of entries. Each entry in the list is a pair of stream name and the entries reported for that key.
   */
  Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> xreadGroupBinary(byte[] groupName, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map<byte[], StreamEntryID> streams);

  /**
   * Read from one or more streams using a consumer group and return a map of stream name to list of entries.
   *
   * @param groupName Consumer group name
   * @param consumer Consumer name
   * @param xReadGroupParams {@link XReadGroupParams}
   * @param streams Map of stream name and ID to read from.
   * @return Map of stream name to list of entries.
   */
  Response<Map<byte[], List<StreamEntryBinary>>> xreadGroupBinaryAsMap(byte[] groupName, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map<byte[], StreamEntryID> streams);

  /**
   * XCFGSET key [IDMP-DURATION duration] [IDMP-MAXSIZE maxsize]
   * Configure idempotent producer settings for a stream.
   *
   * @param key Stream name
   * @param params Configuration parameters
   * @return OK if successful
   */
  Response<byte[]> xcfgset(byte[] key, XCfgSetParams params);

}
