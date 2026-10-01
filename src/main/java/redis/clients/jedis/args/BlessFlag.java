package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Protection flags accepted by the Redis {@code BLESS} command family ({@code BLESS SET},
 * {@code BLESS CLEAR}, {@code BLESS GET}, {@code BLESS SCAN}). The server models blessing as an
 * open set of independent flags rather than an ordered level; today exactly one flag exists,
 * {@link #NO_EVICT}. The flag token is passed to the server verbatim and the server rejects unknown
 * tokens, so this enum is not validated client-side beyond type-safety.
 * @since 8.1
 */
public enum BlessFlag implements Rawable {

  /** Protects the key from {@code maxmemory} eviction under any eviction policy. */
  NO_EVICT("NO-EVICT");

  private final byte[] raw;

  BlessFlag(String token) {
    raw = SafeEncoder.encode(token);
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
