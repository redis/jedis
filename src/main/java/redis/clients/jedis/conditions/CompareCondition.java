package redis.clients.jedis.conditions;

import java.util.Arrays;
import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.annots.Experimental;

/**
 * A compare condition to be used with commands that support conditional value checks (e.g. SET with
 * IFEQ/IFNE/IFDEQ/IFDNE and DELEX). This abstraction lets callers express value-based or
 * digest-based comparisons.
 * <p>
 * Digest-based comparisons use a 64-bit XXH3 digest represented as a 16-character lower-case
 * hexadecimal string.
 * </p>
 */
@Experimental
public final class CompareCondition {

  /**
   * The kind of condition represented by this instance.
   */
  public enum Condition {
    /** current value must equal provided value */
    VALUE_EQUAL(Keyword.IFEQ),
    /** current value must not equal provided value */
    VALUE_NOT_EQUAL(Keyword.IFNE),
    /** current value's digest must equal provided digest */
    DIGEST_EQUAL(Keyword.IFDEQ),
    /** current value's digest must not equal provided digest */
    DIGEST_NOT_EQUAL(Keyword.IFDNE);

    private final Keyword keyword;

    Condition(Keyword keyword) {
      this.keyword = keyword;
    }

    /** The protocol keyword to emit for this condition. */
    public Keyword getKeyword() {
      return keyword;
    }
  }

  private final Condition condition;
  private final Object payload; // String or byte[]

  private CompareCondition(Condition condition, Object payload) {
    if (!(payload instanceof String) && !(payload instanceof byte[])) {
      throw new IllegalArgumentException("payload must be String or byte[]");
    }
    this.condition = condition;
    this.payload = payload;
  }

  // Factory methods: value-based
  public static CompareCondition valueEq(String value) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    return new CompareCondition(Condition.VALUE_EQUAL, value);
  }

  public static CompareCondition valueNe(String value) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    return new CompareCondition(Condition.VALUE_NOT_EQUAL, value);
  }

  public static CompareCondition valueEq(byte[] value) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    return new CompareCondition(Condition.VALUE_EQUAL, value);
  }

  public static CompareCondition valueNe(byte[] value) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    return new CompareCondition(Condition.VALUE_NOT_EQUAL, value);
  }

  // Factory methods: digest-based
  public static CompareCondition digestEq(String hex16) {
    if (hex16 == null) {
      throw new IllegalArgumentException("digest must not be null");
    }
    return new CompareCondition(Condition.DIGEST_EQUAL, hex16);
  }

  public static CompareCondition digestNe(String hex16) {
    if (hex16 == null) {
      throw new IllegalArgumentException("digest must not be null");
    }
    return new CompareCondition(Condition.DIGEST_NOT_EQUAL, hex16);
  }

  public static CompareCondition digestEq(byte[] digest) {
    if (digest == null) {
      throw new IllegalArgumentException("digest must not be null");
    }
    return new CompareCondition(Condition.DIGEST_EQUAL, digest);
  }

  public static CompareCondition digestNe(byte[] digest) {
    if (digest == null) {
      throw new IllegalArgumentException("digest must not be null");
    }
    return new CompareCondition(Condition.DIGEST_NOT_EQUAL, digest);
  }

  /**
   * Append this condition to the command arguments by emitting the appropriate keyword and
   * payload.
   */
  public void addTo(CommandArguments args) {
    args.add(condition.getKeyword()).add(payload);
  }

  /** The kind of this condition. */
  public Condition getCondition() {
    return condition;
  }

  /** The payload for this condition (String or byte[]). */
  public Object getPayload() {
    return payload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompareCondition that = (CompareCondition) o;
    if (condition != that.condition) return false;
    // Handle byte[] comparison
    if (payload instanceof byte[] && that.payload instanceof byte[]) {
      return Arrays.equals((byte[]) payload, (byte[]) that.payload);
    }
    return Objects.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(condition);
    if (payload instanceof byte[]) {
      result = 31 * result + Arrays.hashCode((byte[]) payload);
    } else {
      result = 31 * result + Objects.hashCode(payload);
    }
    return result;
  }

  @Override
  public String toString() {
    return "CompareCondition{" + "condition=" + condition + (payload != null ? ", payload="
        + payload : "") + '}';
  }

}

