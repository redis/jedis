package redis.clients.jedis.csc.hash;

import redis.clients.jedis.Builder;
import redis.clients.jedis.args.Rawable;

/**
 * It is possible to extend {@link AbstractSimpleCommandHasher this abstract class} in order to implement
 * {@link CommandLongHasher} as {@link AbstractSimpleCommandHasher#hashLongs(long[])} and
 * {@link AbstractSimpleCommandHasher#hashBytes(byte[])} are supported by almost all Java hashing libraries.
 */
public abstract class AbstractSimpleCommandHasher extends AbstractCommandHasher {

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
