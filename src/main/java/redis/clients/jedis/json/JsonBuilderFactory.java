package redis.clients.jedis.json;

import static redis.clients.jedis.BuilderFactory.STRING;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Builder;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public final class JsonBuilderFactory {

  public static final Builder<Class<?>> JSON_TYPE = new Builder<Class<?>>() {
    @Override
    public Class<?> build(Object data) {
      if (data == null) return null;
      String str = STRING.build(data);
      switch (str) {
        case "null":
          return null;
        case "boolean":
          return boolean.class;
        case "integer":
          return int.class;
        case "number":
          return float.class;
        case "string":
          return String.class;
        case "object":
          return Object.class;
        case "array":
          return List.class;
        default:
          throw new JedisException("Unknown type: " + str);
      }
    }

    @Override
    public String toString() {
      return "Class<?>";
    }
  };

  public static final Builder<List<Class<?>>> JSON_TYPE_LIST = new Builder<List<Class<?>>>() {
    @Override
    public List<Class<?>> build(Object data) {
      List<Object> list = (List<Object>) data;
      List<Class<?>> classes = new ArrayList<>(list.size());
      for (Object elem : list) {
        try {
          classes.add(JSON_TYPE.build(elem));
        } catch (JedisException je) {
          classes.add(null);
        }
      }
      return classes;
    }
  };

  public static final Builder<List<List<Class<?>>>> JSON_TYPE_RESPONSE_RESP3 = new Builder<List<List<Class<?>>>>() {
    @Override
    public List<List<Class<?>>> build(Object data) {
      return ((List<Object>) data).stream().map(JSON_TYPE_LIST::build).collect(Collectors.toList());
    }
  };

  public static final Builder<List<Class<?>>> JSON_TYPE_RESPONSE_RESP3_COMPATIBLE = new Builder<List<Class<?>>>() {
    @Override
    public List<Class<?>> build(Object data) {
      List<List<Class<?>>> fullReply = JSON_TYPE_RESPONSE_RESP3.build(data);
      return fullReply == null ? null : fullReply.get(0);
    }
  };

  public static final Builder<Object> JSON_OBJECT = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      if (data == null) {
        return null;
      }

      if (!(data instanceof byte[])) {
        return data;
      }
      String str = STRING.build(data);
      if (str.charAt(0) == '{') {
        try {
          return new JSONObject(str);
        } catch (Exception ex) {
        }
      } else if (str.charAt(0) == '[') {
        try {
          return new JSONArray(str);
        } catch (Exception ex) {
        }
      }
      return str;
    }
  };

  public static final Builder<JSONArray> JSON_ARRAY = new Builder<JSONArray>() {
    @Override
    public JSONArray build(Object data) {
      if (data == null) {
        return null;
      }
      String str = STRING.build(data);
      try {
        return new JSONArray(str);
      } catch (JSONException ex) { // This is not necessary but we are doing this
        // just to make it safe for com.vaadin.external.google:android-json library
        throw new JedisException(ex);
      }
    }
  };

  /**
   * Builder that preserves numeric types (Long for integers, Double for decimals).
   */
  public static final Builder<List<Number>> JSON_NUMBER_LIST = new Builder<List<Number>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Number> build(Object data) {
      if (data == null) return null;
      List<Object> list = (List<Object>) data;
      List<Number> result = new ArrayList<>(list.size());
      for (Object element : list) {
        if (element == null) {
          result.add(null);
        } else if (element instanceof Long) {
          result.add((Long) element);
        } else if (element instanceof Double) {
          result.add((Double) element);
        } else if (element instanceof Number) {
          result.add((Number) element);
        } else {
          // Parse from string (RESP2 fallback)
          String str = STRING.build(element);
          if (str.contains(".") || str.contains("e") || str.contains("E")) {
            result.add(Double.parseDouble(str));
          } else {
            result.add(Long.parseLong(str));
          }
        }
      }
      return result;
    }

    @Override
    public String toString() {
      return "List<Number>";
    }
  };

  public static final Builder<Object> JSON_ARRAY_OR_NUMBER_LIST = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      if (data == null) return null;
      if (data instanceof List) return JSON_NUMBER_LIST.build(data);
      return JSON_ARRAY.build(data);
    }
  };

  public static final Builder<List<JSONArray>> JSON_ARRAY_LIST = new Builder<List<JSONArray>>() {
    @Override
    public List<JSONArray> build(Object data) {
      if (data == null) {
        return null;
      }
      List<Object> list = (List<Object>) data;
      return list.stream().map(o -> JSON_ARRAY.build(o)).collect(Collectors.toList());
    }
  };

  /**
   * Builder that parses a List of numbers with type preservation (Long for integers, Double for decimals).
   * Supports both RESP3 (List<Object>) and RESP2 (JSON array as byte[]).
   * Returns null values for JSON null literals.
   */
  public static final Builder<List<Number>> NUMBER_LIST = new Builder<List<Number>>() {
    @Override
    public List<Number> build(Object data) {
      if (data == null) {
        return null;
      }
      // RESP3 -> expected List<Number>
      if (data instanceof List) return JSON_NUMBER_LIST.build(data);
      // RESP2 > Bulk string reply containing a JSON array of Number elements (nullable):
      if (data instanceof byte[]) {
        return parseJsonNumberArray(SafeEncoder.encode((byte[]) data));
      }
      throw new JedisException("Unsupported type: " + data.getClass());
    }

    @Override
    public String toString() {
      return "List<Number>";
    }
  };

  private static List<Number> parseJsonNumberArray(String json) {
    int len = json.length();
    int start = 0;
    // Find '[' skipping leading whitespace
    while (start < len && Character.isWhitespace(json.charAt(start))) {
      start++;
    }
    if (start >= len || json.charAt(start) != '[') {
      throw new JedisException("Expected a JSON array of numbers but got: " + json);
    }

    int end = len - 1;
    // Find ']' skipping trailing whitespace
    while (end > start && Character.isWhitespace(json.charAt(end))) {
      end--;
    }
    if (json.charAt(end) != ']') {
      throw new JedisException("Expected a JSON array of numbers but got: " + json);
    }

    List<Number> out = new ArrayList<>();
    int i = start + 1;
    boolean expectToken = true;
    while (i < end) {
      // Skip whitespace
      while (i < end && Character.isWhitespace(json.charAt(i))) {
        i++;
      }
      if (i >= end) break;

      // Skip comma if present (only after tokens, not at start)
      if (json.charAt(i) == ',') {
        if (expectToken) {
          throw new JedisException("Expected null or number but got empty element in: " + json);
        }
        i++;
        // Skip trailing whitespace after comma
        while (i < end && Character.isWhitespace(json.charAt(i))) {
          i++;
        }
        if (i >= end || json.charAt(i) == ',') {
          throw new JedisException("Expected null or number but got empty element in: " + json);
        }
        expectToken = true;
        continue;
      }

      // Parse token (null or number)
      int tokenStart = i;
      while (i < end && json.charAt(i) != ',') {
        i++;
      }

      // Trim leading whitespace
      while (tokenStart < i && Character.isWhitespace(json.charAt(tokenStart))) {
        tokenStart++;
      }

      // Trim trailing whitespace
      int tokenEnd = i;
      while (tokenEnd > tokenStart && Character.isWhitespace(json.charAt(tokenEnd - 1))) {
        tokenEnd--;
      }

      if (tokenStart >= tokenEnd) {
        throw new JedisException("Expected null or number but got empty element in: " + json);
      }

      String token = json.substring(tokenStart, tokenEnd);
      out.add(parseNullOrNumber(token));
      expectToken = false;
    }
    return out;
  }

  private static Number parseNullOrNumber(String str) {
    if ("null".equals(str)) {
      return null;
    }
    // Single pass to detect decimal or exponent
    boolean isDecimal = false;
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '.' || c == 'e' || c == 'E') {
        isDecimal = true;
        break;
      }
    }

    if (!isDecimal) {
      try {
        return Long.parseLong(str);
      } catch (NumberFormatException e) {
        // integer outside the long range; fall through to double
      }
    }
    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException e) {
      throw new JedisException("Expected a number but got: " + str, e);
    }
  }
  private JsonBuilderFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
