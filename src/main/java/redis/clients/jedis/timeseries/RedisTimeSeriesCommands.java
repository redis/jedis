package redis.clients.jedis.timeseries;

import java.util.List;
import java.util.Map;

public interface RedisTimeSeriesCommands {

  /**
   * {@code TS.CREATE key}
   *
   * @param key
   */
  String tsCreate(String key);

  /**
   * {@code TS.CREATE key [RETENTION retentionTime] [ENCODING [UNCOMPRESSED|COMPRESSED]] [CHUNK_SIZE size] [DUPLICATE_POLICY policy] [LABELS label value..]}
   *
   * @param key
   * @param createParams
   */
  String tsCreate(String key, TSCreateParams createParams);

  /**
   * {@code TS.DEL key fromTimestamp toTimestamp}
   *
   * @param key
   * @param fromTimestamp
   * @param toTimestamp
   * @return The number of samples that were removed
   */
  long tsDel(String key, long fromTimestamp, long toTimestamp);

  /**
   * {@code TS.ALTER key [RETENTION retentionTime] [LABELS label value..]}
   *
   * @param key
   * @param alterParams
   * @return OK
   */
  String tsAlter(String key, TSAlterParams alterParams);

  /**
   * {@code TS.ADD key * value}
   *
   * @param key
   * @param value
   * @return timestamp
   */
  long tsAdd(String key, double value);

  /**
   * {@code TS.ADD key timestamp value}
   *
   * @param key
   * @param timestamp
   * @param value
   * @return timestamp
   */
  long tsAdd(String key, long timestamp, double value);

  /**
   * @param key
   * @param timestamp
   * @param value
   * @param createParams
   * @return timestamp
   * @deprecated Use {@link RedisTimeSeriesCommands#tsAdd(java.lang.String, long, double, redis.clients.jedis.timeseries.TSAddParams)}.
   */
  @Deprecated
  long tsAdd(String key, long timestamp, double value, TSCreateParams createParams);

  /**
   * {@code TS.ADD key timestamp value
   * [RETENTION retentionTime]
   * [ENCODING <COMPRESSED|UNCOMPRESSED>]
   * [CHUNK_SIZE size]
   * [DUPLICATE_POLICY policy]
   * [ON_DUPLICATE policy_ovr]
   * [LABELS label value..]}
   *
   * @param key
   * @param timestamp
   * @param value
   * @param addParams
   * @return timestamp
   */
  long tsAdd(String key, long timestamp, double value, TSAddParams addParams);

  /**
   * {@code TS.MADD key timestamp value [key timestamp value ...]}
   *
   * @param entries key, timestamp, value
   * @return timestamps
   */
  List<Long> tsMAdd(Map.Entry<String, TSElement>... entries);

  long tsIncrBy(String key, double value);

  long tsIncrBy(String key, double value, long timestamp);

  /**
   * {@code TS.INCRBY key addend
   * [TIMESTAMP timestamp]
   * [RETENTION retentionPeriod]
   * [ENCODING <COMPRESSED|UNCOMPRESSED>]
   * [CHUNK_SIZE size]
   * [DUPLICATE_POLICY policy]
   * [IGNORE ignoreMaxTimediff ignoreMaxValDiff]
   * [LABELS [label value ...]]}
   *
   * @param key
   * @param addend
   * @param incrByParams
   * @return timestamp
   */
  long tsIncrBy(String key, double addend, TSIncrByParams incrByParams);

  long tsDecrBy(String key, double value);

  long tsDecrBy(String key, double value, long timestamp);

  /**
   * {@code TS.DECRBY key subtrahend
   * [TIMESTAMP timestamp]
   * [RETENTION retentionPeriod]
   * [ENCODING <COMPRESSED|UNCOMPRESSED>]
   * [CHUNK_SIZE size]
   * [DUPLICATE_POLICY policy]
   * [IGNORE ignoreMaxTimediff ignoreMaxValDiff]
   * [LABELS [label value ...]]}
   *
   * @param key
   * @param subtrahend
   * @param decrByParams
   * @return timestamp
   */
  long tsDecrBy(String key, double subtrahend, TSDecrByParams decrByParams);

  /**
   * {@code TS.RANGE key fromTimestamp toTimestamp}
   *
   * @param key
   * @param fromTimestamp
   * @param toTimestamp
   * @return range elements
   */
  List<TSElement> tsRange(String key, long fromTimestamp, long toTimestamp);

  /**
   * {@code TS.RANGE key fromTimestamp toTimestamp
   * [LATEST]
   * [FILTER_BY_TS ts...]
   * [FILTER_BY_VALUE min max]
   * [COUNT count]
   * [[ALIGN value] AGGREGATION aggregator bucketDuration [BUCKETTIMESTAMP bt] [EMPTY]]}
   *
   * @param key
   * @param rangeParams
   * @return range elements
   */
  List<TSElement> tsRange(String key, TSRangeParams rangeParams);

  /**
   * {@code TS.REVRANGE key fromTimestamp toTimestamp}
   *
   * @param key
   * @param fromTimestamp
   * @param toTimestamp
   * @return range elements
   */
  List<TSElement> tsRevRange(String key, long fromTimestamp, long toTimestamp);

