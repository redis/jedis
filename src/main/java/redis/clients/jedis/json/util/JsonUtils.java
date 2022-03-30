package redis.clients.jedis.json.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisException;

public final class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    private static volatile JsonTools JSON_TOOLS;

    static {
        JSON_TOOLS = loadGsonTools();
    }

    private JsonUtils() {
    }

    public static JsonTools getJsonTools() {
        return JSON_TOOLS;
    }

    public static void setJsonTools(JsonTools jsonTools) {
        JSON_TOOLS = jsonTools;
    }

    public static String toJson(Object src) {
        if (JSON_TOOLS == null) {
            throw new JedisException("GSON not found on classpath, please add com.google.code.gson:gson to your classpath or inject your own impl. of JsonTools");
        }
        return JSON_TOOLS.toJson(src);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        if (JSON_TOOLS == null) {
            throw new JedisException("GSON not found on classpath, please add com.google.code.gson:gson to your classpath or inject your own impl. of JsonTools");
        }
        return JSON_TOOLS.fromJson(json, classOfT);
    }

    private static JsonTools loadGsonTools() {
        try {
            Class jsonUtilsGson =
                    Thread.currentThread().getContextClassLoader().loadClass("redis.clients.jedis.json.util.JsonUtilsGson");
            return (JsonTools) jsonUtilsGson.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.debug("Could not load optional GSON: ", e);
            return null;
        }
    }

}
