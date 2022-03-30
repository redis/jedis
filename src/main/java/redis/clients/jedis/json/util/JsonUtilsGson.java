package redis.clients.jedis.json.util;

import com.google.gson.Gson;

public class JsonUtilsGson implements JsonTools {

    private final Gson gson = new Gson();

    @Override
    public String toJson(Object src) {
        return gson.toJson(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}
