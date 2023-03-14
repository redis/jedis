package redis.clients.jedis.json.parser;

/**
 * The parser contract for all the <em>Redis Command</em> that perform JSON operations.
 *
 * @see redis.clients.jedis.json.RedisJsonCommands
 * @see redis.clients.jedis.json.RedisJsonPipelineCommands
 */
public interface RedisJsonParser {
  /**
   * Set an implementation of {@link JsonParser} for deserialization and serialization of JSON format string.
   *
   * @param jsonParser the implementation that knows how deserialize and serialize an JSON format string
   *
   * @see JsonParser
   *
   */
  void setJsonParser(JsonParser jsonParser);
}
