package redis.clients.jedis.search;

import redis.clients.jedis.util.SafeEncoder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.KeyValue;

/**
 * Document represents a single indexed document or entity in the engine
 */
public class Document implements Serializable {

  private static final long serialVersionUID = 4884173545291367373L;

  private final String id;
  private Double score;
  private final Map<String, Object> fields;

  public Document(String id) {
    this(id, 1.0);
  }

  public Document(String id, double score) {
    this(id, new HashMap<>(), score);
  }

  public Document(String id, Map<String, Object> fields) {
    this(id, fields, 1.0f);
  }

  public Document(String id, Map<String, Object> fields, double score) {
    this.id = id;
    this.fields = fields;
    this.score = score;
  }

  private Document(String id, Double score, Map<String, Object> fields) {
    this.id = id;
    this.score = score;
    this.fields = fields;
  }

  public Iterable<Map.Entry<String, Object>> getProperties() {
    return fields.entrySet();
  }

  /**
   * @return the document's id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the document's score
   */
  public Double getScore() {
    return score;
  }

  /**
   * return the property value inside a key
   *
   * @param key key of the property
   *
   * @return the property value
   */
  public Object get(String key) {
    return fields.get(key);
  }

  /**
   * return the property value inside a key
   *
   * @param key key of the property
   *
   * @return the property value
   */
  public String getString(String key) {
    Object value = fields.get(key);
    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return (String) value;
    } else if (value instanceof byte[]) {
      return SafeEncoder.encode((byte[]) value);
    } else {
      return String.valueOf(value);
    }
  }

  public boolean hasProperty(String key) {
    return fields.containsKey(key);
  }

  // TODO: private ??
  public Document set(String key, Object value) {
    fields.put(key, value);
    return this;
  }

  /**
   * Set the document's score
   *
   * @param score new score to set
   * @return the document itself
   * @deprecated
   */
  @Deprecated
  public Document setScore(float score) {
    this.score = (double) score;
    return this;
  }

  @Override
  public String toString() {
    return "id:" + this.getId() + ", score: " + this.getScore() +
            ", properties:" + this.getProperties();
  }

  /// RESP2 -->
  public static Document load(String id, double score, byte[] payload, List<byte[]> fields) {
    return Document.load(id, score, fields, true);
  }

  public static Document load(String id, double score, List<byte[]> fields, boolean decode) {
    return load(id, score, fields, decode, null);
  }

  /**
   * Parse document object from FT.SEARCH reply.
   * @param id
   * @param score
   * @param fields
   * @param decode
   * @param isFieldDecode checked only if {@code decode=true}
   * @return document
   */
  public static Document load(String id, double score, List<byte[]> fields, boolean decode,
      Map<String, Boolean> isFieldDecode) {
    Document ret = new Document(id, score);
    if (fields != null) {
      for (int i = 0; i < fields.size(); i += 2) {
        byte[] rawKey = fields.get(i);
        byte[] rawValue = fields.get(i + 1);
        String key = SafeEncoder.encode(rawKey);
        Object value = rawValue == null ? null
            : (decode && (isFieldDecode == null || !Boolean.FALSE.equals(isFieldDecode.get(key))))
            ? SafeEncoder.encode(rawValue) : rawValue;
        ret.set(key, value);
      }
    }
    return ret;
  }
  /// <-- RESP2

  /// RESP3 -->
  // TODO: final
  static Builder<Document> SEARCH_DOCUMENT = new PerFieldDecoderDocumentBuilder((Map) null);

  static final class PerFieldDecoderDocumentBuilder extends Builder<Document> {

    private static final String ID_STR = "id";
    private static final String SCORE_STR = "score";
    private static final String FIELDS_STR = "extra_attributes";

    private final Map<String, Boolean> isFieldDecode;

    public PerFieldDecoderDocumentBuilder(Map<String, Boolean> isFieldDecode) {
      this.isFieldDecode = isFieldDecode != null ? isFieldDecode : Collections.emptyMap();
    }

    @Override
    public Document build(Object data) {
      List<KeyValue> list = (List<KeyValue>) data;
      String id = null;
      Double score = null;
      Map<String, Object> fields = null;
      for (KeyValue kv : list) {
        String key = BuilderFactory.STRING.build(kv.getKey());
        switch (key) {
          case ID_STR:
            id = BuilderFactory.STRING.build(kv.getValue());
            break;
          case SCORE_STR:
            score = BuilderFactory.DOUBLE.build(kv.getValue());
            break;
          case FIELDS_STR:
            fields = makeFieldsMap(isFieldDecode, kv.getValue());
            break;
        }
      }
      return new Document(id, score, fields);
    }
  };

  private static Map<String, Object> makeFieldsMap(Map<String, Boolean> isDecode, Object data) {
    if (data == null) return null;

    final List<KeyValue> list = (List) data;

    Map<String, Object> map = new HashMap<>(list.size(), 1f);
    list.stream().filter((kv) -> (kv != null && kv.getKey() != null && kv.getValue() != null))
        .forEach((kv) -> {
          String key = BuilderFactory.STRING.build(kv.getKey());
          map.put(key,
              (Boolean.FALSE.equals(isDecode.get(key)) ? BuilderFactory.RAW_OBJECT
                  : BuilderFactory.AGGRESSIVE_ENCODED_OBJECT).build(kv.getValue()));
        });
    return map;
  }
  /// <-- RESP3
}
