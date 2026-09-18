package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Ordering of the elements moved (and returned) by the {@code LMOVEM} and {@code BLMOVEM} commands.
 * @since 8.0
 */
public enum ListMoveOrder implements Rawable {

  /**
   * Move the elements one by one ({@code OBO}), as if each was individually popped from the source
   * and pushed to the destination.
   */
  OBO,

  /**
   * Move the elements as a single block ({@code BULK}), preserving the source list's relative order
   * at the destination.
   */
  BULK;

  private final byte[] raw;

  private ListMoveOrder() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
