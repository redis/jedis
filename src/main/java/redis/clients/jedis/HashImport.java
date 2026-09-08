package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.SafeEncoder;

/**
 * A reusable field-set template for {@code HIMPORT} (Hinted Hash Templates, Redis 8.10): declare
 * the ordered field names once, then create many hashes by sending only their values with
 * {@link redis.clients.jedis.commands.HashCommands#himportSet(String, HashImport, String...)
 * himportSet}.
 * <p>
 * A template may be reused across keys, calls, and connections; the server-side
 * {@code PREPARE}/{@code DISCARD} lifecycle is managed by the client and never exposed. Close the
 * template when done (it is {@link AutoCloseable}), preferably with try-with-resources; after
 * {@code close()} it must not be used again. A template dropped without {@code close()} leaves
 * stale server-side state behind until its connections are recycled.
 *
 * <pre>
 * {@code
 * try (HashImport fs = HashImport.of("name", "email", "age")) {
 *     jedis.himportSet("u:1", fs, "alice", "a@x.com", "25");
 *     jedis.himportSet("u:2", fs, "bob",   "b@x.com", "30");
 * }
 * }
 * </pre>
 * <p>
 * A template must declare at least one field, with no {@code null}, empty, or duplicate names. Each
 * {@code himportSet} supplies exactly one value per field, in field order; the keys it produces are
 * ordinary hashes.
 * @since 8.0
 */
@Experimental
public final class HashImport implements AutoCloseable {

  private static final AtomicLong SEQUENCE = new AtomicLong();

  private final String name;
  private final List<byte[]> fields;
  private volatile boolean discarded = false;

  /**
   * Connections this template has been prepared on, so {@link #close()} can mark its discard on
   * each. Weakly referenced: a long-lived template never pins a connection that was already
   * destroyed (e.g. by its pool), and a connection that dies before {@code close()} needs no
   * discard &mdash; its server-side state died with the socket. Preparation (register) and
   * {@code close()} (visit) may run on different threads; the registry is concurrent. A connection
   * re-prepared after a reconnect registers again; the duplicate discard mark is filtered at drain
   * time.
   */
  private final ConnectionRegistry connections = new ConnectionRegistry();

  private HashImport(String name, List<byte[]> fields) {
    this.name = name;
    this.fields = fields;
  }

  /**
   * Creates a template from the given {@link String} field names.
   * @param fields the ordered field names; must be non-empty with no {@code null}, empty, or
   *          duplicate entries
   * @return a new, uniquely named template
   * @since 8.0
   */
  public static HashImport of(String... fields) {
    if (fields == null || fields.length == 0) {
      throw new IllegalArgumentException("HashImport fields must be non-null and non-empty");
    }
    List<byte[]> encoded = new ArrayList<>(fields.length);
    for (String field : fields) {
      if (field == null) {
        throw new IllegalArgumentException("HashImport fields must not contain null");
      }
      encoded.add(SafeEncoder.encode(field));
    }
    return build(encoded);
  }

  /**
   * Creates a template from the given binary field names.
   * @param fields the ordered field names; must be non-empty with no {@code null}, empty, or
   *          duplicate entries (duplicates are compared by content).
   * @return a new, uniquely named template
   * @since 8.0
   */
  public static HashImport of(byte[]... fields) {
    if (fields == null || fields.length == 0) {
      throw new IllegalArgumentException("HashImport fields must be non-null and non-empty");
    }
    List<byte[]> copy = new ArrayList<>(fields.length);
    for (byte[] field : fields) {
      if (field == null) {
        throw new IllegalArgumentException("HashImport fields must not contain null");
      }
      copy.add(field.clone()); // clone so later caller mutation can't alter the template
    }
    return build(copy);
  }

  private static HashImport build(List<byte[]> fields) {
    Set<ByteBuffer> seen = new HashSet<>();
    for (byte[] field : fields) {
      if (field.length == 0) {
        throw new IllegalArgumentException("HashImport fields must not contain empty names");
      }
      if (!seen.add(ByteBuffer.wrap(field))) {
        throw new IllegalArgumentException("HashImport fields must not contain duplicates");
      }
    }
    return new HashImport(nextName(), Collections.unmodifiableList(fields));
  }

  /**
   * @return {@code true} once {@link #close()} has been called
   * @since 8.0
   */
  public boolean isDiscarded() {
    return discarded;
  }

  /**
   * Discards this template; it must not be used again afterwards. Server-side cleanup is deferred
   * and handled by the client. Idempotent.
   * @since 8.0
   */
  @Override
  public void close() {
    if (discarded) {
      return;
    }
    discarded = true;
    connections.forEachLive(connection -> connection.himportState().markForDiscard(name));
  }

  /**
   * Records that this template has been prepared on {@code connection}, so {@link #close()} can
   * mark its discard there. Called during prepare-before-use.
   */
  void registerConnection(Connection connection) {
    connections.register(connection);
    if (discarded) {
      connections.forEachLive(c -> c.himportState().markForDiscard(name));
    }
  }

  /** The wire {@code <fieldset>} token, {@code "j:<seq>"}. */
  String name() {
    return name;
  }

  /** The ordered field names as encoded tokens (unmodifiable; used to build {@code PREPARE}). */
  List<byte[]> fields() {
    return fields;
  }

  /** The number of fields, i.e. the required value count per {@code himportSet}. */
  int size() {
    return fields.size();
  }

  private static String nextName() {
    return "j:" + SEQUENCE.incrementAndGet();
  }
}
