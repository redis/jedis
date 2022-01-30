package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;
import java.util.Locale;

public enum SetExpireOption implements Rawable {

  NX, XX, GT, LT;

  private final byte[] raw;

  private SetExpireOption() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
