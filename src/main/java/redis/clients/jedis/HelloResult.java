package redis.clients.jedis;

import java.util.Map;

class HelloResult {
  final Map<String, Object> helloResponse;

  HelloResult(Map<String, Object> helloResponse) {
    this.helloResponse = helloResponse;
  }

  /**
   * @return the protocol version the server actually accepted
   */
  public RedisProtocol getProtocol() {
    Long proto = (Long) helloResponse.get("proto");
    return RedisProtocol.from(proto);
  }

  public String getServer() {
    return (String) helloResponse.get("server");
  }

  public String getVersion() {
    return (String) helloResponse.get("version");
  }
}
