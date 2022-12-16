package redis.clients.jedis;

import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.exceptions.JedisException;

final class JsonBuilderFactory {

  private JsonBuilderFactory() {
  }

  static final Builder<Object> JSON_OBJECT = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      if (data == null) {
        return null;
      }

      if (!(data instanceof byte[])) {
        return data;
      }

      String str = BuilderFactory.STRING.build(data);
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

  static final Builder<JSONArray> JSON_ARRAY = new Builder<JSONArray>() {
    @Override
    public JSONArray build(Object data) {
      if (data == null) {
        return null;
      }
      String str = BuilderFactory.STRING.build(data);
      try {
        return new JSONArray(str);
      } catch (JSONException ex) {
        // This is not necessary but we are doing this just to make is safer
        // for com.vaadin.external.google:android-json library
        throw new JedisException(ex);
      }
    }
  };

  static final Builder<List<JSONArray>> JSON_ARRAY_LIST = new Builder<List<JSONArray>>() {
    @Override
    public List<JSONArray> build(Object data) {
      if (data == null) {
        return null;
      }
      List<Object> list = (List<Object>) data;
      return list.stream().map(o -> JSON_ARRAY.build(o)).collect(Collectors.toList());
    }
  };

}
