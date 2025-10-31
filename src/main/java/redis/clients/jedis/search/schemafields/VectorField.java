package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.INDEXMISSING;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.VECTOR;

import java.util.LinkedHashMap;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Represents a vector field in a Redis search index schema for performing semantic vector searches.
 * Vector fields enable high-performance similarity searches over vector embeddings using various
 * algorithms and distance metrics.
 *
 * @see <a href="https://redis.io/docs/latest/develop/ai/search-and-query/vectors/">Redis Vector Search Documentation</a>
 */
public class VectorField extends SchemaField {

  /**
   * Enumeration of supported vector indexing algorithms in Redis.
   * Each algorithm has different performance characteristics and use cases.
   */
  public enum VectorAlgorithm implements Rawable {

    /**
     * FLAT algorithm provides exact vector search with perfect accuracy.
     * Best suited for smaller datasets (&lt; 1M vectors) where search accuracy
     * is more important than search latency.
     */
    FLAT("FLAT"),

    /**
     * HNSW (Hierarchical Navigable Small World) algorithm provides approximate
     * vector search with configurable accuracy-performance trade-offs.
     * Best suited for larger datasets (&gt; 1M vectors) where search performance
     * and scalability are more important than perfect accuracy.
     */
    HNSW("HNSW"),

    /**
     * SVS_VAMANA algorithm provides high-performance approximate vector search
     * optimized for specific use cases with advanced compression and optimization features.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>High-performance approximate search</li>
     *   <li>Support for vector compression (LVQ, LeanVec)</li>
     *   <li>Configurable graph construction and search parameters</li>
     *   <li>Optimized for Intel platforms with fallback support</li>
     * </ul>
     *
     * <p>Note: This algorithm may have specific requirements and limitations.
     * Consult the Redis documentation for detailed usage guidelines.
     */
    SVS_VAMANA("SVS-VAMANA");

    private final byte[] raw;

    /**
     * Creates a VectorAlgorithm enum value.
     *
     * @param redisParamName the Redis parameter name for this algorithm
     */
    VectorAlgorithm(String redisParamName) {
      raw = SafeEncoder.encode(redisParamName);
    }

    /**
     * Returns the raw byte representation of the algorithm name for Redis commands.
     *
     * @return the raw bytes of the algorithm name
     */
    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  private final VectorAlgorithm algorithm;
  private final Map<String, Object> attributes;

  private boolean indexMissing;
  // private boolean noIndex; // throws Field `NOINDEX` does not have a type

  /**
   * Creates a new VectorField with the specified field name, algorithm, and attributes.
   *
   * @param fieldName the name of the vector field in the index
   * @param algorithm the vector indexing algorithm to use
   * @param attributes the algorithm-specific configuration attributes
   * @throws IllegalArgumentException if required attributes are missing or invalid
   */
  public VectorField(String fieldName, VectorAlgorithm algorithm, Map<String, Object> attributes) {
    super(fieldName);
    this.algorithm = algorithm;
    this.attributes = attributes;
  }

  /**
   * Creates a new VectorField with the specified field name, algorithm, and attributes.
   *
   * @param fieldName the field name object containing the field name and optional alias
   * @param algorithm the vector indexing algorithm to use
   * @param attributes the algorithm-specific configuration attributes
   * @throws IllegalArgumentException if required attributes are missing or invalid
   * @see #VectorField(String, VectorAlgorithm, Map) for detailed attribute documentation
   */
  public VectorField(FieldName fieldName, VectorAlgorithm algorithm, Map<String, Object> attributes) {
    super(fieldName);
    this.algorithm = algorithm;
    this.attributes = attributes;
  }

  /**
   * Sets an alias for this field that can be used in queries instead of the field name.
   * This is useful when the field name contains special characters or when you want
   * to use a shorter name in queries.
   *
   * @param attribute the alias name to use for this field in queries
   * @return this VectorField instance for method chaining
   */
  @Override
  public VectorField as(String attribute) {
    super.as(attribute);
    return this;
  }

