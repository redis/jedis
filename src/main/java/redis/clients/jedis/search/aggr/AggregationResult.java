package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

public class AggregationResult {

  /**
   * @deprecated Use {@link AggregationResult#getTotalResults()}.
   */
  @Deprecated
  public final long totalResults;

  private final List<Map<String, Object>> results;

  private long cursorId = -1;

  public AggregationResult(Object resp, long cursorId) {
    this(resp);
    this.cursorId = cursorId;
  }

  public AggregationResult(Object resp) {
    List<Object> list = (List<Object>) SafeEncoder.encodeObject(resp);

    // the first element is always the number of results
    totalResults = (Long) list.get(0);
    results = new ArrayList<>(list.size() - 1);

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
  }

  public long getTotalResults() {
    return totalResults;
  }

  public List<Map<String, Object>> getResults() {
    return results;
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

  public long getCursorId() {
    return cursorId;
  }
}
