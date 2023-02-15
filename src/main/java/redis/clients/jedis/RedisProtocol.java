package redis.clients.jedis;

public enum RedisProtocol {

  RESP2(2),
  RESP3(3);

  private final int version;

  private RedisProtocol(int version) {
    this.version = version;
  }

  public int version() {
    return version;
  }
}
