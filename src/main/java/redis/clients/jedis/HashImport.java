package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import redis.clients.jedis.annots.Experimental;

/**
 * An immutable {@code HIMPORT} fieldset template: a locally generated, process-unique fieldset name
 * plus the ordered field names shared by the hashes to import. The type parameter {@code T} is the
 * field-name type, inferred from {@link #of(Object...)} &mdash; {@code HashImport<String>} for
 * String field names, {@code HashImport<byte[]>} for binary. Create one and reuse it across many
 * {@link redis.clients.jedis.commands.HashPipelineCommands#himportSet(String, HashImport, String...)
 * himportSet} calls.
 * <p>
 * The generated {@link #name()} (<code>"j:&lt;seq&gt;"</code>) is unique per instance and never
 * reused, so a discarded template's name cannot collide with a later one. Empty, {@code null}, and
 * duplicate field names are rejected eagerly; all other validation is server-authoritative.
 * @param <T> the field-name type, {@link String} or {@code byte[]}
 * @since 8.0
 */
@Experimental
public class HashImport<T> {

  private static final AtomicLong SEQUENCE = new AtomicLong();

  private final String name;
  private final List<T> fields;

  private HashImport(String name, List<T> fields) {
    this.name = name;
    this.fields = fields;
  }

  /**
   * Creates a template from the given field names. The field-name type is inferred: pass
   * {@link String}s for {@code HashImport<String>} or {@code byte[]}s for
   * {@code HashImport<byte[]>}.
   * @param fields the ordered field names; must be non-empty with no {@code null} or duplicate
   *          entries ({@code byte[]} duplicates are compared by content)
   * @param <T> the field-name type ({@link String} or {@code byte[]})
   * @return a new, uniquely named template
   * @since 8.0
   */
  @SafeVarargs
  public static <T> HashImport<T> of(T... fields) {
    if (fields == null || fields.length == 0) {
      throw new IllegalArgumentException("HashImport fields must be non-null and non-empty");
    }
    Set<Object> seen = new HashSet<>();
    List<T> copy = new ArrayList<>(fields.length);
    for (T field : fields) {
      if (field == null) {
        throw new IllegalArgumentException("HashImport fields must not contain null");
      }
      // byte[] has identity equals, so compare duplicates by content.
      Object dedupKey = (field instanceof byte[]) ? ByteBuffer.wrap((byte[]) field) : field;
      if (!seen.add(dedupKey)) {
        throw new IllegalArgumentException("HashImport fields must not contain duplicates");
      }
      copy.add(field);
    }
    return new HashImport<>(nextName(), Collections.unmodifiableList(copy));
  }

  /**
   * @return the wire {@code <fieldset>} token, {@code "j:<seq>"}
   * @since 8.0
   */
  public String name() {
    return name;
  }

  /**
   * @return the ordered field names (unmodifiable)
   * @since 8.0
   */
  public List<T> fields() {
    return fields;
  }

  /**
   * @return the number of fields, i.e. the required value count per
   *         {@link redis.clients.jedis.commands.HashPipelineCommands#himportSet(String, HashImport, String...)
   *         himportSet}
   * @since 8.0
   */
  public int size() {
    return fields.size();
  }

  private static String nextName() {
    return "j:" + SEQUENCE.incrementAndGet();
  }
}
