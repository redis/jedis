package redis.clients.jedis.csc;

import net.openhft.hashing.LongHashFunction;
import redis.clients.jedis.csc.hash.AbstractSimpleCommandHasher;

public class OpenHftCommandHasher extends AbstractSimpleCommandHasher {

  public static final LongHashFunction DEFAULT_HASH_FUNCTION = LongHashFunction.xx3();

  private final LongHashFunction function;

  OpenHftCommandHasher() {
    this(DEFAULT_HASH_FUNCTION);
  }

  public OpenHftCommandHasher(LongHashFunction function) {
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
