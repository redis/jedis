package redis.clients.jedis.args;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.util.SafeEncoder.encode;

public final class RawableFactory {

  public static Rawable from(int i) {
    return from(toByteArray(i));
  }

  public static Rawable from(double d) {
    return from(toByteArray(d));
  }

  public static Rawable from(byte[] binary) {
    return new Raw(binary);
  }

  public static Rawable from(String string) {
    return new RawString(string);
  }

  public static class Raw implements Rawable {

    private final byte[] raw;

    public Raw(byte[] raw) {
      this.raw = raw;
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static class RawString extends Raw {

    public RawString(String str) {
      super(encode(str));
    }
  }

  private RawableFactory() {
    throw new InstantiationError();
  }
}
