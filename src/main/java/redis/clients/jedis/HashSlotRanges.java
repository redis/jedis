package redis.clients.jedis;

import java.util.function.IntConsumer;

/**
 * An immutable set of hash slots expressed as comma-separated slots and/or inclusive
 * {@code from-to} ranges, e.g. {@code "123,456,789-1000"} — the slots-or-ranges wire format of
 * cluster maintenance pushes.
 */
final class HashSlotRanges {

  /** Inclusive bounds, flattened as {@code [from0, to0, from1, to1, ...]} in wire order. */
  private final int[] bounds;
  private final String source;

  private HashSlotRanges(int[] bounds, String source) {
    this.bounds = bounds;
    this.source = source;
  }

  /**
   * Parses the slots-or-ranges wire format.
   * @throws IllegalArgumentException on an empty string, a non-numeric token, an inverted range, or
   *           a slot outside {@code [0, 16384)}
   */
  static HashSlotRanges parse(String s) {
    if (s == null || s.isEmpty()) {
      throw new IllegalArgumentException("Empty slot ranges");
    }
    String[] tokens = s.split(",");
    int[] bounds = new int[tokens.length * 2];
    for (int t = 0; t < tokens.length; t++) {
      String token = tokens[t];
      int dash = token.indexOf('-');
      int from, to;
      try {
        if (dash < 0) {
          from = to = Integer.parseInt(token);
        } else {
          from = Integer.parseInt(token.substring(0, dash));
          to = Integer.parseInt(token.substring(dash + 1));
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Unparseable slot range token: " + token, e);
      }
      if (from < 0 || to < from || to >= Protocol.CLUSTER_HASHSLOTS) {
        throw new IllegalArgumentException("Slot range out of bounds: " + token);
      }
      bounds[t * 2] = from;
      bounds[t * 2 + 1] = to;
    }
    return new HashSlotRanges(bounds, s);
  }

  boolean contains(int slot) {
    for (int i = 0; i < bounds.length; i += 2) {
      if (slot >= bounds[i] && slot <= bounds[i + 1]) {
        return true;
      }
    }
    return false;
  }

  int slotCount() {
    int count = 0;
    for (int i = 0; i < bounds.length; i += 2) {
      count += bounds[i + 1] - bounds[i] + 1;
    }
    return count;
  }

  void forEachSlot(IntConsumer action) {
    for (int i = 0; i < bounds.length; i += 2) {
      for (int slot = bounds[i]; slot <= bounds[i + 1]; slot++) {
        action.accept(slot);
      }
    }
  }

  @Override
  public String toString() {
    return source;
  }
}
