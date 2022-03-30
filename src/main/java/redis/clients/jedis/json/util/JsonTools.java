package redis.clients.jedis.json.util;

public interface JsonTools {
    String toJson(Object src);

    <T> T fromJson(String json, Class<T> classOfT);
}