  /**
   * Configures the field to handle missing values during indexing.
   * When enabled, documents that don't contain this vector field will still be indexed,
   * but won't participate in vector searches.
   *
   * <p>This is useful when not all documents in your dataset contain vector embeddings,
   * but you still want to index them for other types of searches.
   *
   * @return this VectorField instance for method chaining
   */
  public VectorField indexMissing() {
    this.indexMissing = true;
    return this;
  }

  /**
   * Adds the vector field parameters to the Redis command arguments.
   * This method is used internally when creating the search index.
   *
   * @param args the command arguments to add parameters to
   */
  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(VECTOR);

    args.add(algorithm);
    args.add(attributes.size() << 1);
    attributes.forEach((name, value) -> args.add(name).add(value));

    if (indexMissing) {
      args.add(INDEXMISSING);
    }
  }

  /**
   * Creates a new Builder instance for constructing VectorField objects using the builder pattern.
   * The builder pattern provides a fluent interface for setting field properties and is especially
   * useful when dealing with complex vector field configurations.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing VectorField instances using the builder pattern.
   * Provides a fluent interface for setting vector field properties and attributes.
   *
   * <p>Example usage:
   * <pre>{@code
   * VectorField field = VectorField.builder()
   *     .fieldName("product_embedding")
   *     .algorithm(VectorAlgorithm.HNSW)
   *     .addAttribute("TYPE", "FLOAT32")
   *     .addAttribute("DIM", 768)
   *     .addAttribute("DISTANCE_METRIC", "COSINE")
   *     .addAttribute("M", 32)
   *     .addAttribute("EF_CONSTRUCTION", 200)
   *     .build();
   * }</pre>
   */
  public static class Builder {

    private FieldName fieldName;
    private VectorAlgorithm algorithm;
    private Map<String, Object> attributes;

    /**
     * Private constructor to enforce use of the static builder() method.
     */
    private Builder() {
    }

    /**
     * Builds and returns a new VectorField instance with the configured properties.
     *
     * @return a new VectorField instance
     * @throws IllegalArgumentException if required parameters (fieldName, algorithm, or attributes) are not set
     */
    public VectorField build() {
      if (fieldName == null || algorithm == null || attributes == null || attributes.isEmpty()) {
        throw new IllegalArgumentException("All required VectorField parameters are not set.");
      }
      return new VectorField(fieldName, algorithm, attributes);
    }

    /**
     * Sets the field name for the vector field.
     *
     * @param fieldName the name of the vector field in the index
     * @return this Builder instance for method chaining
     */
    public Builder fieldName(String fieldName) {
      this.fieldName = FieldName.of(fieldName);
      return this;
    }

    /**
     * Sets the field name using a FieldName object.
     *
     * @param fieldName the FieldName object containing the field name and optional alias
     * @return this Builder instance for method chaining
     */
    public Builder fieldName(FieldName fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    /**
     * Sets an alias for the field that can be used in queries.
     *
     * @param attribute the alias name to use for this field in queries
     * @return this Builder instance for method chaining
     */
    public Builder as(String attribute) {
      this.fieldName.as(attribute);
      return this;
    }

    /**
     * Sets the vector indexing algorithm to use.
     *
     * @param algorithm the vector algorithm (FLAT, HNSW, or SVS_VAMANA)
     * @return this Builder instance for method chaining
     */
    public Builder algorithm(VectorAlgorithm algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    /**
     * Sets all vector field attributes at once, replacing any previously set attributes.
     *
     * @param attributes a map of attribute names to values
     * @return this Builder instance for method chaining
     */
    public Builder attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    /**
     * Adds a single attribute to the vector field configuration.
     * If this is the first attribute added, initializes the attributes map.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @return this Builder instance for method chaining
     */
    public Builder addAttribute(String name, Object value) {
      if (this.attributes == null) {
        this.attributes = new LinkedHashMap<>();
      }
      this.attributes.put(name, value);
      return this;
    }
  }
}
