package redis.clients.jedis.timeseries;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;

public interface RedisTimeSeriesPipelineCommands {

  Response<String> tsCreate(String key);

  Response<String> tsCreate(String key, TSCreateParams createParams);

  Response<Long> tsDel(String key, long fromTimestamp, long toTimestamp);

  Response<String> tsAlter(String key, TSAlterParams alterParams);

  Response<Long> tsAdd(String key, double value);

  Response<Long> tsAdd(String key, long timestamp, double value);

  @Deprecated
  Response<Long> tsAdd(String key, long timestamp, double value, TSCreateParams createParams);

  Response<Long> tsAdd(String key, long timestamp, double value, TSAddParams addParams);

  Response<List<Long>> tsMAdd(Map.Entry<String, TSElement>... entries);

  Response<Long> tsIncrBy(String key, double value);

  Response<Long> tsIncrBy(String key, double value, long timestamp);

  Response<Long> tsIncrBy(String key, double addend, TSIncrByParams incrByParams);

  Response<Long> tsDecrBy(String key, double value);

  Response<Long> tsDecrBy(String key, double value, long timestamp);

  Response<Long> tsDecrBy(String key, double subtrahend, TSDecrByParams decrByParams);

  Response<List<TSElement>> tsRange(String key, long fromTimestamp, long toTimestamp);

  Response<List<TSElement>> tsRange(String key, TSRangeParams rangeParams);

  Response<List<TSElement>> tsRevRange(String key, long fromTimestamp, long toTimestamp);

  Response<List<TSElement>> tsRevRange(String key, TSRangeParams rangeParams);

  Response<Map<String, TSMRangeElements>> tsMRange(long fromTimestamp, long toTimestamp, String... filters);

  Response<Map<String, TSMRangeElements>> tsMRange(TSMRangeParams multiRangeParams);

  Response<Map<String, TSMRangeElements>> tsMRevRange(long fromTimestamp, long toTimestamp, String... filters);

  Response<Map<String, TSMRangeElements>> tsMRevRange(TSMRangeParams multiRangeParams);

  Response<TSElement> tsGet(String key);

  Response<TSElement> tsGet(String key, TSGetParams getParams);

  Response<Map<String, TSMGetElement>> tsMGet(TSMGetParams multiGetParams, String... filters);

  Response<String> tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType, long timeBucket);

  Response<String> tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType, long bucketDuration, long alignTimestamp);

  Response<String> tsDeleteRule(String sourceKey, String destKey);

  Response<List<String>> tsQueryIndex(String... filters);

  Response<TSInfo> tsInfo(String key);

  Response<TSInfo> tsInfoDebug(String key);
}
