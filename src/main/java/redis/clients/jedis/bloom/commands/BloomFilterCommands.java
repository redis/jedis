package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;

public interface BloomFilterCommands {

  /**
   * {@code BF.RESERVE {key} {error_rate} {capacity}}
   *
   * @param key
   * @param errorRate
   * @param capacity
   * @return OK
   */
  String bfReserve(String key, double errorRate, long capacity);

  /**
   * {@code BF.RESERVE {key} {error_rate} {capacity} [EXPANSION {expansion}] [NONSCALING]}
   *
   * @param key
   * @param errorRate
   * @param capacity
   * @param reserveParams
   * @return OK
   */
  String bfReserve(String key, double errorRate, long capacity, BFReserveParams reserveParams);

  /**
   * {@code BF.ADD {key} {item}}
   *
   * @param key
   * @param item
   */
  boolean bfAdd(String key, String item);

  /**
   * {@code BF.MADD {key} {item ...}}
   *
   * @param key
   * @param items
   */
  List<Boolean> bfMAdd(String key, String... items);

  /**
   * {@code BF.INSERT {key} ITEMS {item ...}}
   *
   * @param key
   * @param items
   */
  List<Boolean> bfInsert(String key, String... items);

  /**
   * {@code BF.INSERT {key} [CAPACITY {cap}] [ERROR {error}] [EXPANSION {expansion}] [NOCREATE]
   * [NONSCALING] ITEMS {item ...}}
   *
   * @param key
   * @param insertParams
   * @param items
   */
  List<Boolean> bfInsert(String key, BFInsertParams insertParams, String... items);

  /**
   * {@code BF.EXISTS {key} {item}}
   *
   * @param key
   * @param item
   * @return if the item may exist
   */
  boolean bfExists(String key, String item);

  /**
   * {@code BF.MEXISTS {key} {item ...}}
   *
   * @param key
   * @param items
   */
  List<Boolean> bfMExists(String key, String... items);

  /**
   * {@code BF.SCANDUMP {key} {iterator}}
   *
   * @param key
   * @param iterator
   * @return Pair of next iterator and current data
   */
  Map.Entry<Long, byte[]> bfScanDump(String key, long iterator);

  /**
   * {@code BF.LOADCHUNK {key} {iterator} {data}}
   *
   * @param key
   * @param iterator
   * @param data
   * @return OK
   */
  String bfLoadChunk(String key, long iterator, byte[] data);

  Map<String, Object> bfInfo(String key);
}
