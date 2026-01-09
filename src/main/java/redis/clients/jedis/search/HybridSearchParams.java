package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Arguments for the SEARCH clause in FT.HYBRID command.
 * Configures text search with optional scorer and score aliasing.
 */
@Experimental
public class HybridSearchParams implements IParams {

  private String query;
  private Scorer scorer;
  private String scoreAlias;

  private HybridSearchParams() {
  }

  /**
   * @return a new {@link Builder} for {@link HybridSearchParams}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HybridSearchParams}.
   */
  public static class Builder {
    private final HybridSearchParams instance = new HybridSearchParams();

    /**
     * Build the {@link HybridSearchParams} instance.
     * 
     * @return the configured arguments
     */
    public HybridSearchParams build() {
      if (instance.query == null) {
        throw new IllegalArgumentException("Query is required for SEARCH clause");
      }
      return instance;
    }

    /**
     * Set the search query string.
     * 
     * @param query the query string
     * @return this builder
     */
    public Builder query(String query) {
      instance.query = query;
      return this;
    }

    /**
     * Set the scorer for text search.
     * 
     * @param scorer the scorer configuration
     * @return this builder
     */
    public Builder scorer(Scorer scorer) {
      instance.scorer = scorer;
      return this;
    }

    /**
     * Set an alias for the text search score in the results.
     * 
     * @param scoreAlias the score alias name
     * @return this builder
     */
    public Builder scoreAlias(String scoreAlias) {
      instance.scoreAlias = scoreAlias;
      return this;
    }
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(SEARCH);
    args.add(query);

    if (scorer != null) {
      args.add(SCORER);
      args.add(scorer.function.name());
      if (scorer.params != null && scorer.params.length > 0) {
        for (Object param : scorer.params) {
          args.add(param);
        }
      }
    }

    if (scoreAlias != null) {
      args.add(YIELD_SCORE_AS);
      args.add(scoreAlias);
    }
  }

  /**
   * Represents a scoring function configuration for text search.
   */
  public static class Scorer {
    private final ScoringFunction function;
    private final Object[] params;

    private Scorer(ScoringFunction function, Object... params) {
      this.function = function;
      this.params = params;
    }

    /**
     * Create a scorer with the given function and optional parameters.
     * 
     * @param function the scoring function
     * @param params optional parameters for the scoring function
     * @return a new Scorer instance
     */
    public static Scorer of(ScoringFunction function, Object... params) {
      return new Scorer(function, params);
    }
  }

  /**
   * Enumeration of available scoring functions for text search.
   */
  public enum ScoringFunction {
    /** Term Frequency-Inverse Document Frequency */
    TFIDF,
    /** TF-IDF with document normalization */
    TFIDF_DOCNORM("TFIDF.DOCNORM"),
    /** Best Matching 25 */
    BM25,
    /** Disable scoring */
    DISMAX,
    /** Document only scoring */
    DOCSCORE,
    /** Hamming distance */
    HAMMING;

    private final String value;

    ScoringFunction() {
      this.value = name();
    }

    ScoringFunction(String value) {
      this.value = value;
    }
  }
}

