package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

public class AggregationResult {

  private final long totalResults;

  private final List<Map<String, Object>> results;

  private final List<String> warnings;

  private Long cursorId = -1L;

  private AggregationResult(long totalResults, List<Map<String, Object>> results) {
    this(totalResults, results, (List<String>) null);
  }

  private AggregationResult(long totalResults, List<Map<String, Object>> results, List<String> warnings) {
    this.totalResults = totalResults;
    this.results = results;
    this.warnings = warnings;
  }

  private void setCursorId(Long cursorId) {
    this.cursorId = cursorId;
  }

  public Long getCursorId() {
    return cursorId;
  }

  public long getTotalResults() {
    return totalResults;
  }

  public List<Map<String, Object>> getResults() {
    return Collections.unmodifiableList(results);
  }

  /**
   * @return results as {@link Row}s.
   * @see #getResults()
   */
  public List<Row> getRows() {
    return results.stream().map(Row::new).collect(Collectors.toList());
  }

  public Row getRow(int index) {
    return new Row(results.get(index));
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public static final Builder<AggregationResult> SEARCH_AGGREGATION_RESULT = new Builder<AggregationResult>() {

    private static final String TOTAL_RESULTS_STR = "total_results";
    private static final String RESULTS_STR = "results";
    // private static final String FIELDS_STR = "fields";
    private static final String FIELDS_STR = "extra_attributes";
    private static final String WARNINGS_STR = "warning";

    @Override
    public AggregationResult build(Object data) {
      // return new AggregationResult(data);
      List list = (List) data;

      if (list.get(0) instanceof KeyValue) {
        List<KeyValue> kvList = (List<KeyValue>) data;
        long totalResults = -1;
        List<Map<String, Object>> results = null;
        List<String> warnings = null;
        for (KeyValue kv : kvList) {
          String key = BuilderFactory.STRING.build(kv.getKey());
          Object rawVal = kv.getValue();
          switch (key) {
            case TOTAL_RESULTS_STR:
              totalResults = BuilderFactory.LONG.build(rawVal);
              break;
            case RESULTS_STR:
              List<List<KeyValue>> resList = (List<List<KeyValue>>) rawVal;
              results = new ArrayList<>(resList.size());
              for (List<KeyValue> rikv : resList) {
                for (KeyValue ikv : rikv) {
                  if (FIELDS_STR.equals(BuilderFactory.STRING.build(ikv.getKey()))) {
                    results.add(BuilderFactory.ENCODED_OBJECT_MAP.build(ikv.getValue()));
                    break;
                  }
                }
              }
              break;
            case WARNINGS_STR:
              warnings = BuilderFactory.STRING_LIST.build(rawVal);
              break;
          }
        }
        return new AggregationResult(totalResults, results, warnings);
      }

      list = (List<Object>) SafeEncoder.encodeObject(data);

      // the first element is always the number of results
      long totalResults = (Long) list.get(0);
      List<Map<String, Object>> results = new ArrayList<>(list.size() - 1);

      for (int i = 1; i < list.size(); i++) {
        List<Object> mapList = (List<Object>) list.get(i);
        Map<String, Object> map = new HashMap<>(mapList.size() / 2, 1f);
        for (int j = 0; j < mapList.size(); j += 2) {
          Object r = mapList.get(j);
          if (r instanceof JedisDataException) {
            throw (JedisDataException) r;
          }
          map.put((String) r, mapList.get(j + 1));
        }
        results.add(map);
      }
      return new AggregationResult(totalResults, results);
    }
  };

  public static final Builder<AggregationResult> SEARCH_AGGREGATION_RESULT_WITH_CURSOR = new Builder<AggregationResult>() {
    @Override
    public AggregationResult build(Object data) {
      List<Object> list = (List<Object>) data;
      // return new AggregationResult(list.get(0), (long) list.get(1));
      AggregationResult r = SEARCH_AGGREGATION_RESULT.build(list.get(0));
      r.setCursorId((Long) list.get(1));
      return r;
    }
  };
}
