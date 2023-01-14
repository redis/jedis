package redis.clients.jedis.json;

public interface JsonEncoderDecoder {

  <T> T fromJson(final String json, final Class<T> classOfT);

  String toJson(final Object src);
}