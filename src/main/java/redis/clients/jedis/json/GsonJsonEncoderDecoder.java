package redis.clients.jedis.json;

import com.google.gson.Gson;

public class GsonJsonEncoderDecoder implements JsonEncoderDecoder {

  private static final Gson GSON = new Gson();

  public <T> T fromJson(final String json, final Class<T> classOfT) {
    return GSON.fromJson(json, classOfT);
  }

  public String toJson(final Object src) {
    return GSON.toJson(src);
  }
}
