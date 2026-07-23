package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import redis.clients.jedis.annots.Experimental;

/**
 * An immutable {@code HIMPORT} fieldset template: a locally generated, process-unique fieldset name
 * plus the ordered field names shared by the hashes to import. Create one with
 * {@link #of(String...)} (or {@link #of(byte[]...)}) and reuse it across many
 * {@link redis.clients.jedis.commands.HashPipelineCommands#himportSet(String, HashImport, String...)
 * himportSet} calls.
 * <p>
 * The generated {@link #name()} (<code>"j:&lt;seq&gt;"</code>) is unique per instance and never
 * reused, so a discarded template's name cannot collide with a later one. Empty, {@code null}, and
 * duplicate field names are rejected eagerly; all other validation is server-authoritative.
 * @since 8.0
 */
@Experimental
public class HashImport {

  private static final AtomicLong SEQUENCE = new AtomicLong();

  private final String name;
  private final String[] fields; // non-null iff built from String field names
  private final byte[][] binaryFields; // non-null iff built from byte[] field names

  private HashImport(String name, String[] fields, byte[][] binaryFields) {
    this.name = name;
    this.fields = fields;
    this.binaryFields = binaryFields;
  }

  /**
   * Creates a template from String field names.
   * @param fields the ordered field names; must be non-empty with no {@code null} or duplicate
   *          entries
   * @return a new, uniquely named template
   * @since 8.0
   */
  public static HashImport of(String... fields) {
    if (fields == null || fields.length == 0) {
      throw new IllegalArgumentException("HashImport fields must be non-null and non-empty");
    }
    Set<String> seen = new HashSet<>();
    for (String field : fields) {
      if (field == null) {
        throw new IllegalArgumentException("HashImport fields must not contain null");
      }
      if (!seen.add(field)) {
        throw new IllegalArgumentException(
            "HashImport fields must not contain duplicates: " + field);
      }
    }
    return new HashImport(nextName(), Arrays.copyOf(fields, fields.length), null);
  }

  /**
   * Creates a template from binary field names.
   * @param fields the ordered field names; must be non-empty with no {@code null} or duplicate
   *          entries
   * @return a new, uniquely named template
   * @since 8.0
   */
  public static HashImport of(byte[]... fields) {
    if (fields == null || fields.length == 0) {
      throw new IllegalArgumentException("HashImport fields must be non-null and non-empty");
    }
    Set<ByteBuffer> seen = new HashSet<>();
    byte[][] copy = new byte[fields.length][];
    for (int i = 0; i < fields.length; i++) {
      if (fields[i] == null) {
        throw new IllegalArgumentException("HashImport fields must not contain null");
      }
      if (!seen.add(ByteBuffer.wrap(fields[i]))) {
        throw new IllegalArgumentException("HashImport fields must not contain duplicates");
      }
      copy[i] = Arrays.copyOf(fields[i], fields[i].length);
    }
    return new HashImport(nextName(), null, copy);
  }

  /**
   * @return the wire {@code <fieldset>} token, {@code "j:<seq>"}
   * @since 8.0
   */
  public String name() {
    return name;
  }

  /**
   * @return the String field names, or {@code null} if this template was built from {@code byte[]}
   * @since 8.0
   */
  public String[] fields() {
    return fields;
  }

  /**
   * @return the binary field names, or {@code null} if this template was built from String
   * @since 8.0
   */
  public byte[][] binaryFields() {
    return binaryFields;
  }

  /**
   * @return the number of fields, i.e. the required value count per
   *         {@link redis.clients.jedis.commands.HashPipelineCommands#himportSet(String, HashImport, String...)
   *         himportSet}
   * @since 8.0
   */
  public int size() {
    return fields != null ? fields.length : binaryFields.length;
  }

  /** @return {@code true} if this template carries binary field names. */
  boolean isBinary() {
    return binaryFields != null;
  }

  private static String nextName() {
    return "j:" + SEQUENCE.incrementAndGet();
  }
}
