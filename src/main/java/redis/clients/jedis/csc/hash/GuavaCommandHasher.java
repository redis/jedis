package redis.clients.jedis.csc.hash;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import redis.clients.jedis.CommandObject;

/**
 * An implementation of {@link CommandLongHasher} based on {@link HashFunction} from Google Guava library.
 */
public final class GuavaCommandHasher implements CommandLongHasher {

  public static final HashFunction DEFAULT_HASH_FUNCTION = com.google.common.hash.Hashing.fingerprint2011();

  private final HashFunction function;

  /**
   * It is advised to use a {@link HashFunction} capable of producing 64-bit hash.
   * @param function an implementation of hash function
   */
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
