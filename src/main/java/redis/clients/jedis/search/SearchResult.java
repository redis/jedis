package redis.clients.jedis.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.util.KeyValue;

/**
 * SearchResult encapsulates the returned result from a search query. It contains publicly
 * accessible fields for the total number of results, and an array of {@link Document} objects
 * containing the actual returned documents.
 */
public class SearchResult {

  private final long totalResults;
  private final List<Document> documents;

  private SearchResult(long totalResults, List<Document> documents) {
    this.totalResults = totalResults;
    this.documents = documents;
  }

  public long getTotalResults() {
    return totalResults;
  }

  public List<Document> getDocuments() {
    return Collections.unmodifiableList(documents);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{Total results:" + totalResults
        + ", Documents:" + documents + "}";
  }

  public static class SearchResultBuilder extends Builder<SearchResult> {

    private final boolean hasContent;
    private final boolean hasScores;
    private final boolean decode;

    private final Map<String, Boolean> isFieldDecode;

    public SearchResultBuilder(boolean hasContent, boolean hasScores, boolean decode) {
      this(hasContent, hasScores, decode, null);
    }

    public SearchResultBuilder(boolean hasContent, boolean hasScores, boolean decode,
        Map<String, Boolean> isFieldDecode) {
      this.hasContent = hasContent;
      this.hasScores = hasScores;
      this.decode = decode;
      this.isFieldDecode = isFieldDecode;
    }

    @Override
    public SearchResult build(Object data) {
      List<Object> resp = (List<Object>) data;

      int step = 1;
      int scoreOffset = 0;
      int contentOffset = 1;
      if (hasScores) {
        step += 1;
        scoreOffset = 1;
        contentOffset += 1;
      }
      if (hasContent) {
        step += 1;
      }

      // the first element is always the number of results
      long totalResults = (Long) resp.get(0);
      List<Document> documents = new ArrayList<>(resp.size() - 1);

      for (int i = 1; i < resp.size(); i += step) {

        String id = BuilderFactory.STRING.build(resp.get(i));
        double score = hasScores ? BuilderFactory.DOUBLE.build(resp.get(i + scoreOffset)) : 1.0;
        List<byte[]> fields = hasContent ? (List<byte[]>) resp.get(i + contentOffset) : null;

        documents.add(Document.load(id, score, fields, decode, isFieldDecode));
      }

      return new SearchResult(totalResults, documents);
    }
  }

  /// RESP3 -->
  // TODO: final
  public static Builder<SearchResult> SEARCH_RESULT_BUILDER
      = new PerFieldDecoderSearchResultBuilder(Document.SEARCH_DOCUMENT);

  @Internal
  public static final class PerFieldDecoderSearchResultBuilder extends Builder<SearchResult> {

    private static final String TOTAL_RESULTS_STR = "total_results";
    private static final String RESULTS_STR = "results";

    private final Builder<Document> documentBuilder;

    public PerFieldDecoderSearchResultBuilder(Map<String, Boolean> isFieldDecode) {
      this(new Document.PerFieldDecoderDocumentBuilder(isFieldDecode));
    }

    private PerFieldDecoderSearchResultBuilder(Builder<Document> builder) {
      this.documentBuilder = Objects.requireNonNull(builder);
    }

    @Override
    public SearchResult build(Object data) {
      List<KeyValue> list = (List<KeyValue>) data;
      long totalResults = -1;
      List<Document> results = null;
      for (KeyValue kv : list) {
        String key = BuilderFactory.STRING.build(kv.getKey());
        switch (key) {
          case TOTAL_RESULTS_STR:
            totalResults = BuilderFactory.LONG.build(kv.getValue());
            break;
          case RESULTS_STR:
            results = ((List<Object>) kv.getValue()).stream()
                .map(documentBuilder::build)
                .collect(Collectors.toList());
            break;
        }
      }
      return new SearchResult(totalResults, results);
    }
  };
  /// <-- RESP3
}
