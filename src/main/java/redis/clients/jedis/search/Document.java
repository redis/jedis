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
    this.id = id;
    this.properties = new HashMap<>(fields);
    this.score = score;
    this.payload = payload;
  }

  public Iterable<Map.Entry<String, Object>> getProperties() {
    return properties.entrySet();
  }

  public static Document load(String id, double score, byte[] payload, List<byte[]> fields) {
    return Document.load(id, score, payload, fields, true);
  }

  public static Document load(String id, double score, byte[] payload, List<byte[]> fields, boolean decode) {
    Document ret = new Document(id, score);
    ret.payload = payload;
    if (fields != null) {
      for (int i = 0; i < fields.size(); i += 2) {
        byte[] rawKey = fields.get(i);
        byte[] rawValue = fields.get(i + 1);
        String key = SafeEncoder.encode(rawKey);
        Object value = rawValue == null ? null : decode ? SafeEncoder.encode(rawValue) : rawValue;
        ret.set(key, value);
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
            ", payload:" + (this.getPayload() == null ? "null" : SafeEncoder.encode(this.getPayload())) +
            ", properties:" + this.getProperties();
  }
}
