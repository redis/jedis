package redis.clients.jedis;

import com.google.gson.Gson;

class GsonJson {

  private static final Gson GSON = new Gson();

  static <T> T fromJson(final String json, final Class<T> classOfT) {
    return GSON.fromJson(json, classOfT);
  }

  static String toJson(final Object src) {
    return GSON.toJson(src);
  }
}
