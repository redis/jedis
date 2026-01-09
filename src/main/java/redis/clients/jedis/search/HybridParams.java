package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Argument list builder for the Redis {@code FT.HYBRID} command. Combines text search and vector similarity
 * search with configurable combination strategies and post-processing operations.
 *
 * <h3>Basic Usage:</h3>
 * <pre>
 * HybridParams params = HybridParams.builder()
 *     .search(HybridSearchParams.builder().query("comfortable shoes").build())
 *     .vectorSearch(HybridVectorParams.builder()
 *         .field("@embedding")
 *         .vector(vectorBlob)
 *         .method(HybridVectorParams.Knn.of(10))
 *         .build())
 *     .combine(CombineParams.of(new CombineParams.RRF()))
 *     .build();
 * </pre>
 *
 * @see HybridSearchParams
 * @see HybridVectorParams
 * @see CombineParams
 * @see PostProcessingParams
 */
@Experimental
public class HybridParams implements IParams {

  private final List<HybridSearchParams> searchArgs = new ArrayList<>();
  private final List<HybridVectorParams> vectorArgs = new ArrayList<>();
  private CombineParams combineArgs;
  private PostProcessingParams postProcessingArgs;
  private final Map<String, Object> params = new HashMap<>();
  private Long timeout;

  private HybridParams() {
  }

  /**
   * @return a new {@link Builder} for {@link HybridParams}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HybridParams}.
   */
  public static class Builder {
    private final HybridParams instance = new HybridParams();

    /**
     * Build the {@link HybridParams} instance.
     *
     * @return the configured arguments
     */
    public HybridParams build() {
      // Validate that both SEARCH and VSIM are configured (per FT.HYBRID requirements)
      if (instance.searchArgs.isEmpty()) {
        throw new IllegalArgumentException(
            "At least one SEARCH clause must be configured");
      }
      if (instance.vectorArgs.isEmpty()) {
        throw new IllegalArgumentException(
            "At least one VSIM clause must be configured");
      }
      return instance;
    }

    /**
     * Configure the SEARCH clause using {@link HybridSearchParams}.
     * 
     * @param searchArgs the search arguments
     * @return this builder
     */
    public Builder search(HybridSearchParams searchArgs) {
      if (searchArgs == null) {
        throw new IllegalArgumentException("Search args must not be null");
      }
      instance.searchArgs.add(searchArgs);
      return this;
    }

    /**
     * Configure the VSIM clause using {@link HybridVectorParams}.
     * 
     * @param vectorArgs the vector search arguments
     * @return this builder
     */
    public Builder vectorSearch(HybridVectorParams vectorArgs) {
      if (vectorArgs == null) {
        throw new IllegalArgumentException("Vector args must not be null");
      }
      instance.vectorArgs.add(vectorArgs);
      return this;
    }

    /**
     * Configure the COMBINE clause using {@link CombineParams}.
     * 
     * @param combineArgs the combine arguments
     * @return this builder
     */
    public Builder combine(CombineParams combineArgs) {
      if (combineArgs == null) {
        throw new IllegalArgumentException("Combine args must not be null");
      }
      instance.combineArgs = combineArgs;
      return this;
    }

    /**
     * Set the post-processing arguments.
     * 
     * @param postProcessingArgs the post-processing configuration
     * @return this builder
     */
    public Builder postProcessing(PostProcessingParams postProcessingArgs) {
      if (postProcessingArgs == null) {
        throw new IllegalArgumentException("PostProcessingParams must not be null");
      }
      instance.postProcessingArgs = postProcessingArgs;
      return this;
    }

    /**
     * Add a parameter for parameterized queries.
     * <p>
     * Parameters can be referenced in queries using {@code $name} syntax.
     * </p>
     * 
     * @param name the parameter name
     * @param value the parameter value
     * @return this builder
     */
    public Builder param(String name, Object value) {
      if (name == null) {
        throw new IllegalArgumentException("Parameter name must not be null");
      }
      if (value == null) {
        throw new IllegalArgumentException("Parameter value must not be null");
      }
      instance.params.put(name, value);
      return this;
    }

    /**
     * Set the maximum time to wait for the query to complete (in milliseconds).
     *
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
    for (HybridSearchParams searchArg : searchArgs) {
      searchArg.addParams(args);
    }

    // VSIM clause(s)
    for (HybridVectorParams vectorArg : vectorArgs) {
      vectorArg.addParams(args);
    }

    // COMBINE clause
    if (combineArgs != null) {
      args.add(COMBINE);
      combineArgs.addParams(args);
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

