package redis.clients.jedis.args;

public class LazyRaw implements Rawable {

  private byte[] raw = null;

  public LazyRaw() {
  }

  public void setRaw(byte[] raw) {
    this.raw = raw;
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
