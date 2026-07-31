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
 * A managed {@code HIMPORT} fieldset template (Hinted Hash Templates, Redis 8.10): a locally
 * generated, process-unique fieldset name plus the ordered field names shared by the hashes to
 * import.
 * <p>
 * A fieldset is per-physical-connection server state. Rather than expose the raw
 * {@code PREPARE}/{@code DISCARD} lifecycle, the client manages it transparently: each
 * {@link redis.clients.jedis.commands.HashCommands#himportSet(String, HashImport, String...)
 * himportSet} lazily prepares the template on whichever pooled connection it lands on, and cleanup
 * happens at the only thread-safe moment &mdash; when a connection is next borrowed &mdash; driven
 * by this template's {@linkplain #isDiscarded() discarded} flag (or by garbage collection if the
 * caller drops it without closing). Use it with try-with-resources:
 * 
 * <pre>
 * {@code
 * try (HashImport fs = HashImport.of("name", "email", "age")) {
 *     jedis.himportSet("u:1", fs, "alice", "a@x.com", "25");
 *     jedis.himportSet("u:2", fs, "bob",   "b@x.com", "30");
 * }   // close() -> the fieldset is discarded from each connection as it is next borrowed
 * }
 * </pre>
 * <p>
 * The generated {@link #name()} (<code>"j:&lt;seq&gt;"</code>) is unique per instance and never
 * reused, so a discarded template's name cannot collide with a later one. Empty, {@code null}, and
 * duplicate field names are rejected eagerly; all other validation is server-authoritative.
 * @since 8.0
 */
@Experimental
public final class HashImport implements AutoCloseable {

  private static final AtomicLong SEQUENCE = new AtomicLong();

  private final String name;
  private final List<byte[]> fields;
  private volatile boolean discarded = false;

  private HashImport(String name, List<byte[]> fields) {
    this.name = name;
    this.fields = fields;
  }

  /**
   * Creates a template from the given {@link String} field names.
   * @param fields the ordered field names; must be non-empty with no {@code null} or duplicate
   *          entries
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
   * @param fields the ordered field names; must be non-empty with no {@code null} or duplicate
   *          entries (duplicates are compared by content). Each array is defensively copied.
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
   * Discards this template. The server-side fieldset is not touched synchronously; instead every
   * connection that prepared it drops it (issuing {@code HIMPORT DISCARD}) the next time it is
   * borrowed. After this call the template must not be used again.
   * @since 8.0
   */
  @Override
  public void close() {
    discarded = true;
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
