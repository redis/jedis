package redis.clients.jedis;

public enum BitOP {
  AND, OR, XOR, NOT;

  public final byte[] raw;

  private BitOP() {
    this.raw = redis.clients.jedis.util.SafeEncoder.encode(name());
  }
}
