package redis.clients.jedis.search;

import redis.clients.jedis.util.SafeEncoder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Document represents a single indexed document or entity in the engine
 */
public class Document implements Serializable {

  private static final long serialVersionUID = 4884173545291367373L;

  private final String id;
  private double score;
  private byte[] payload;
  private String sortKey;
  private List<byte[]> explainScore;
  private final Map<String, Object> properties;

  public Document(String id, double score) {
    this(id, new HashMap<>(), score);
  }

  public Document(String id) {
    this(id, 1.0);
  }

  public Document(String id, Map<String, Object> fields) {
    this(id, fields, 1.0f);
  }

  public Document(String id, Map<String, Object> fields, double score) {
    this(id, fields, score, null);
  }

  public Document(String id, Map<String, Object> fields, double score, byte[] payload) {
    this(id, fields, score, payload, null);
  }

  public Document(String id, Map<String, Object> fields, double score, byte[] payload, String sortKey) {
    this(id, fields, score, payload, sortKey, null);
  }

  public Document(String id, Map<String, Object> fields, double score, byte[] payload, String sortKey, List<byte[]> explainScore) {
    this.id = id;
    this.properties = new HashMap<>(fields);
    this.score = score;
    this.payload = payload;
    this.sortKey = sortKey;
    this.explainScore = explainScore;
  }

  public Iterable<Map.Entry<String, Object>> getProperties() {
    return properties.entrySet();
  }

  public static Document load(String id, double score, byte[] payload, String sortKey, List<byte[]> explainScore, List<byte[]> fields) {
    return Document.load(id, score, payload, sortKey, explainScore, fields, true);
  }

  public static Document load(String id, double score, byte[] payload, String sortKey, List<byte[]> explainScore, List<byte[]> fields, boolean decode) {
    Document ret = new Document(id, score);
    ret.payload = payload;
    ret.sortKey = sortKey;
    ret.explainScore = explainScore;
    if (fields != null) {
      for (int i = 0; i < fields.size(); i += 2) {
        ret.set(SafeEncoder.encode(fields.get(i)), decode ? SafeEncoder.encode(fields.get(i + 1)) : fields.get(i + 1));
      }
    }
    return ret;
  }

  public Document set(String key, Object value) {
    properties.put(key, value);
    return this;
  }

  /**
   * return the property value inside a key
   *
   * @param key key of the property
   *
   * @return the property value
   */
  public Object get(String key) {
    return properties.get(key);
  }

  /**
   * return the property value inside a key
   *
   * @param key key of the property
   *
   * @return the property value
   */
  public String getString(String key) {
    Object value = properties.get(key);
    if (value instanceof String) {
      return (String) value;
    }
    return value instanceof byte[] ? SafeEncoder.encode((byte[]) value) : value.toString();
  }

  /**
   * @return the document's score
   */
  public double getScore() {
    return score;
  }

  public byte[] getPayload() {
    return payload;
  }

  public String getSortKey() {
    return sortKey;
  }

  public List<byte[]> getExplainScore() {
    return explainScore;
  }

  /**
   * Set the document's score
   *
   * @param score new score to set
   * @return the document itself
   */
  public Document setScore(float score) {
    this.score = score;
    return this;
  }

  /**
   * @return the document's id
   */
  public String getId() {
    return id;
  }

  public boolean hasProperty(String key) {
    return properties.containsKey(key);
  }

  @Override
  public String toString() {
    return "id:" + this.getId() + ", score: " + this.getScore() +
            ", payload:" + SafeEncoder.encode(this.getPayload()) +
            ", sortKey:" + this.getSortKey() +
            ", explainScore:" + SafeEncoder.encodeObject(this.getExplainScore()) +
            ", properties:" + this.getProperties();
  }
}
