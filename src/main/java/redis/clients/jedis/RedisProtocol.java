package redis.clients.jedis;

public enum RedisProtocol {

  RESP2("2"),
  RESP3("3");

  private final String version;

  private RedisProtocol(String ver) {
    this.version = ver;
  }

  public String version() {
    return version;
  }
}
