package redis.clients.jedis;

public enum BitOP {
  AND, OR, XOR, NOT;

  public final byte[] raw;

  private BitOP() {
    this.raw = redis.clients.util.SafeEncoder.encode(name());
  }
}
