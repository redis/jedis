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
   * @param key
   * @param id
   * @param hash
   * @return the ID of the added entry
   */
  StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash);

  /**
   * XADD key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|ID field value [field value ...]
   *
   * @param key
   * @param hash
   * @param params
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
   * @param key
   * @return length of stream
   */
  long xlen(String key);

  /**
   * XRANGE key start end
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @return The entries with IDs matching the specified range.
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end);

  /**
   * XRANGE key start end COUNT count
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @param count maximum number of entries returned
   * @return The entries with IDs matching the specified range.
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count);

  /**
   * XREVRANGE key end start
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @return the entries with IDs matching the specified range, from the higher ID to the lower ID matching.
   */
  List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start);

  /**
   * XREVRANGE key end start COUNT count
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
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
   *
   * @param key
   * @param group
   * @param ids
   */
  long xack(String key, String group, StreamEntryID... ids);

  /**
   * XGROUP CREATE <key> <groupName> <id or $>
   *
   * @param key
   * @param groupName
   * @param id
   * @param makeStream
   */
  String xgroupCreate( String key, String groupName, StreamEntryID id, boolean makeStream);

  /**
   * XGROUP SETID <key> <groupName> <id or $>
   *
   * @param key
   * @param groupName
   * @param id
   */
  String xgroupSetID( String key, String groupName, StreamEntryID id);

  /**
   * XGROUP DESTROY <key> <groupName>
   *
   * @param key
   * @param groupName
   */
  long xgroupDestroy(String key, String groupName);

  /**
   * XGROUP CREATECONSUMER <key> <groupName> <consumerName>
   * @param key
   * @param groupName
   * @param consumerName
   */
  boolean xgroupCreateConsumer(String key, String groupName, String consumerName);

  /**
   * XGROUP DELCONSUMER <key> <groupName> <consumerName>
   * @param key
   * @param groupName
   * @param consumerName
   */
  long xgroupDelConsumer(String key, String groupName, String consumerName);

  /**
   * XPENDING key group
   *
   * @param key
   * @param groupName
   */
  StreamPendingSummary xpending(String key, String groupName);

  /**
   * XPENDING key group [start end count] [consumer]
   *
   * @param key
   * @param groupName
   * @param start
   * @param end
   * @param count
   * @param consumerName
   * @deprecated Use {@link StreamCommands#xpending(java.lang.String, java.lang.String, redis.clients.jedis.params.XPendingParams)}.
   */
  @Deprecated
  List<StreamPendingEntry> xpending(String key, String groupName, StreamEntryID start,
      StreamEntryID end, int count, String consumerName);

  /**
   * XPENDING key group [[IDLE min-idle-time] start end count [consumer]]
   *
   * @param key
   * @param groupName
   * @param params
   */
  List<StreamPendingEntry> xpending(String key, String groupName, XPendingParams params);

  /**
   * XDEL key ID [ID ...]
   * @param key
   * @param ids
   */
  long xdel(String key, StreamEntryID... ids);

  /**
   * XTRIM key MAXLEN [~] count
   * @param key
   * @param maxLen
   * @param approximate
   */
  long xtrim(String key, long maxLen, boolean approximate);

  /**
   * XTRIM key MAXLEN|MINID [=|~] threshold [LIMIT count]
   * @param key
   * @param params
   */
  long xtrim(String key, XTrimParams params);

  /**
   *  XCLAIM <key> <group> <consumer> <min-idle-time> <ID-1> ... <ID-N>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE]
   */
  List<StreamEntry> xclaim(String key, String group, String consumerName, long minIdleTime,
      XClaimParams params, StreamEntryID... ids);

  /**
   *  XCLAIM <key> <group> <consumer> <min-idle-time> <ID-1> ... <ID-N>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE] JUSTID
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
   * @param start {@link StreamEntryID} - Entries >= start will be transferred ownership, passing <code>null</code> will indicate '-'
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
   * @param start {@link StreamEntryID} - Entries >= start will be transferred ownership, passing <code>null</code> will indicate '-'
   * @param params {@link XAutoClaimParams}
   */
  Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params);

  /**
   * Introspection command used in order to retrieve different information about the stream
   * @param key Stream name
   * @return {@link StreamInfo} that contains information about the stream
   */
  StreamInfo xinfoStream (String key);

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
   * @deprecated Use {@link StreamCommands#xinfoGroups(java.lang.String)}.
   */
  @Deprecated
  List<StreamGroupInfo> xinfoGroup(String key);

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
   * to the the group
   */
  List<StreamConsumersInfo> xinfoConsumers (String key, String group);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   *
   * @param xReadParams
   * @param streams
   */
  List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams,
      Map<String, StreamEntryID> streams);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   *
   * @param groupName
   * @param consumer
   * @param xReadGroupParams
   * @param streams
   */
  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupName, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams);

}
