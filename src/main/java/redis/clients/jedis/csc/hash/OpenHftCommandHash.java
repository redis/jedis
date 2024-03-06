package redis.clients.jedis.csc.hash;

import net.openhft.hashing.LongHashFunction;

public class OpenHftCommandHash extends PrimitiveArrayHash implements CommandLongHash {

  public static final LongHashFunction DEFAULT_HASH_FUNCTION = LongHashFunction.xx3();

  private final LongHashFunction function;

  public OpenHftCommandHash(LongHashFunction function) {
    this.function = function;
  }

  @Override
  protected long hashLongs(long[] longs) {
    return function.hashLongs(longs);
  }

  @Override
  protected long hashBytes(byte[] bytes) {
    return function.hashBytes(bytes);
  }

  @Override
  protected long hashInt(int hashCode) {
    return function.hashInt(hashCode);
  }
}
