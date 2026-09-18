package redis.clients.jedis.search.hybrid;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.Combiner;
import redis.clients.jedis.search.Combiners;
import redis.clients.jedis.util.JedisAsserts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Argument list builder for the Redis {@code FT.HYBRID} command. Combines text search and vector
 * similarity search with configurable combination strategies and post-processing operations.
 * <p>
 * <strong>Basic Usage:</strong>
 * </p>
 *
 * <pre>
 * FTHybridParams params = FTHybridParams.builder()
 *     .search(FTHybridSearchParams.builder().query("comfortable shoes").build())
 *     .vectorSearch(FTHybridVectorParams.builder().field("@embedding").vector("vec")
 *         .method(FTHybridVectorParams.Knn.of(10)).build())
 *     .combine(Combiners.rrf()).param("vec", vectorBlob).build();
 * </pre>
 *
 * @see FTHybridSearchParams
 * @see FTHybridVectorParams
 * @see Combiner
 * @see Combiners
 * @see FTHybridPostProcessingParams
 */
@Experimental
public class FTHybridParams implements IParams {

  private final List<FTHybridSearchParams> searchArgs = new ArrayList<>();
  private final List<FTHybridVectorParams> vectorArgs = new ArrayList<>();
  private Combiner combiner;
  private FTHybridPostProcessingParams postProcessingArgs;
  private final Map<String, Object> params = new HashMap<>();
  private Long timeout;

  private FTHybridParams() {
  }

  /**
   * @return a new {@link Builder} for {@link FTHybridParams}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link FTHybridParams}.
   */
  public static class Builder {
    private final FTHybridParams instance = new FTHybridParams();

    /**
     * Build the {@link FTHybridParams} instance.
     * @return the configured arguments
     */
    public FTHybridParams build() {
      // Validate that both SEARCH and VSIM are configured (per FT.HYBRID requirements)
      JedisAsserts.isTrue(!instance.searchArgs.isEmpty(),
        "At least one SEARCH clause must be configured");
      JedisAsserts.isTrue(!instance.vectorArgs.isEmpty(),
        "At least one VSIM clause must be configured");

      return instance;
    }

    /**
     * Configure the SEARCH clause using {@link FTHybridSearchParams}.
     * @param searchArgs the search arguments
     * @return this builder
     */
    public Builder search(FTHybridSearchParams searchArgs) {
      JedisAsserts.notNull(searchArgs, "Search args must not be null");

      instance.searchArgs.add(searchArgs);
      return this;
    }

    /**
     * Configure the VSIM clause using {@link FTHybridVectorParams}.
     * @param vectorArgs the vector search arguments
     * @return this builder
     */
    public Builder vectorSearch(FTHybridVectorParams vectorArgs) {
      JedisAsserts.notNull(vectorArgs, "Vector args must not be null");

      instance.vectorArgs.add(vectorArgs);
      return this;
    }

    /**
     * Configure the COMBINE clause using a {@link Combiner}.
     * @param combiner the combiner (e.g., {@code Combiners.rrf()} or {@code Combiners.linear()})
     * @return this builder
     * @see Combiners
     */
    public Builder combine(Combiner combiner) {
      JedisAsserts.notNull(combiner, "Combiner must not be null");

      instance.combiner = combiner;
      return this;
    }

    /**
     * Set the post-processing arguments.
     * @param postProcessingArgs the post-processing configuration
     * @return this builder
     */
    public Builder postProcessing(FTHybridPostProcessingParams postProcessingArgs) {
      JedisAsserts.notNull(postProcessingArgs, "PostProcessingParams must not be null");

      instance.postProcessingArgs = postProcessingArgs;
      return this;
    }

    /**
     * Add a parameter for parameterized queries.
     * <p>
     * Parameters can be referenced in queries using {@code $name} syntax.
     * </p>
     * @param name the parameter name
     * @param value the parameter value
     * @return this builder
     */
    public Builder param(String name, Object value) {
      JedisAsserts.notNull(name, "Parameter name must not be null");
      JedisAsserts.notNull(value, "Parameter value must not be null");

      instance.params.put(name, value);
      return this;
    }

    /**
     * Set the maximum time to wait for the query to complete (in milliseconds).
     * @param timeout the timeout in milliseconds
     * @return this builder
     */
    public Builder timeout(long timeout) {
      instance.timeout = timeout;
      return this;
    }
  }

  @Override
  public void addParams(CommandArguments args) {
    // SEARCH clause(s)
    for (FTHybridSearchParams searchArg : searchArgs) {
      searchArg.addParams(args);
    }

    // VSIM clause(s)
    for (FTHybridVectorParams vectorArg : vectorArgs) {
      vectorArg.addParams(args);
    }

    // COMBINE clause
    if (combiner != null) {
      args.add(COMBINE);
      combiner.addParams(args);
    }

    // Post-processing operations (LOAD, GROUPBY, APPLY, SORTBY, FILTER, LIMIT)
    if (postProcessingArgs != null) {
      postProcessingArgs.addParams(args);
    }

    // PARAMS clause
    if (!params.isEmpty()) {
      args.add(PARAMS);
      args.add(params.size() * 2);
      for (Map.Entry<String, Object> entry : params.entrySet()) {
        args.add(entry.getKey());
        args.add(entry.getValue());
      }
    }

    // TIMEOUT clause
    if (timeout != null) {
      args.add(TIMEOUT);
      args.add(timeout);
    }
  }
}
