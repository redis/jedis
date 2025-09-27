package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.StreamDeletionPolicy;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.StreamEntryBinary;
import redis.clients.jedis.resps.StreamEntryDeletionResult;

public interface StreamBinaryCommands {

  default byte[] xadd(byte[] key, Map<byte[], byte[]> hash, XAddParams params) {
    return xadd(key, params, hash);
  }

  byte[] xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash);

  long xlen(byte[] key);

  List<Object> xrange(byte[] key, byte[] start, byte[] end);

  List<Object> xrange(byte[] key, byte[] start, byte[] end, int count);

  List<Object> xrevrange(byte[] key, byte[] end, byte[] start);

  List<Object> xrevrange(byte[] key, byte[] end, byte[] start, int count);

  long xack(byte[] key, byte[] group, byte[]... ids);

  /**
   * XACKDEL key group [KEEPREF | DELREF | ACKED] IDS numids id [id ...]
   */
  List<StreamEntryDeletionResult> xackdel(byte[] key, byte[] group, byte[]... ids);

  /**
   * XACKDEL key group [KEEPREF | DELREF | ACKED] IDS numids id [id ...]
   */
  List<StreamEntryDeletionResult> xackdel(byte[] key, byte[] group, StreamDeletionPolicy trimMode, byte[]... ids);

  String xgroupCreate(byte[] key, byte[] groupName, byte[] id, boolean makeStream);

  String xgroupSetID(byte[] key, byte[] groupName, byte[] id);

  long xgroupDestroy(byte[] key, byte[] groupName);

  boolean xgroupCreateConsumer(byte[] key, byte[] groupName, byte[] consumerName);

  long xgroupDelConsumer(byte[] key, byte[] groupName, byte[] consumerName);

  long xdel(byte[] key, byte[]... ids);

  /**
   * XDELEX key [KEEPREF | DELREF | ACKED] IDS numids id [id ...]
   */
  List<StreamEntryDeletionResult> xdelex(byte[] key, byte[]... ids);

  /**
   * XDELEX key [KEEPREF | DELREF | ACKED] IDS numids id [id ...]
   */
  List<StreamEntryDeletionResult> xdelex(byte[] key, StreamDeletionPolicy trimMode, byte[]... ids);

  long xtrim(byte[] key, long maxLen, boolean approximateLength);

  long xtrim(byte[] key, XTrimParams params);

  Object xpending(byte[] key, byte[] groupName);

  List<Object> xpending(byte[] key, byte[] groupName, XPendingParams params);

  List<byte[]> xclaim(byte[] key, byte[] group, byte[] consumerName, long minIdleTime, XClaimParams params, byte[]... ids);

  List<byte[]> xclaimJustId(byte[] key, byte[] group, byte[] consumerName, long minIdleTime, XClaimParams params, byte[]... ids);

  List<Object> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params);

  List<Object> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params);

  Object xinfoStream(byte[] key);

  /**
   * Introspection command used in order to retrieve all information about the stream
   * @param key Stream name
   */
  Object xinfoStreamFull(byte[] key);

  /**
   * Introspection command used in order to retrieve all information about the stream
   * @param key Stream name
   * @param count stream info count
   */
  Object xinfoStreamFull(byte[] key, int count);

  List<Object> xinfoGroups(byte[] key);

  List<Object> xinfoConsumers(byte[] key, byte[] group);

  /**
   * @deprecated As of Jedis 6.1.0, replaced by {@link #xreadBinary(XReadParams, Map)} or
   * {@link #xreadBinaryAsMap(XReadParams, Map)} for type safety and better stream entry parsing.
   */
  @Deprecated
  List<Object> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams);

  /**
   * @deprecated As of Jedis 6.1.0, use {@link #xreadGroupBinary(byte[], byte[], XReadGroupParams, Map)} or
   * {@link #xreadGroupBinaryAsMap(byte[], byte[], XReadGroupParams, Map)} instead.
   */
  @Deprecated
  List<Object> xreadGroup(byte[] groupName, byte[] consumer, XReadGroupParams xReadGroupParams,
      Map.Entry<byte[], byte[]>... streams);

  /**
   * Read from one or more streams.
   * @param xReadParams {@link XReadParams}
   * @param streams Map of stream name and ID to read from.
   * @return List of entries. Each entry in the list is a pair of stream name and the entries
   *     reported for that key.
   */
  List<Map.Entry<byte[], List<StreamEntryBinary>>> xreadBinary(XReadParams xReadParams,
      Map<byte[], StreamEntryID> streams);

  /**
   * Read from one or more streams and return a map of stream name to list of entries.
   * @param xReadParams {@link XReadParams}
   * @param streams Map of stream name and ID to read from.
   * @return Map of stream name to list of entries. key is the stream name and value is the list of
   *     entries reported for that key.
   */
  Map<byte[], List<StreamEntryBinary>> xreadBinaryAsMap(XReadParams xReadParams,
      Map<byte[], StreamEntryID> streams);

  /**
   * Read from one or more streams as a consumer group.
   * @param groupName Consumer group name.
   * @param consumer Consumer name.
   * @param xReadGroupParams {@link XReadGroupParams}
   * @param streams Map of stream name and ID to read from.
   * @return List of entries. Each entry in the list is a pair of stream name and the entries
   *     reported for that key.
   */
  List<Map.Entry<byte[], List<StreamEntryBinary>>> xreadGroupBinary(byte[] groupName,
      byte[] consumer, XReadGroupParams xReadGroupParams, Map<byte[], StreamEntryID> streams);

  /**
   * Read from one or more streams as a consumer group and return a map of stream name to list of
   * entries.
   * @param groupName Consumer group name.
   * @param consumer Consumer name.
   * @param xReadGroupParams {@link XReadGroupParams}
   * @param streams Map of stream name and ID to read from.
   * @return Map of stream name to list of entries. key is the stream name and value is the list of
   *     entries reported for that key.
   */
  Map<byte[], List<StreamEntryBinary>> xreadGroupBinaryAsMap(byte[] groupName, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map<byte[], StreamEntryID> streams);

}
