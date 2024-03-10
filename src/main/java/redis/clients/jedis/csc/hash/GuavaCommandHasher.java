package redis.clients.jedis.csc.hash;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import redis.clients.jedis.CommandObject;

public class GuavaCommandHasher implements CommandLongHasher {

  public static final HashFunction DEFAULT_HASH_FUNCTION = com.google.common.hash.Hashing.fingerprint2011();

  private final HashFunction function;

  public GuavaCommandHasher(HashFunction function) {
    this.function = function;
  }

  @Override
  public long hash(CommandObject command) {
    Hasher hasher = function.newHasher();
    command.getArguments().forEach(raw -> hasher.putBytes(raw.getRaw()));
    hasher.putInt(command.getBuilder().hashCode());
    return hasher.hash().asLong();
  }
}
