package redis.clients.jedis.args;

import java.util.Arrays;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Protection flag a key can carry via the {@code BLESS} command family. Flags are independent of
 * each other and form an open set: {@link #NO_EVICT} is the only one the server defines today, and
 * {@link #of(String)} forwards any other token verbatim so newer server flags can be used without a
 * client upgrade. Unknown tokens are rejected by the server with a syntax error.
 * @since 8.1
 */
public final class BlessFlag implements Rawable {

  /**
   * The key is never chosen as a {@code maxmemory} eviction victim, under any eviction policy.
   */
  public static final BlessFlag NO_EVICT = new BlessFlag("NO-EVICT");

  private final byte[] raw;

  private BlessFlag(String token) {
    this.raw = SafeEncoder.encode(token);
  }

  /**
   * A flag from its wire token, for example {@code "NO-EVICT"}. No client-side validation is done.
   * @param token the flag token as accepted by the server
   * @return the flag
   */
  public static BlessFlag of(String token) {
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Flag token must be non-null and non-empty");
    }
    return new BlessFlag(token);
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return Arrays.equals(raw, ((BlessFlag) o).raw);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(raw);
  }

  @Override
  public String toString() {
    return SafeEncoder.encode(raw);
  }
}
