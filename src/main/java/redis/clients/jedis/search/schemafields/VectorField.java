package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NOINDEX;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.VECTOR;

import java.util.LinkedHashMap;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class VectorField extends SchemaField {

  public enum VectorAlgorithm {
    FLAT,
    HNSW
  }

  private boolean noIndex;
  private final VectorAlgorithm algorithm;
  private final Map<String, Object> attributes;

  public VectorField(String fieldName, VectorAlgorithm algorithm) {
    this(fieldName, algorithm, new LinkedHashMap<>());
  }

  public VectorField(FieldName fieldName, VectorAlgorithm algorithm) {
    this(fieldName, algorithm, new LinkedHashMap<>());
  }

  public VectorField(String fieldName, VectorAlgorithm algorithm, Map<String, Object> attributes) {
    super(fieldName);
    this.algorithm = algorithm;
    this.attributes = attributes;
  }

  public VectorField(FieldName fieldName, VectorAlgorithm algorithm, Map<String, Object> attributes) {
    super(fieldName);
    this.algorithm = algorithm;
    this.attributes = attributes;
  }

  public static VectorField vectorField(String fieldName, VectorAlgorithm algorithm) {
    return new VectorField(fieldName, algorithm);
  }

  public static VectorField vectorField(FieldName fieldName, VectorAlgorithm algorithm) {
    return new VectorField(fieldName, algorithm);
  }

  @Override
  public VectorField as(String attribute) {
    super.as(attribute);
    return this;
  }

  /**
   * Avoid indexing.
   */
  public VectorField noIndex() {
    this.noIndex = true;
    return this;
  }

  public VectorField addAttribute(String name, Object value) {
    this.attributes.put(name, value);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(VECTOR);

    if (noIndex) {
      args.add(NOINDEX);
    }

    args.add(algorithm);
    args.add(attributes.size() * 2);
    attributes.forEach((name, value) -> args.add(name).add(value));
  }
}
