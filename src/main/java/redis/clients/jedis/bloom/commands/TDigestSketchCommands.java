package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.bloom.TDigestMergeParams;

public interface TDigestSketchCommands {

  /**
   * {@code TDIGEST.CREATE key}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @return OK
   */
  String tdigestCreate(String key);

  /**
   * {@code TDIGEST.CREATE key [compression]}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @param compression The compression parameter. 100 is a common value for normal uses. 1000 is extremely large.
   * @return OK
   */
  String tdigestCreate(String key, int compression);

  /**
   * {@code TDIGEST.RESET key}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @return OK
   */
  String tdigestReset(String key);

  /**
   * {@code TDIGEST.MERGE destination-key numkeys source-key [source-key ...]}
   *
   * @param destinationKey Sketch to copy observation values to (a t-digest data structure)
   * @param sourceKeys Sketch(es) to copy observation values from (a t-digest data structure)
   * @return OK
   */
  String tdigestMerge(String destinationKey, String... sourceKeys);

  /**
   * {@code TDIGEST.MERGE destination-key numkeys source-key [source-key ...]
   * [COMPRESSION compression] [OVERRIDE]}
   *
   * @param mergeParams compression and override options
   * @param destinationKey Sketch to copy observation values to (a t-digest data structure)
   * @param sourceKeys Sketch(es) to copy observation values from (a t-digest data structure)
   * @return OK
   */
  String tdigestMerge(TDigestMergeParams mergeParams, String destinationKey, String... sourceKeys);

  /**
   * {@code TDIGEST.INFO key}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @return information about the sketch
   */
  Map<String, Object> tdigestInfo(String key);

  /**
   * {@code TDIGEST.ADD key value weight [ value weight ...]}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @param values The value of the observation (floating-point)
   * @return OK
   */
  String tdigestAdd(String key, double... values);

  /**
   * {@code TDIGEST.CDF key value [value ...]}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @param values upper limit of observation value, for which the fraction of all observations added which are &le; value
   * @return estimation of the fraction of all observations added which are &le; value
   */
  List<Double> tdigestCDF(String key, double... values);

  /**
   * {@code TDIGEST.QUANTILE key quantile [quantile ...]}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @param quantiles The desired fraction(s) (between 0 and 1 inclusively)
   * @return results
   */
  List<Double> tdigestQuantile(String key, double... quantiles);

  /**
   * {@code TDIGEST.MIN key}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @return minimum observation value from the sketch
   */
  double tdigestMin(String key);

  /**
   * {@code TDIGEST.MAX key}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @return maximum observation value from the sketch
   */
  double tdigestMax(String key);

  /**
   * {@code TDIGEST.TRIMMED_MEAN key low_cut_quantile high_cut_quantile}
   *
   * @param key The name of the sketch (a t-digest data structure)
   * @param lowCutQuantile Exclude observation values lower than this quantile
   * @param highCutQuantile Exclude observation values higher than this quantile
   * @return estimation of the mean value
   */
  double tdigestTrimmedMean(String key, double lowCutQuantile, double highCutQuantile);

  List<Long> tdigestRank(String key, double... values);

  List<Long> tdigestRevRank(String key, double... values);

  List<Double> tdigestByRank(String key, long... ranks);

  List<Double> tdigestByRevRank(String key, long... ranks);
}
