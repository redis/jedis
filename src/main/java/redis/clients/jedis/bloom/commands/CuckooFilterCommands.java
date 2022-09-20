package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.bloom.CFInsertParams;
import redis.clients.jedis.bloom.CFReserveParams;

/**
 * Interface for RedisBloom Cuckoo Filter Commands
 *
 * @see <a href=
 *      "https://oss.redislabs.com/redisbloom/Cuckoo_Commands/">RedisBloom
 *      Cuckoo Filter Documentation</a>
 */
public interface CuckooFilterCommands {

  /**
   * CF.RESERVE Creates a Cuckoo Filter under key with the given parameters
   *
   * @param key The name of the filter
   * @param capacity
   * @return OK
   */
  String cfReserve(String key, long capacity);

  /**
   * CF.RESERVE Creates a Cuckoo Filter under key with the given parameters
   *
   * @param key The name of the filter
   * @param capacity
   * @param reserveParams An instance of CFReserveParams containing the options
   * @return OK
   */
  String cfReserve(String key, long capacity, CFReserveParams reserveParams);

  /**
   * CF.ADD Adds an item to the cuckoo filter, creating the filter if it does not
   * exist
   *
   * @param key  The name of the filter
   * @param item The item to add
   * @return true on success, false otherwise
   */
  boolean cfAdd(String key, String item);

  /**
   * CF.ADDNX Adds an item to the cuckoo filter, only if it does not exist yet
   *
   * @param key  The name of the filter
   * @param item The item to add
   * @return true if the item was added to the filter, false if the item already
   *         exists.
   */
  boolean cfAddNx(String key, String item);

  /**
   * CF.INSERT Adds one or more items to a cuckoo filter, creating it if it does
   * not exist yet.
   *
   * @param key   The name of the filter
   * @param items One or more items to add
   * @return true if the item was successfully inserted, false if an error
   *         occurred
   */
  List<Boolean> cfInsert(String key, String... items);

  /**
   * CF.INSERT Adds one or more items to a cuckoo filter, using the passed
   * options
   *
   * @param key     The name of the filter
   * @param insertParams An instance of CFInsertParams containing the options
   * @param items    One or more items to add
   * @return true if the item was successfully inserted, false if an error
   *         occurred
   */
  List<Boolean> cfInsert(String key, CFInsertParams insertParams, String... items);

  /**
   * CF.INSERTNX Adds one or more items to a cuckoo filter, only if it does not
   * exist yet
   *
   * @param key   The name of the filter
   * @param items One or more items to add
   * @return true if the item was added to the filter, false if the item already
   *         exists.
   */
  List<Boolean> cfInsertNx(String key, String... items);

  /**
   * CF.INSERTNX Adds one or more items to a cuckoo filter, using the passed
   * options
   *
   * @param key     The name of the filter
   * @param insertParams An instance of CFInsertParams containing the options
   *                (CAPACITY/NOCREATE)
   * @param items   One or more items to add
   * @return true if the item was added to the filter, false if the item already
   *         exists.
   */
  List<Boolean> cfInsertNx(String key, CFInsertParams insertParams, String... items);

  /**
   * CF.EXISTS Check if an item exists in a Cuckoo Filter
   *
   * @param key  The name of the filter
   * @param item The item to check for
   * @return false if the item certainly does not exist, true if the item may
   *         exist.
   */
  boolean cfExists(String key, String item);

  /**
   * {@code CF.MEXISTS {key} {item ...}}
   *
   * @param key   The name of the filter
   * @param items Items to check for (non empty sequence)
   * @return a list of booleans where false if the item certainly does not exist,
   *         true if the item may exist.
   */
  List<Boolean> cfMExists(String key, String... items);

  /**
   * CF.DEL Deletes an item once from the filter. If the item exists only once, it
   * will be removed from the filter. If the item was added multiple times, it
   * will still be present.
   *
   * @param key  The name of the filter
   * @param item The item to delete from the filter
   * @return true if the item has been deleted, false if the item was not found.
   */
  boolean cfDel(String key, String item);

  /**
   * CF.COUNT Returns the number of times an item may be in the filter.
   *
   * @param key  The name of the filter
   * @param item The item to count
   * @return The number of times the item exists in the filter
   */
  long cfCount(String key, String item);

  /**
   * CF.SCANDUMP Begins an incremental save of the cuckoo filter. This is useful
   * for large cuckoo filters which cannot fit into the normal SAVE and RESTORE
   * model.
   *
   * The Iterator is passed as input to the next invocation of SCANDUMP . If
   * Iterator is 0, the iteration has completed.
   *
   * @param key      Name of the filter
   * @param iterator This is either 0, or the iterator from a previous invocation
   *                 of this command
   * @return a Map.Entry containing the Iterator and Data.
   */
  Map.Entry<Long, byte[]> cfScanDump(String key, long iterator);
//
//  /**
//   * CF.SCANDUMP Begins an incremental save of the cuckoo filter. This is useful
//   * for large cuckoo filters which cannot fit into the normal SAVE and RESTORE
//   * model.
//   *
//   * Returns an iterator over the Map.Entry containing the Iterator and Data in
//   * proper sequence.
//   *
//   * @param key Name of the filter
//   * @return An iterator over the Pair containing the chunks of byte[] representing the filter
//   */
//  Iterator<Map.Entry<Long, byte[]>> cfScanDumpIterator(String key);
//
//  /**
//   * CF.SCANDUMP Begins an incremental save of the cuckoo filter. This is useful
//   * for large cuckoo filters which cannot fit into the normal SAVE and RESTORE
//   * model.
//   *
//   * Returns a sequential Stream with the Map.Entry containing the Iterator and
//   * Data as its source.
//   *
//   * @return A sequential Stream of Pair of iterator and data
//   */
//  Stream<Map.Entry<Long, byte[]>> cfScanDumpStream(String key);

  /**
   * CF.LOADCHUNK Restores a filter previously saved using SCANDUMP.
   *
   * @param key       Name of the filter to restore
   * @param iterator Iterator from CF.SCANDUMP
   * @param data     Data from CF.SCANDUMP
   * @return OK
   */
  String cfLoadChunk(String key, long iterator, byte[] data);
//
//  /**
//   * CF.LOADCHUNK Restores a filter previously saved using SCANDUMP . See the
//   * SCANDUMP command for example usage.
//   *
//   * @param key         Name of the filter to restore
//   * @param iterAndData Pair of iterator and data
//   */
//  void cfLoadChunk(String key, Map.Entry<Long, byte[]> iterAndData);

  /**
   * CF.INFO Return information about filter
   *
   * @param key Name of the filter to restore
   * @return A Map containing Size, Number of buckets, Number of filter, Number of
   *         items inserted, Number of items deleted, Bucket size, Expansion rate,
   *         Max iteration
   */
  Map<String, Object> cfInfo(String key);
}
