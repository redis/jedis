package redis.clients.jedis.args;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.util.SafeEncoder.encode;

/**
 * Args transform Factory.
 */
public final class RawableFactory {

  /**
   * Transform an int value to raw.
   * @param i value
   * @return raw
   */
  public static Rawable from(int i) {
    return from(toByteArray(i));
  }

  /**
   * Transform a double value to raw.
   * @param d value
   * @return raw
   */
  public static Rawable from(double d) {
    return from(toByteArray(d));
  }

  /**
   * Transform a byte array value to raw.
   * @param binary value
   * @return raw
   */
  public static Rawable from(byte[] binary) {
    return new Raw(binary);
  }

  /**
   * Transform a string value to raw.
   * @param string value
   * @return raw
   */
  public static Rawable from(String string) {
    return new RawString(string);
  }

  /**
   * Default implement of Rawable
   * */
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

  /**
   * Init raw with string.
   */
  public static class RawString extends Raw {

    public RawString(String str) {
      super(encode(str));
    }
  }

  private RawableFactory() {
    throw new InstantiationError();
  }
}
