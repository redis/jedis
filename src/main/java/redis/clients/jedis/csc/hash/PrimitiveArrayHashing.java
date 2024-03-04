package redis.clients.jedis.csc.hash;

import redis.clients.jedis.Builder;
import redis.clients.jedis.args.Rawable;

public abstract class PrimitiveArrayHashing extends AbstractCommandHashing {

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
