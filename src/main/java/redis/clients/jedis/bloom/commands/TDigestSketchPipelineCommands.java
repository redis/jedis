package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;
import redis.clients.jedis.bloom.TDigestMergeParams;

public interface TDigestSketchPipelineCommands {

  Response<String> tdigestCreate(String key);

  Response<String> tdigestCreate(String key, int compression);

  Response<String> tdigestReset(String key);

  Response<String> tdigestMerge(String destinationKey, String... sourceKeys);

  Response<String> tdigestMerge(TDigestMergeParams mergeParams, String destinationKey, String... sourceKeys);

  Response<Map<String, Object>> tdigestInfo(String key);

  Response<String> tdigestAdd(String key, double... values);

  Response<List<Double>> tdigestCDF(String key, double... values);

  Response<List<Double>> tdigestQuantile(String key, double... quantiles);

  Response<Double> tdigestMin(String key);

  Response<Double> tdigestMax(String key);

  Response<Double> tdigestTrimmedMean(String key, double lowCutQuantile, double highCutQuantile);

  Response<List<Long>> tdigestRank(String key, double... values);

  Response<List<Long>> tdigestRevRank(String key, double... values);

  Response<List<Double>> tdigestByRank(String key, long... ranks);

  Response<List<Double>> tdigestByRevRank(String key, long... ranks);
}
