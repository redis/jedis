package redis.clients.jedis.bloom.commands;

import java.util.Map;

public interface TDigestSketchCommands {

  String tdigestCreate(String key);

  String tdigestCreate(String key, int compression);

  String tdigestReset(String key);

  String tdigestMerge(String destinationKey, String sourceKey);

  String tdigestMergeStore(String destinationKey, String... sourceKeys);

  String tdigestMergeStore(int compression, String destinationKey, String... sourceKeys);

  Map<String, Object> tdigestInfo(String key);

  String tdigestAdd(String key, Map.Entry<Double, Double>... valueWeights);

  double tdigestCDF(String key, double value);

  Map<Double, Double> tdigestQuantile(String key, double... quantiles);

  double tdigestMin(String key);

  double tdigestMax(String key);

  double tdigestTrimmedMean(String key, double lowCutQuantile, double highCutQuantile);
}
