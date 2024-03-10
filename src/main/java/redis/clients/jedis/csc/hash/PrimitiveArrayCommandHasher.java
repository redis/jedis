package redis.clients.jedis.csc.hash;

import redis.clients.jedis.Builder;
import redis.clients.jedis.args.Rawable;

/**
 * It is possible to extend {@link PrimitiveArrayCommandHasher this abstract class} in order to implement
 * {@link CommandLongHasher} as {@link PrimitiveArrayCommandHasher#hashLongs(long[])} and
 * {@link PrimitiveArrayCommandHasher#hashBytes(byte[])} can be supported by almost all Java hashing libraries.
 */
public abstract class PrimitiveArrayCommandHasher extends AbstractCommandHasher {

  @Override
  protected final long hashRawable(Rawable raw) {
    return hashBytes(raw.getRaw());
  }

  @Override
  protected final long hashBuilder(Builder builder) {
    return hashInt(builder.hashCode());
  }

  protected abstract long hashBytes(byte[] bytes);

  protected long hashInt(int hashCode) {
    return hashCode;
  }
}
