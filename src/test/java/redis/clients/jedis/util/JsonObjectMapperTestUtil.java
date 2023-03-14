package redis.clients.jedis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.json.JsonObjectMapper;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class JsonObjectMapperTestUtil {
  public static CustomJacksonObjectMapper getCustomJacksonObjectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new CustomJacksonObjectMapper(om);
  }

  public static CustomGsonObjectMapper getCustomGsonObjectMapper() {
    final class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
      DateTimeFormatter format = DateTimeFormatter.ISO_INSTANT;

      @Override
      public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (!primitive.isJsonNull()) {
          String asString = primitive.getAsString();
          TemporalAccessor temporalAccessor = format.parse(asString);
          return Instant.from(temporalAccessor);
        }
        return null;
      }

      @Override
      public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(format.format(src));
      }
    }
    return new CustomGsonObjectMapper(
        new GsonBuilder().registerTypeAdapter(Instant.class, new InstantAdapter()).create());
  }

  public static class CustomJacksonObjectMapper implements JsonObjectMapper {
    private final ObjectMapper om;

    CustomJacksonObjectMapper(ObjectMapper om) {
      this.om = om;
    }

    @Override
    public <T> T fromJson(String value, Class<T> valueType) {
      try {
        return om.readValue(value, valueType);
      } catch (JsonProcessingException e) {
        throw new JedisException(e);
      }
    }

    @Override
    public String toJson(Object value) {
      try {
        return om.writeValueAsString(value);
      } catch (JsonProcessingException e) {
        throw new JedisException(e);
      }
    }
  }

  public static class CustomGsonObjectMapper implements JsonObjectMapper {
    private final Gson gson;

    public CustomGsonObjectMapper(Gson gson) {
      this.gson = gson;
    }

    @Override
    public <T> T fromJson(String value, Class<T> valueType) {
      return gson.fromJson(value, valueType);
    }

    @Override
    public String toJson(Object value) {
      return gson.toJson(value);
    }
  }
}
