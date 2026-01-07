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
 * HybridArgs args = HybridArgs.builder()
 *     .search(HybridSearchArgs.builder().query("comfortable shoes").build())
 *     .vectorSearch(HybridVectorArgs.builder()
 *         .field("@embedding")
 *         .vector(vectorBlob)
 *         .method(HybridVectorArgs.Knn.of(10))
 *         .build())
 *     .combine(CombineArgs.of(new CombineArgs.RRF()))
 *     .build();
 * </pre>
 *
 * @see HybridSearchArgs
 * @see HybridVectorArgs
 * @see CombineArgs
 * @see PostProcessingArgs
 */
@Experimental
public class HybridArgs implements IParams {

  private final List<HybridSearchArgs> searchArgs = new ArrayList<>();
  private final List<HybridVectorArgs> vectorArgs = new ArrayList<>();
  private CombineArgs combineArgs;
  private PostProcessingArgs postProcessingArgs;
  private final Map<String, Object> params = new HashMap<>();
  private Long timeout;

  private HybridArgs() {
  }

  /**
   * @return a new {@link Builder} for {@link HybridArgs}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HybridArgs}.
   */
  public static class Builder {
    private final HybridArgs instance = new HybridArgs();

    /**
     * Build the {@link HybridArgs} instance.
     *
     * @return the configured arguments
     */
    public HybridArgs build() {
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
     * Configure the SEARCH clause using {@link HybridSearchArgs}.
     * 
     * @param searchArgs the search arguments
     * @return this builder
     */
    public Builder search(HybridSearchArgs searchArgs) {
      if (searchArgs == null) {
        throw new IllegalArgumentException("Search args must not be null");
      }
      instance.searchArgs.add(searchArgs);
      return this;
    }

    /**
     * Configure the VSIM clause using {@link HybridVectorArgs}.
     * 
     * @param vectorArgs the vector search arguments
     * @return this builder
     */
    public Builder vectorSearch(HybridVectorArgs vectorArgs) {
      if (vectorArgs == null) {
        throw new IllegalArgumentException("Vector args must not be null");
      }
      instance.vectorArgs.add(vectorArgs);
      return this;
    }

    /**
     * Configure the COMBINE clause using {@link CombineArgs}.
     * 
     * @param combineArgs the combine arguments
     * @return this builder
     */
    public Builder combine(CombineArgs combineArgs) {
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
    public Builder postProcessing(PostProcessingArgs postProcessingArgs) {
      if (postProcessingArgs == null) {
        throw new IllegalArgumentException("PostProcessingArgs must not be null");
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
    for (HybridSearchArgs searchArg : searchArgs) {
      searchArg.addParams(args);
    }

    // VSIM clause(s)
    for (HybridVectorArgs vectorArg : vectorArgs) {
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

