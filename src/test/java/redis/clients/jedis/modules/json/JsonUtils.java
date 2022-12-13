package redis.clients.jedis.modules.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;

public class JsonUtils {

  public static final ObjectMapper JACKSON = createJackson();

  private static ObjectMapper createJackson() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    mapper.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature());
    return mapper;
  }

  public static String writeJson(Object obj) throws JsonProcessingException {
    return JACKSON.writer().writeValueAsString(obj);
  }

  public static <T> T readJson(String str, Class<T> cls) throws IOException {
    return JACKSON.reader().readValue(str, cls);
  }

  public static Object readJson(String str) throws JsonProcessingException {
    return JACKSON.reader().readTree(str);
  }

  public static Object readJson(Object obj) throws JsonProcessingException {
    return JACKSON.reader().readTree(JACKSON.writer().writeValueAsString(obj));
  }

  public static Object emptyJson() {
    return JACKSON.createObjectNode();
  }
}