  /**
   * {@code TS.REVRANGE key fromTimestamp toTimestamp
   * [LATEST]
   * [FILTER_BY_TS TS...]
   * [FILTER_BY_VALUE min max]
   * [COUNT count]
   * [[ALIGN value] AGGREGATION aggregator bucketDuration [BUCKETTIMESTAMP bt] [EMPTY]]}
   *
   * @param key
   * @param rangeParams
   * @return range elements
   */
  List<TSElement> tsRevRange(String key, TSRangeParams rangeParams);

  /**
   * {@code TS.READ key timestamp}
   * <p>
   * Non-blocking read of up to unlimited samples with timestamp greater than or equal to the given
   * literal cursor, in ascending timestamp order. An empty list is a successful reply.
   *
   * @param key the time series key
   * @param timestamp inclusive literal cursor (non-negative Unix milliseconds; {@code 0} reads from
   *          the beginning)
   * @return samples with timestamp {@code >= timestamp}
   * @since 8.0
   */
  List<TSElement> tsRead(String key, long timestamp);

  /**
   * {@code TS.READ key timestamp [BLOCK milliseconds min_count] [MAX_COUNT max_count]}
   * <p>
   * Returns up to {@code max_count} samples with timestamp greater than or equal to the cursor, in
   * ascending timestamp order. With {@code BLOCK} the call waits until at least {@code min_count}
   * qualifying samples exist or until the timeout elapses; without it the call returns immediately.
   * An empty list is a successful reply, including when a blocking call times out.
   *
   * @param key the time series key
   * @param readParams the cursor and optional {@code BLOCK} / {@code MAX_COUNT} arguments
   * @return samples with timestamp {@code >=} the resolved cursor
   * @since 8.0
   */
  List<TSElement> tsRead(String key, TSReadParams readParams);

  /**
   * {@code TS.MRANGE fromTimestamp toTimestamp FILTER filter...}
   *
   * @param fromTimestamp
   * @param toTimestamp
   * @param filters
   * @return multi range elements
   */
  Map<String, TSMRangeElements> tsMRange(long fromTimestamp, long toTimestamp, String... filters);

  /**
   * {@code TS.MRANGE fromTimestamp toTimestamp
   * [LATEST]
   * [FILTER_BY_TS ts...]
   * [FILTER_BY_VALUE min max]
   * [WITHLABELS | SELECTED_LABELS label...]
   * [COUNT count]
   * [[ALIGN value] AGGREGATION aggregator bucketDuration [BUCKETTIMESTAMP bt] [EMPTY]]
   * FILTER filter...
   * [GROUPBY label REDUCE reducer]}
   *
   * @param multiRangeParams
   * @return multi range elements
   */
  Map<String, TSMRangeElements> tsMRange(TSMRangeParams multiRangeParams);

  /**
   * {@code TS.MREVRANGE fromTimestamp toTimestamp FILTER filter...}
   *
   * @param fromTimestamp
   * @param toTimestamp
   * @param filters
   * @return multi range elements
   */
  Map<String, TSMRangeElements> tsMRevRange(long fromTimestamp, long toTimestamp, String... filters);

  /**
   * {@code TS.MREVRANGE fromTimestamp toTimestamp
   * [LATEST]
   * [FILTER_BY_TS TS...]
   * [FILTER_BY_VALUE min max]
   * [WITHLABELS | SELECTED_LABELS label...]
   * [COUNT count]
   * [[ALIGN value] AGGREGATION aggregator bucketDuration [BUCKETTIMESTAMP bt] [EMPTY]]
   * FILTER filter...
   * [GROUPBY label REDUCE reducer]}
   *
   * @param multiRangeParams
   * @return multi range elements
   */
  Map<String, TSMRangeElements> tsMRevRange(TSMRangeParams multiRangeParams);

  /**
   * {@code TS.GET key}
   *
   * @param key the key
   * @return the element
   */
  TSElement tsGet(String key);

  /**
   * {@code TS.GET key [LATEST]}
   *
   * @param key the key
   * @param getParams optional arguments
   * @return the element
   */
  TSElement tsGet(String key, TSGetParams getParams);

  /**
   * {@code TS.MGET [LATEST] [ WITHLABELS | SELECTED_LABELS label...] FILTER filter...}
   *
   * @param multiGetParams optional arguments
   * @param filters secondary indexes
   * @return multi get elements
   */
  Map<String, TSMGetElement> tsMGet(TSMGetParams multiGetParams, String... filters);

  /**
   * {@code TS.CREATERULE sourceKey destKey AGGREGATION aggregationType timeBucket}
   *
   * @param sourceKey
   * @param destKey
   * @param aggregationType
   * @param timeBucket
   */
  String tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType, long timeBucket);

  /**
   * {@code TS.CREATERULE sourceKey destKey AGGREGATION aggregationType bucketDuration [alignTimestamp]}
   *
   * @param sourceKey
   * @param destKey
   * @param aggregationType
   * @param bucketDuration
   * @param alignTimestamp
   */
  String tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType, long bucketDuration, long alignTimestamp);

  /**
   * {@code TS.DELETERULE sourceKey destKey}
   *
   * @param sourceKey
   * @param destKey
   */
  String tsDeleteRule(String sourceKey, String destKey);

  /**
   * {@code TS.QUERYINDEX filter...}
   *
   * @param filters
   * @return list of timeseries keys
   */
  List<String> tsQueryIndex(String... filters);

  TSInfo tsInfo(String key);

  TSInfo tsInfoDebug(String key);
}
