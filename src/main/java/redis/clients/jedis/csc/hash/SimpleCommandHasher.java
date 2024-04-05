package redis.clients.jedis.csc.hash;

import java.util.Arrays;

/**
 * This {@link CommandLongHasher} implementation is simply based on {@link Arrays#hashCode(long[])}
 * and {@link Arrays#hashCode(byte[])}. These methods actually produce 32-bit hash codes. It is
 * advised to use proper 64-bit hash codes in production.
 */
public final class SimpleCommandHasher extends AbstractSimpleCommandHasher {

  public static final SimpleCommandHasher INSTANCE = new SimpleCommandHasher();

  public SimpleCommandHasher() { }

  @Override
  protected long hashLongs(long[] longs) {
    return Arrays.hashCode(longs);
  }

  @Override
  protected long hashBytes(byte[] bytes) {
    return Arrays.hashCode(bytes);
  }
}
