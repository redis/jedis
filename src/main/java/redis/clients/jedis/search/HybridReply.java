package redis.clients.jedis.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.KeyValue;

/**
 * Represents the results of an {@code FT.HYBRID} command. Contains total result count, execution time,
 * warnings, and a list of per-document results with document key and field values.
 */
@Experimental
public class HybridReply {

  private long totalResults;
  private double executionTime;
  private final List<Result> results;
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
   * 
   * @param totalResults the total number of matching documents
   * @param results the list of results
   */
  public HybridReply(long totalResults, List<Result> results) {
    this.totalResults = totalResults;
    this.executionTime = 0;
    this.results = results != null ? results : new ArrayList<>();
    this.warnings = new ArrayList<>();
  }

  /**
   * Creates a HybridReply with all parameters.
   * 
   * @param totalResults the total number of matching documents
   * @param results the list of results
   * @param warnings the list of warnings
   */
  public HybridReply(long totalResults, List<Result> results, List<String> warnings) {
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
   * 
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
   * 
   * @param executionTime execution time in seconds
   */
  public void setExecutionTime(double executionTime) {
    this.executionTime = executionTime;
  }

  /**
   * @return an unmodifiable view of all results returned by the command
   */
  public List<Result> getResults() {
    return Collections.unmodifiableList(results);
  }

  /**
   * Add a new result entry.
   * 
   * @param result the result to add
   */
  public void addResult(Result result) {
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
   * 
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

  /**
   * Represents a single result entry in an {@code FT.HYBRID} response.
   * <p>
   * Each result contains field values returned by the query. The document key is available in the fields map
   * under the reserved field name {@code __key} when returning individual documents. Score information (text
   * score, vector distance, combined score) is included in the fields map when using {@code YIELD_SCORE_AS} in
   * the query.
   * </p>
   */
  public static class Result {
    private final Map<String, String> fields = new HashMap<>();

    public Result() {
    }

    /**
     * @return a mutable map of all fields associated with this result
     */
    public Map<String, String> getFields() {
      return fields;
    }

    /**
     * Add a single field to this result.
     *
     * @param key field name
     * @param value field value
     */
    public void addField(String key, String value) {
      fields.put(key, value);
    }
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
      List<Result> results = null;
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
              Result result = new Result();
              List<KeyValue> resultFields = (List<KeyValue>) resultObj;
              for (KeyValue fieldKv : resultFields) {
                String fieldKey = BuilderFactory.STRING.build(fieldKv.getKey());
                String fieldValue = BuilderFactory.STRING.build(fieldKv.getValue());
                result.addField(fieldKey, fieldValue);
              }
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
      List<Result> results = null;
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
              Result result = new Result();
              List<Object> resultFields = (List<Object>) resultObj;
              // RESP2: flat list of alternating keys and values
              for (int j = 0; j + 1 < resultFields.size(); j += 2) {
                String fieldKey = BuilderFactory.STRING.build(resultFields.get(j));
                String fieldValue = BuilderFactory.STRING.build(resultFields.get(j + 1));
                result.addField(fieldKey, fieldValue);
              }
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

