package redis.clients.jedis.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntPredicate;

import redis.clients.jedis.Protocol;

/**
 * One registered hash tag per slot for cluster tests, shared by the mock-server fixtures and the
 * scenario harness. The tag is found on first use and reused afterwards, so every test names a slot
 * the same way. Every key handed out is {@code {tag}:name}: the tag pins the slot, the caller's
 * name keeps keys of different tests (or workers) in the same slot apart. The bare tag is never
 * exposed, so two callers cannot collide on a slot by accident.
 */
public final class ClusterKeyRegistry {

  private static final ConcurrentMap<Integer, String> TAG_BY_SLOT = new ConcurrentHashMap<>();

  private ClusterKeyRegistry() {
  }

  /** A key named {@code name} in {@code slot}. */
  public static String keyForSlot(int slot, String name) {
    return "{" + tagForSlot(slot) + "}:" + name;
  }

  /** A key named {@code name} in the first slot of the inclusive range {@code [from, to]}. */
  public static String keyInRange(int from, int to, String name) {
    if (from > to) {
      throw new IllegalArgumentException("empty range: [" + from + ", " + to + "]");
    }
    return keyForSlot(from, name);
  }

  /** A key named {@code name} in the first slot satisfying {@code slotMatches}. */
  public static String keyMatching(IntPredicate slotMatches, String name) {
    for (int slot = 0; slot < Protocol.CLUSTER_HASHSLOTS; slot++) {
      if (slotMatches.test(slot)) {
        return keyForSlot(slot, name);
      }
    }
    throw new IllegalArgumentException("no slot satisfies the predicate");
  }

  /** The registered tag of {@code slot}, found on first use. */
  private static String tagForSlot(int slot) {
    if (slot < 0 || slot >= Protocol.CLUSTER_HASHSLOTS) {
      throw new IllegalArgumentException("slot out of range: " + slot);
    }
    return TAG_BY_SLOT.computeIfAbsent(slot, ClusterKeyRegistry::findTag);
  }

  /** The lowest decimal string whose CRC16 is {@code slot}; deterministic across runs. */
  private static String findTag(int slot) {
    for (int i = 0;; i++) {
      String tag = Integer.toString(i);
      if (JedisClusterCRC16.getSlot(tag) == slot) {
        return tag;
      }
    }
  }
}
