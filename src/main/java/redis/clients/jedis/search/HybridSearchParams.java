package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Arguments for the SEARCH clause in FT.HYBRID command. Configures text search with optional scorer
 * and score aliasing.
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
     * @param query the query string
     * @return this builder
     */
    public Builder query(String query) {
      instance.query = query;
      return this;
    }

    /**
     * Set the scorer for text search.
     * @param scorer the scorer configuration
     * @return this builder
     */
    public Builder scorer(Scorer scorer) {
      instance.scorer = scorer;
      return this;
    }

    /**
     * Set an alias for the text search score in the results.
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
      args.add(scorer.function);
    }

    if (scoreAlias != null) {
      args.add(YIELD_SCORE_AS);
      args.add(scoreAlias);
    }
  }

  // We are using a wrapper, because in the future the server will allow params
  /**
   * Represents a scoring function configuration for text search.
   */
  public static class Scorer {
    private final String function;

    private Scorer(String function) {
      this.function = function;
    }

    /**
     * Create a scorer with the given function.
     * @param function the scoring function
     * @return a new Scorer instance
     */
    public static Scorer of(String function) {
      return new Scorer(function);
    }
  }
}
