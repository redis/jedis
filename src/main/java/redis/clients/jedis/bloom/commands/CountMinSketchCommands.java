package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;

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
   * @return 
   */
  String cmsInitByProb(String key, double error, double probability);

  /**
   * CMS.INCRBY Increases the count of item by increment
   * 
   * @param key       The name of the sketch
   * @param item      The item which counter to be increased
   * @param increment Counter to be increased by this integer
   * @return Count for the item after increment
   */
  // long cmsIncrBy(String key, String item, long increment);
  default long cmsIncrBy(String key, String item, long increment) {
    return cmsIncrBy(key, java.util.Collections.singletonMap(item, increment)).get(0);
  }

  /**
   * CMS.INCRBY Increases the count of one or more item.
   * 
   * @param key            The name of the sketch
   * @param itemIncrements a Map of the items to be increased and their integer
   *                       increment
   * @return Count of each item after increment
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
   * identical width and depth.
   * 
   * @param destKey The name of destination sketch. Must be initialized.
   * @param keys    The sketches to be merged
   * @return OK
   */
  String cmsMerge(String destKey, String... keys);

  /**
   * CMS.MERGE Merges several sketches into one sketch. All sketches must have
   * identical width and depth. Weights can be used to multiply certain sketches.
   * Default weight is 1.
   * 
   * @param destKey        The name of destination sketch. Must be initialized.
   * @param keysAndWeights A map of keys and weights used to multiply the sketch.
   * @return OK
   */
  String cmsMerge(String destKey, Map<String, Long> keysAndWeights);

  /**
   * CMS.INFO Returns width, depth and total count of the sketch.
   * 
   * @param key The name of the sketch
   * @return A Map with width, depth and total count.
   */
  Map<String, Object> cmsInfo(String key);
}
