package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import java.util.ArrayList;
import java.util.List;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Arguments for the VSIM (Vector Similarity) clause in FT.HYBRID command. Configures vector search
 * with KNN or RANGE methods.
 */
@Experimental
public class HybridVectorParams implements IParams {

  private String field;
  private String vector;
  private VectorMethod method;
  private final List<String> filters = new ArrayList<>();
  private String scoreAlias;

  private HybridVectorParams() {
  }

  /**
   * @return a new {@link Builder} for {@link HybridVectorParams}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HybridVectorParams}.
   */
  public static class Builder {
    private final HybridVectorParams instance = new HybridVectorParams();

    /**
     * Build the {@link HybridVectorParams} instance.
     * @return the configured arguments
     */
    public HybridVectorParams build() {
      if (instance.field == null) {
        throw new IllegalArgumentException("Field is required for VSIM clause");
      }
      if (instance.vector == null) {
        throw new IllegalArgumentException("Vector is required for VSIM clause");
      }
      if (instance.method == null) {
        throw new IllegalArgumentException("Method (KNN or RANGE) is required for VSIM clause");
      }
      return instance;
    }

    /**
     * Set the vector field name.
     * @param field the field name (e.g., "@embedding")
     * @return this builder
     */
    public Builder field(String field) {
      instance.field = field;
      return this;
    }

    /**
     * Set the param name to reference the query vector BLOB.
     * @param vector the vector param name
     * @return this builder
     */
    public Builder vector(String vector) {
      instance.vector = vector;
      return this;
    }

    /**
     * Set the vector search method (KNN or RANGE).
     * @param method the vector search method
     * @return this builder
     */
    public Builder method(VectorMethod method) {
      instance.method = method;
      return this;
    }

    /**
     * Add a FILTER expression for pre-filtering documents before vector scoring. Can be called
     * multiple times to add multiple filters.
     * @param filter the filter expression
     * @return this builder
     */
    public Builder filter(String filter) {
      if (filter == null) {
        throw new IllegalArgumentException("Filter expression must not be null");
      }
      instance.filters.add(filter);
      return this;
    }

    /**
     * Set an alias for the vector distance score in the results.
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
    args.add(VSIM);
    args.add(field);
    if (vector.startsWith("$")) {
      args.add(vector);
    } else {
      args.add(String.format("$%s", vector));
    }

    method.addParams(args);

    // FILTER inside VSIM - can have multiple filters
    for (String filter : filters) {
      args.add(FILTER);
      args.add(filter);
    }

    if (scoreAlias != null) {
      args.add(YIELD_SCORE_AS);
      args.add(scoreAlias);
    }
  }

  /**
   * Base interface for vector search methods.
   */
  public interface VectorMethod extends IParams {
  }

  /**
   * KNN (K-Nearest Neighbors) vector search method.
   */
  public static class Knn implements VectorMethod {
    private final int k;
    private Integer efRuntime;

    private Knn(int k) {
      this.k = k;
    }

    /**
     * Create a KNN method with the specified K value.
     * @param k the number of nearest neighbors to return
     * @return a new Knn instance
     */
    public static Knn of(int k) {
      return new Knn(k);
    }

    /**
     * Set the EF_RUNTIME parameter for HNSW algorithm.
     * @param efRuntime the EF_RUNTIME value
     * @return this Knn instance
     */
    public Knn efRuntime(int efRuntime) {
      this.efRuntime = efRuntime;
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(KNN);
      int paramCount = efRuntime != null ? 4 : 2;
      args.add(paramCount);
      args.add(K);
      args.add(k);
      if (efRuntime != null) {
        args.add(EF_RUNTIME);
        args.add(efRuntime);
      }
    }
  }

  /**
   * RANGE vector search method.
   */
  public static class Range implements VectorMethod {
    private final double radius;
    private Double epsilon;

    private Range(double radius) {
      this.radius = radius;
    }

    /**
     * Create a RANGE method with the specified radius.
     * @param radius the search radius
     * @return a new Range instance
     */
    public static Range of(double radius) {
      return new Range(radius);
    }

    /**
     * Set the epsilon parameter for range search.
     * @param epsilon the epsilon value
     * @return this Range instance
     */
    public Range epsilon(double epsilon) {
      this.epsilon = epsilon;
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(RANGE);
      int paramCount = epsilon != null ? 4 : 2;
      args.add(paramCount);
      args.add(RADIUS);
      args.add(radius);
      if (epsilon != null) {
        args.add(EPSILON);
        args.add(epsilon);
      }
    }
  }
}
