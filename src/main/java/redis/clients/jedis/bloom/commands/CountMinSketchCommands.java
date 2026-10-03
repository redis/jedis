package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.bloom.CmsCellSize;

/**
 * Interface for RedisBloom Count-Min Sketch Commands
 * 
 * @see <a href=
 *      "https://oss.redislabs.com/redisbloom/CountMinSketch_Commands/">RedisBloom
 *      Count-Min Sketch Documentation</a>
 */
public interface CountMinSketchCommands {
  /**
   * CMS.INITBYDIM Initializes a Count-Min Sketch to dimensions specified by user.
   * 
   * @param key   The name of the sketch
   * @param width Number of counter in each array. Reduces the error size
   * @param depth Number of counter-arrays. Reduces the probability for an error
   *              of a certain size (percentage of total count
   * @return OK
   */
  String cmsInitByDim(String key, long width, long depth);

  /**
   * CMS.INITBYDIM Initializes a Count-Min Sketch to dimensions specified by user,
   * with an explicit counter cell size.
   * 
   * @param key      The name of the sketch
   * @param width    Number of counter in each array. Reduces the error size
   * @param depth    Number of counter-arrays. Reduces the probability for an error
   *                 of a certain size (percentage of total count
   * @param cellSize Bytes per counter cell. Smaller cells reduce memory usage but
   *                 lower the maximum count a cell can hold before
   *                 {@code CMS.INCRBY} fails with an overflow error. The server
   *                 default is {@link CmsCellSize#FOUR_BYTES}.
   * @return OK
   * @since 8.1
   */
  String cmsInitByDim(String key, long width, long depth, CmsCellSize cellSize);

  /**
   * CMS.INITBYPROB Initializes a Count-Min Sketch to accommodate requested
   * capacity.
   * 
   * @param key         The name of the sketch.
   * @param error       Estimate size of error. The error is a percent of total
   *                    counted items. This effects the width of the sketch.
   * @param probability The desired probability for inflated count. This should be
   *                    a decimal value between 0 and 1. This effects the depth of
   *                    the sketch. For example, for a desired false positive rate
   *                    of 0.1% (1 in 1000), error_rate should be set to 0.001.
   *                    The closer this number is to zero, the greater the memory
   *                    consumption per item and the more CPU usage per operation.
   * @return OK
   */
  String cmsInitByProb(String key, double error, double probability);

  /**
   * CMS.INITBYPROB Initializes a Count-Min Sketch to accommodate requested
   * capacity, with an explicit counter cell size.
   * 
   * @param key         The name of the sketch.
   * @param error       Estimate size of error. The error is a percent of total
   *                    counted items. This effects the width of the sketch.
   * @param probability The desired probability for inflated count. This should be
   *                    a decimal value between 0 and 1. This effects the depth of
   *                    the sketch.
   * @param cellSize    Bytes per counter cell. See
   *                    {@link #cmsInitByDim(String, long, long, CmsCellSize)}.
   * @return OK
   * @since 8.1
   */
  String cmsInitByProb(String key, double error, double probability, CmsCellSize cellSize);

  /**
   * CMS.INCRBY Changes the count of item by increment. A negative increment
   * decrements the count; only decrement an item that was previously added by at
   * least that amount, otherwise the server rejects the call with an underflow
   * error. An increment that would exceed the capacity of the sketch's cells is
   * rejected with an overflow error, leaving the sketch unchanged.
   * 
   * @param key       The name of the sketch
   * @param item      The item which counter to be changed
   * @param increment Counter to be changed by this integer, may be negative
   * @return Count for the item after the change
   */
  // long cmsIncrBy(String key, String item, long increment);
  default long cmsIncrBy(String key, String item, long increment) {
    return cmsIncrBy(key, java.util.Collections.singletonMap(item, increment)).get(0);
  }

  /**
   * CMS.INCRBY Changes the count of one or more items. Increments may be negative;
   * see {@link #cmsIncrBy(String, String, long)} for the underflow and overflow
   * rules, which are reported per item.
   * 
   * @param key            The name of the sketch
   * @param itemIncrements a Map of the items to be changed and their integer
   *                       increment
   * @return Count of each item after the change
   */
  List<Long> cmsIncrBy(String key, Map<String, Long> itemIncrements);

  /**
   * CMS.QUERY Returns count for item. Multiple items can be queried with one
   * call.
   * 
   * @param key   The name of the sketch
   * @param items The items for which to retrieve the counts
   * @return Count for one or more items
   */
  List<Long> cmsQuery(String key, String... items);

  /**
   * CMS.MERGE Merges several sketches into one sketch. All sketches must have
   * identical width, depth and cell size.
   * 
   * @param destKey The name of destination sketch. Must be initialized.
   * @param keys    The sketches to be merged
   * @return OK
   */
  String cmsMerge(String destKey, String... keys);

  /**
   * CMS.MERGE Merges several sketches into one sketch. All sketches must have
   * identical width, depth and cell size. Weights can be used to multiply certain
   * sketches. Default weight is 1.
   * 
   * @param destKey        The name of destination sketch. Must be initialized.
   * @param keysAndWeights A map of keys and weights used to multiply the sketch.
   * @return OK
   */
  String cmsMerge(String destKey, Map<String, Long> keysAndWeights);

  /**
   * CMS.INFO Returns width, depth, total count and cell size of the sketch.
   * 
   * @param key The name of the sketch
   * @return A Map with {@code width}, {@code depth}, {@code count} and, on servers
   *         that support configurable cell sizes, {@code cell_size}.
   */
  Map<String, Object> cmsInfo(String key);
}
