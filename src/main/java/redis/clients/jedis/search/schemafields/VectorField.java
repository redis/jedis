package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.INDEXMISSING;
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

  private final VectorAlgorithm algorithm;
  private final Map<String, Object> attributes;

  private boolean indexMissing;
  // private boolean noIndex; // throws Field `NOINDEX` does not have a type

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

  @Override
  public VectorField as(String attribute) {
    super.as(attribute);
    return this;
  }

  public VectorField indexMissing() {
    this.indexMissing = true;
    return this;
  }

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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private FieldName fieldName;
    private VectorAlgorithm algorithm;
    private Map<String, Object> attributes;

    private Builder() {
    }

    public VectorField build() {
      if (fieldName == null || algorithm == null || attributes == null || attributes.isEmpty()) {
        throw new IllegalArgumentException("All required VectorField parameters are not set.");
      }
      return new VectorField(fieldName, algorithm, attributes);
    }

    public Builder fieldName(String fieldName) {
      this.fieldName = FieldName.of(fieldName);
      return this;
    }

    public Builder fieldName(FieldName fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public Builder as(String attribute) {
      this.fieldName.as(attribute);
      return this;
    }

    public Builder algorithm(VectorAlgorithm algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public Builder attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder addAttribute(String name, Object value) {
      if (this.attributes == null) {
        this.attributes = new LinkedHashMap<>();
      }
      this.attributes.put(name, value);
      return this;
    }
  }
}
