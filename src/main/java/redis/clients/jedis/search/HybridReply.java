package redis.clients.jedis.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.KeyValue;

/**
 * Represents the results of an {@code FT.HYBRID} command. Contains total result count, execution
 * time, warnings, and a list of per-document results with document key and field values.
 */
@Experimental
public class HybridReply {

  private long totalResults;
  private double executionTime;
  private final List<Map<String, Object>> results;
  private final List<String> warnings;

  /**
   * Creates a new empty HybridReply instance.
   */
  public HybridReply() {
    this.totalResults = 0;
    this.executionTime = 0;
    this.results = new ArrayList<>();
    this.warnings = new ArrayList<>();
  }

  /**
   * Creates a HybridReply with the given parameters.
   * @param totalResults the total number of matching documents
   * @param results the list of results
   */
  public HybridReply(long totalResults, List<Map<String, Object>> results) {
    this.totalResults = totalResults;
    this.executionTime = 0;
    this.results = results != null ? results : new ArrayList<>();
    this.warnings = new ArrayList<>();
  }

  /**
   * Creates a HybridReply with all parameters.
   * @param totalResults the total number of matching documents
   * @param results the list of results
   * @param warnings the list of warnings
   */
  public HybridReply(long totalResults, List<Map<String, Object>> results, List<String> warnings) {
    this.totalResults = totalResults;
    this.executionTime = 0;
    this.results = results != null ? results : new ArrayList<>();
    this.warnings = warnings != null ? warnings : new ArrayList<>();
  }

  /**
   * @return the total number of matching documents reported by the server
   */
  public long getTotalResults() {
    return totalResults;
  }

  /**
   * Set the total number of matching documents.
   * @param totalResults the total number of results
   */
  public void setTotalResults(long totalResults) {
    this.totalResults = totalResults;
  }

  /**
   * @return the execution time reported by the server in seconds (or {@code 0.0} if not available)
   */
  public double getExecutionTime() {
    return executionTime;
  }

  /**
   * Set the execution time reported by the server.
   * @param executionTime execution time in seconds
   */
  public void setExecutionTime(double executionTime) {
    this.executionTime = executionTime;
  }

  /**
   * @return an unmodifiable view of all results returned by the command
   */
  public List<Map<String, Object>> getResults() {
    return Collections.unmodifiableList(results);
  }

  /**
   * Add a new result entry.
   * @param result the result to add
   */
  public void addResult(Map<String, Object> result) {
    this.results.add(result);
  }

  /**
   * @return a read-only view of all warnings reported by the server
   */
  public List<String> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  /**
   * Add a warning message.
   * @param warning the warning to add
   */
  public void addWarning(String warning) {
    this.warnings.add(warning);
  }

  /**
   * @return the number of result entries
   */
  public int size() {
    return results.size();
  }

  /**
   * @return {@code true} if no results were returned
   */
  public boolean isEmpty() {
    return results.isEmpty();
  }

  // RESP2/RESP3 Builder
  public static final Builder<HybridReply> HYBRID_REPLY_BUILDER = new Builder<HybridReply>() {
    private static final String TOTAL_RESULTS_STR = "total_results";
    private static final String EXECUTION_TIME_STR = "execution_time";
    private static final String RESULTS_STR = "results";
    private static final String WARNINGS_STR = "warnings";

    @Override
    public HybridReply build(Object data) {
      List list = (List) data;

      // Check if RESP3 (KeyValue) or RESP2 (flat list)
      if (!list.isEmpty() && list.get(0) instanceof KeyValue) {
        return buildResp3((List<KeyValue>) list);
      } else {
        return buildResp2(list);
      }
    }

    private HybridReply buildResp3(List<KeyValue> list) {
      long totalResults = -1;
      double executionTime = 0;
      List<Map<String, Object>> results = null;
      List<String> warnings = null;

      for (KeyValue kv : list) {
        String key = BuilderFactory.STRING.build(kv.getKey());
        Object rawVal = kv.getValue();
        switch (key) {
          case TOTAL_RESULTS_STR:
            totalResults = BuilderFactory.LONG.build(rawVal);
            break;
          case EXECUTION_TIME_STR:
            executionTime = BuilderFactory.DOUBLE.build(rawVal);
            break;
          case RESULTS_STR:
            results = new ArrayList<>();
            List<Object> resultsList = (List<Object>) rawVal;
            for (Object resultObj : resultsList) {
              Map<String, Object> result = BuilderFactory.ENCODED_OBJECT_MAP.build(resultObj);
              results.add(result);
            }
            break;
          case WARNINGS_STR:
            warnings = BuilderFactory.STRING_LIST.build(rawVal);
            break;
        }
      }

      HybridReply reply = new HybridReply(totalResults, results, warnings);
      reply.setExecutionTime(executionTime);
      return reply;
    }

    private HybridReply buildResp2(List list) {
      // RESP2 format: ["key1", value1, "key2", value2, ...]
      long totalResults = -1;
      double executionTime = 0;
      List<Map<String, Object>> results = null;
      List<String> warnings = null;

      for (int i = 0; i + 1 < list.size(); i += 2) {
        String key = BuilderFactory.STRING.build(list.get(i));
        Object rawVal = list.get(i + 1);

        switch (key) {
          case TOTAL_RESULTS_STR:
            totalResults = BuilderFactory.LONG.build(rawVal);
            break;
          case EXECUTION_TIME_STR:
            executionTime = BuilderFactory.DOUBLE.build(rawVal);
            break;
          case RESULTS_STR:
            results = new ArrayList<>();
            List<Object> resultsList = (List<Object>) rawVal;
            for (Object resultObj : resultsList) {
              Map<String, Object> result = BuilderFactory.ENCODED_OBJECT_MAP.build(resultObj);
              results.add(result);
            }
            break;
          case WARNINGS_STR:
            warnings = BuilderFactory.STRING_LIST.build(rawVal);
            break;
        }
      }

      HybridReply reply = new HybridReply(totalResults, results, warnings);
      reply.setExecutionTime(executionTime);
      return reply;
    }
  };
}
