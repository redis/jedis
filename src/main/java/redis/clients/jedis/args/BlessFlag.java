package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Key protection flags used by the {@code BLESS} command family ({@code BLESS SET},
 * {@code BLESS CLEAR}, {@code BLESS SCAN}).
 * <p>
 * Blessing is modelled by the server as a set of independent flags rather than an ordered level.
 * Currently the server defines a single flag, {@link #NO_EVICT}.
 * @since 8.1
 */
public enum BlessFlag implements Rawable {

  /**
   * Protects the key from being chosen as a {@code maxmemory} eviction victim under any eviction
   * policy. Wire token: {@code NO-EVICT}.
   */
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
