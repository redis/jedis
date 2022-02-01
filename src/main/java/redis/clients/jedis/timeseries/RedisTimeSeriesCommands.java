package redis.clients.jedis.timeseries;

import java.util.List;

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
   * {@code TS.ADD key timestamp value [RETENTION retentionTime] [ENCODING [COMPRESSED|UNCOMPRESSED]] [CHUNK_SIZE size] [ON_DUPLICATE policy] [LABELS label value..]}
   *
   * @param key
   * @param timestamp
   * @param value
   * @param createParams
   * @return timestamp
   */
  long tsAdd(String key, long timestamp, double value, TSCreateParams createParams);

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
   * [FILTER_BY_TS TS1 TS2 ..] [FILTER_BY_VALUE min max]
   * [COUNT count] [ALIGN value] [AGGREGATION aggregationType timeBucket]}
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
   * [FILTER_BY_TS TS1 TS2 ..] [FILTER_BY_VALUE min max]
   * [COUNT count] [ALIGN value] [AGGREGATION aggregationType timeBucket]}
   *
   * @param key
   * @param rangeParams
   * @return range elements
   */
  List<TSElement> tsRevRange(String key, TSRangeParams rangeParams);

  /**
   * {@code TS.MRANGE fromTimestamp toTimestamp}
   *
   * @param fromTimestamp
   * @param toTimestamp
   * @param filters
   * @return multi range elements
   */
  List<KeyedTSElements> tsMRange(long fromTimestamp, long toTimestamp, String... filters);

  /**
   * {@code TS.MRANGE fromTimestamp toTimestamp
   * [FILTER_BY_TS TS1 TS2 ..] [FILTER_BY_VALUE min max]
   * [WITHLABELS | SELECTED_LABELS label1 ..]
   * [COUNT count] [ALIGN value]
   * [AGGREGATION aggregationType timeBucket]
   * FILTER filter..
   * [GROUPBY <label> REDUCE <reducer>]}
   *
   * @param multiRangeParams
   * @return multi range elements
   */
  List<KeyedTSElements> tsMRange(TSMRangeParams multiRangeParams);

  /**
   * {@code TS.MREVRANGE fromTimestamp toTimestamp}
   *
   * @param fromTimestamp
   * @param toTimestamp
   * @param filters
   * @return multi range elements
   */
  List<KeyedTSElements> tsMRevRange(long fromTimestamp, long toTimestamp, String... filters);

  /**
   * {@code TS.MREVRANGE fromTimestamp toTimestamp
   * [FILTER_BY_TS TS1 TS2 ..]
   * [FILTER_BY_VALUE min max]
   * [WITHLABELS | SELECTED_LABELS label1 ..]
   * [COUNT count] [ALIGN value]
   * [AGGREGATION aggregationType timeBucket]
   * FILTER filter..
   * [GROUPBY <label> REDUCE <reducer>]}
   *
   * @param multiRangeParams
   * @return multi range elements
   */
  List<KeyedTSElements> tsMRevRange(TSMRangeParams multiRangeParams);

  /**
   * {@code TS.GET key}
   *
   * @param key
   * @return the element
   */
  TSElement tsGet(String key);

  /**
   * {@code TS.MGET [WITHLABELS | SELECTED_LABELS label1 ..] FILTER filter...}
   *
   * @param multiGetParams
   * @param filters
   * @return multi get elements
   */
  List<KeyedTSElements> tsMGet(TSMGetParams multiGetParams, String... filters);

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
}
