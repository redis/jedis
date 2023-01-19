package redis.clients.jedis.json;

import com.google.gson.Gson;

public class GsonJsonEncoderDecoder implements JsonEncoderDecoder {
  private final Gson gson;

  public GsonJsonEncoderDecoder() {
    this.gson = new Gson();
  }

  public GsonJsonEncoderDecoder(Gson gson) {
    this.gson = gson;
  }

  public <T> T fromJson(final String json, final Class<T> classOfT) {
    return gson.fromJson(json, classOfT);
  }

  public String toJson(final Object src) {
    return gson.toJson(src);
  }
}
