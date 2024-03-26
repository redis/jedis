package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;

public interface StreamCommands {

  /**
   * XADD key ID field string [field string ...]
   *
   * @return the ID of the added entry
   */
  StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash);

  /**
   * XADD key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|ID field value [field value ...]
   *
   * @return the ID of the added entry
   */
  // Legacy
  default StreamEntryID xadd(String key, Map<String, String> hash, XAddParams params) {
    return xadd(key, params, hash);
  }

  StreamEntryID xadd(String key, XAddParams params, Map<String, String> hash);

  /**
   * XLEN key
   *
   * @return length of stream
   */
  long xlen(String key);

  /**
   * XRANGE key start end
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate maximum ID possible in the stream
   * @return The entries with IDs matching the specified range.
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end);

  /**
   * XRANGE key start end COUNT count
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate maximum ID possible in the stream
   * @param count maximum number of entries returned
   * @return The entries with IDs matching the specified range.
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count);

  /**
   * XREVRANGE key end start
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate maximum ID possible in the stream
   * @return the entries with IDs matching the specified range, from the higher ID to the lower ID matching.
   */
  List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start);

  /**
   * XREVRANGE key end start COUNT count
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing {@code null} will
   * indicate maximum ID possible in the stream
   * @param count The entries with IDs matching the specified range.
   * @return the entries with IDs matching the specified range, from the higher ID to the lower ID matching.
   */
  List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count);

  List<StreamEntry> xrange(String key, String start, String end);

  List<StreamEntry> xrange(String key, String start, String end, int count);

  List<StreamEntry> xrevrange(String key, String end, String start);

  List<StreamEntry> xrevrange(String key, String end, String start, int count);

  /**
   * XACK key group ID [ID ...]
   */
  long xack(String key, String group, StreamEntryID... ids);

  /**
   * {@code XGROUP CREATE key groupName <id or $>}
   */
  String xgroupCreate(String key, String groupName, StreamEntryID id, boolean makeStream);

  /**
   * {@code XGROUP SETID key groupName <id or $>}
   */
  String xgroupSetID(String key, String groupName, StreamEntryID id);

  /**
   * XGROUP DESTROY key groupName
   */
  long xgroupDestroy(String key, String groupName);

  /**
   * XGROUP CREATECONSUMER key groupName consumerName
   */
  boolean xgroupCreateConsumer(String key, String groupName, String consumerName);

  /**
   * XGROUP DELCONSUMER key groupName consumerName
   */
  long xgroupDelConsumer(String key, String groupName, String consumerName);

  /**
   * XDEL key ID [ID ...]
   */
  long xdel(String key, StreamEntryID... ids);

  /**
   * XTRIM key MAXLEN [~] count
   */
  long xtrim(String key, long maxLen, boolean approximate);

  /**
   * XTRIM key MAXLEN|MINID [=|~] threshold [LIMIT count]
   */
  long xtrim(String key, XTrimParams params);

  /**
   * XPENDING key group
   */
  StreamPendingSummary xpending(String key, String groupName);

  /**
   * XPENDING key group [[IDLE min-idle-time] start end count [consumer]]
   */
  List<StreamPendingEntry> xpending(String key, String groupName, XPendingParams params);

  /**
   * {@code XCLAIM key group consumer min-idle-time <ID-1> ... <ID-N>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE]}
   */
  List<StreamEntry> xclaim(String key, String group, String consumerName, long minIdleTime,
      XClaimParams params, StreamEntryID... ids);

  /**
   * {@code XCLAIM key group consumer min-idle-time <ID-1> ... <ID-N>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE] JUSTID}
   */
  List<StreamEntryID> xclaimJustId(String key, String group, String consumerName, long minIdleTime,
      XClaimParams params, StreamEntryID... ids);

  /**
   * XAUTOCLAIM key group consumer min-idle-time start [COUNT count]
   *
   * @param key Stream Key
   * @param group Consumer Group
   * @param consumerName Consumer name to transfer the auto claimed entries
   * @param minIdleTime Entries pending more than minIdleTime will be transferred ownership
   * @param start {@link StreamEntryID} - Entries &ge; start will be transferred ownership, passing
   * {@code null} will indicate '-'
   * @param params {@link XAutoClaimParams}
   */
  Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params);

  /**
   * XAUTOCLAIM key group consumer min-idle-time start [COUNT count] JUSTID
   *
   * @param key Stream Key
   * @param group Consumer Group
   * @param consumerName Consumer name to transfer the auto claimed entries
   * @param minIdleTime Entries pending more than minIdleTime will be transferred ownership
   * @param start {@link StreamEntryID} - Entries &ge; start will be transferred ownership, passing
   * {@code null} will indicate '-'
   * @param params {@link XAutoClaimParams}
   */
  Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params);

  /**
   * Introspection command used in order to retrieve different information about the stream
   * @param key Stream name
   * @return {@link StreamInfo} that contains information about the stream
   */
  StreamInfo xinfoStream(String key);

  /**
   * Introspection command used in order to retrieve all information about the stream
   * @param key Stream name
   * @return {@link StreamFullInfo} that contains information about the stream
   */
  StreamFullInfo xinfoStreamFull(String key);

  /**
   * Introspection command used in order to retrieve all information about the stream
   * @param key Stream name
   * @param count stream info count
   * @return {@link StreamFullInfo} that contains information about the stream
   */
  StreamFullInfo xinfoStreamFull(String key, int count);

  /**
   * Introspection command used in order to retrieve different information about groups in the stream
   * @param key Stream name
   * @return List of {@link StreamGroupInfo} containing information about groups
   */
  List<StreamGroupInfo> xinfoGroups(String key);

  /**
   * Introspection command used in order to retrieve different information about consumers in the group
   * @param key Stream name
   * @param group Group name
   * @return List of {@link StreamConsumersInfo} containing information about consumers that belong
   * to the group
   * @deprecated Use {@link #xinfoConsumers2(java.lang.String, java.lang.String)}.
   */
  @Deprecated // keep it till at least Jedis 6/7
  List<StreamConsumersInfo> xinfoConsumers(String key, String group);

  /**
   * Introspection command used in order to retrieve different information about consumers in the group
   * @param key Stream name
   * @param group Group name
   * @return List of {@link StreamConsumerInfo} containing information about consumers that belong
   * to the group
   */
  List<StreamConsumerInfo> xinfoConsumers2(String key, String group);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   */
  List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams,
      Map<String, StreamEntryID> streams);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   */
  Map<String, List<StreamEntry>> xreadAsMap(XReadParams xReadParams,
      Map<String, StreamEntryID> streams);

  /**
   * XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] id [id ...]
   */
  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupName, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams);

  /**
   * XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] id [id ...]
   */
  Map<String, List<StreamEntry>> xreadGroupAsMap(String groupName, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams);

}
