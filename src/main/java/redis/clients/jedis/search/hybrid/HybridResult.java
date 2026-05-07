package redis.clients.jedis.search.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.util.KeyValue;

/**
 * Represents the results of an {@code FT.HYBRID} command. Extends the concept of search results
 * with additional hybrid-specific fields like execution time. Results are returned as
 * {@link Document} objects.
 */
@Experimental
public class HybridResult {

  private static final String KEY_FIELD = "__key";
  private static final String SCORE_FIELD = "__score";

  private final long totalResults;
  private final double executionTime;
  private final List<Document> documents;
  private final List<String> warnings;

  private HybridResult(long totalResults, double executionTime, List<Document> documents,
      List<String> warnings) {
    this.totalResults = totalResults;
    this.executionTime = executionTime;
    this.documents = documents != null ? documents : Collections.emptyList();
    this.warnings = warnings != null ? warnings : Collections.emptyList();
  }

  /**
   * @return the total number of matching documents reported by the server
   */
  public long getTotalResults() {
    return totalResults;
  }

  /**
   * @return the execution time reported by the server in seconds (or {@code 0.0} if not available)
   */
  public double getExecutionTime() {
    return executionTime;
  }

  /**
   * @return an unmodifiable view of all documents returned by the command
   */
  public List<Document> getDocuments() {
    return Collections.unmodifiableList(documents);
  }

  /**
   * @return a read-only view of all warnings reported by the server
   */
  public List<String> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{Total results:" + totalResults + ", Execution time:"
        + executionTime + ", Documents:" + documents
        + (warnings != null ? ", Warnings:" + warnings : "") + "}";
  }

  /**
   * Converts a flat map result to a Document. The map may contain __key and __score fields which
   * are extracted as the document id and score respectively.
   */
  private static Document mapToDocument(Map<String, Object> map) {
    String id = null;
    Double score = null;
    Map<String, Object> fields = new HashMap<>();

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (KEY_FIELD.equals(key)) {
        id = value != null ? value.toString() : null;
      } else if (SCORE_FIELD.equals(key)) {
        score = value != null ? Double.parseDouble(value.toString()) : null;
      } else {
        fields.put(key, value);
      }
    }

    return new Document(id, fields, score != null ? score : 1.0);
  }

  // RESP2/RESP3 Builder
  public static final Builder<HybridResult> HYBRID_RESULT_BUILDER = new Builder<HybridResult>() {
    private static final String TOTAL_RESULTS_STR = "total_results";
    private static final String EXECUTION_TIME_STR = "execution_time";
    private static final String RESULTS_STR = "results";
    private static final String WARNINGS_STR = "warnings";

    @Override
    public HybridResult build(Object data) {
      List list = (List) data;

      // Check if RESP3 (KeyValue) or RESP2 (flat list)
      if (!list.isEmpty() && list.get(0) instanceof KeyValue) {
        return buildResp3((List<KeyValue>) list);
      } else {
        return buildResp2(list);
      }
    }

    private HybridResult buildResp3(List<KeyValue> list) {
      long totalResults = -1;
      double executionTime = 0;
      List<Document> documents = null;
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
            documents = new ArrayList<>();
            List<Object> resultsList = (List<Object>) rawVal;
            for (Object resultObj : resultsList) {
              Map<String, Object> resultMap = BuilderFactory.ENCODED_OBJECT_MAP.build(resultObj);
              documents.add(mapToDocument(resultMap));
            }
            break;
          case WARNINGS_STR:
            warnings = BuilderFactory.STRING_LIST.build(rawVal);
            break;
        }
      }

      return new HybridResult(totalResults, executionTime, documents, warnings);
    }

    private HybridResult buildResp2(List list) {
      // RESP2 format: ["key1", value1, "key2", value2, ...]
      long totalResults = -1;
      double executionTime = 0;
      List<Document> documents = null;
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
            documents = new ArrayList<>();
            List<Object> resultsList = (List<Object>) rawVal;
            for (Object resultObj : resultsList) {
              Map<String, Object> resultMap = BuilderFactory.ENCODED_OBJECT_MAP.build(resultObj);
              documents.add(mapToDocument(resultMap));
            }
            break;
          case WARNINGS_STR:
            warnings = BuilderFactory.STRING_LIST.build(rawVal);
            break;
        }
      }

      return new HybridResult(totalResults, executionTime, documents, warnings);
    }
  };
}
