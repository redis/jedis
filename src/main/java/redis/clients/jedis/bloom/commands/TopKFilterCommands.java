package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;

public interface TopKFilterCommands {

  /**
   * {@code TOPK.RESERVE {key} {topk}}
   *
   * @param key
   * @param topk
   * @return OK
   */
  String topkReserve(String key, long topk);

  /**
   * {@code TOPK.RESERVE {key} {topk} [{width} {depth} {decay}]}
   *
   * @param key
   * @param topk
   * @param width
   * @param depth
   * @param decay
   * @return OK
   */
  String topkReserve(String key, long topk, long width, long depth, double decay);

  /**
   * {@code TOPK.ADD {key} {item ...}}
   *
   * @param key
   * @param items
   * @return items dropped from list
   */
  List<String> topkAdd(String key, String... items);

  /**
   * {@code TOPK.INCRBY {key} {item} {increment} [{item} {increment} ...]}
   *
   * @param key
   * @param itemIncrements item and increment pairs
   * @return item dropped from list
   */
  List<String> topkIncrBy(String key, Map<String, Long> itemIncrements);

  /**
   * {@code TOPK.QUERY {key} {item ...}}
   *
   * @param key
   * @param items
   * @return if item is in Top-K
   */
  List<Boolean> topkQuery(String key, String... items);

  /**
   * {@code TOPK.COUNT {key} {item ...}}
   *
   * @param key
   * @param items
   * @return count for item
   * @deprecated As of RedisBloom 2.4, this command is regarded as deprecated.
   */
  @Deprecated
  List<Long> topkCount(String key, String... items);

  /**
   * {@code TOPK.LIST {key}}
   *
   * @param key
   * @return k (or less) items in Top K list
   */
  List<String> topkList(String key);

  /**
   * {@code TOPK.INFO {key}}
   *
   * @param key
   * @return information
   */
  Map<String, Object> topkInfo(String key);
}
