package redis.clients.jedis.commands;

import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.ListPosition;
import redis.clients.jedis.StreamPendingEntry;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.Tuple;

import java.util.Map;

/**
 * Common interface for sharded and non-sharded Jedis
 */
public interface JedisCommands extends JedisBaseCommands {

    Long move(String key, int dbIndex);

    ScanResult<String> sscan(String key, String cursor, ScanParams params);

    ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

    Long bitpos(String key, boolean value);

    Long bitpos(String key, boolean value, BitPosParams params);

    Double hincrByFloat(String key, String field, double value);

    ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

    String restoreReplace(String key, int ttl, byte[] serializedValue);

  /**
   * Executes BITFIELD Redis command
   * @param key
   * @param arguments
   * @return 
   */
  List<Long> bitfield(String key, String...arguments);

  /**
   * Used for HSTRLEN Redis command
   * @param key 
   * @param field
   * @return length of the value for key
   */
  Long hstrlen(String key, String field);

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
   * XADD key MAXLEN ~ LEN ID field string [field string ...]
   * 
   * @param key
   * @param id
   * @param hash
   * @param maxLen
   * @param approximateLength
   * @return
   */
  StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen, boolean approximateLength);
  
  /**
   * XLEN key
   * 
   * @param key
   * @return
   */
  Long xlen(String key);

  /**
   * XRANGE key start end [COUNT count]
   * 
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream  
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @param count maximum number of entries returned 
   * @return The entries with IDs matching the specified range. 
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count);

  /**
   * XREVRANGE key end start [COUNT <n>]
   * 
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream  
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @param count The entries with IDs matching the specified range. 
   * @return the entries with IDs matching the specified range, from the higher ID to the lower ID matching.
   */
  List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count);
    
  /**
   * XACK key group ID [ID ...]
   * 
   * @param key
   * @param group
   * @param ids
   * @return
   */
  long xack(String key, String group,  StreamEntryID... ids);
  
  /**
   * XGROUP CREATE <key> <groupname> <id or $>
   * 
   * @param key
   * @param groupname
   * @param id
   * @param makeStream
   * @return
   */
  String xgroupCreate( String key, String groupname, StreamEntryID id, boolean makeStream);
  
  /**
   * XGROUP SETID <key> <groupname> <id or $>
   * 
   * @param key
   * @param groupname
   * @param id
   * @return
   */
  String xgroupSetID( String key, String groupname, StreamEntryID id);
  
  /**
   * XGROUP DESTROY <key> <groupname>
   * 
   * @param key
   * @param groupname
   * @return
   */
  long xgroupDestroy( String key, String groupname);
  
  /**
   * XGROUP DELCONSUMER <key> <groupname> <consumername> 
   * @param key
   * @param groupname
   * @param consumername
   * @return
   */
  String xgroupDelConsumer( String key, String groupname, String consumername);

  /**
   * XPENDING key group [start end count] [consumer]
   * 
   * @param key
   * @param groupname
   * @param start
   * @param end
   * @param count
   * @param consumername
   * @return
   */
  List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername);
  
  /**
   * XDEL key ID [ID ...]
   * @param key
   * @param ids
   * @return
   */
  long xdel( String key, StreamEntryID... ids);
  
  /**
   * XTRIM key MAXLEN [~] count
   * @param key
   * @param maxLen
   * @param approximate
   * @return
   */
  long xtrim( String key, long maxLen, boolean approximate);
 
  /**
   *  XCLAIM <key> <group> <consumer> <min-idle-time> <ID-1> <ID-2>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE] [JUSTID]
   */        
  List<StreamEntry> xclaim( String key, String group, String consumername, long minIdleTime, 
      long newIdleTime, int retries, boolean force, StreamEntryID... ids);
}
