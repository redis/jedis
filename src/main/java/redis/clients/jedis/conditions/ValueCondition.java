package redis.clients.jedis.conditions;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.annots.Experimental;

/**
 * Experimental condition object for optimistic concurrency with SET/DELEX.
 * Encapsulates either a value-based or digest-based condition with equality or inequality.
 */
@Experimental
public final class ValueCondition {

  public enum Kind { VALUE, DIGEST }
  public enum Mode { EQ, NE }

  private final Kind kind;
  private final Mode mode;
  private final Object payload; // String or byte[]

  private ValueCondition(Kind kind, Mode mode, Object payload) {
    if (!(payload instanceof String) && !(payload instanceof byte[])) {
      throw new IllegalArgumentException("payload must be String or byte[]");
    }
    this.kind = kind;
    this.mode = mode;
    this.payload = payload;
  }

  // Factory methods: value-based
  public static ValueCondition valueEq(String value) { return new ValueCondition(Kind.VALUE, Mode.EQ, value); }
  public static ValueCondition valueNe(String value) { return new ValueCondition(Kind.VALUE, Mode.NE, value); }
  public static ValueCondition valueEq(byte[] value) { return new ValueCondition(Kind.VALUE, Mode.EQ, value); }
  public static ValueCondition valueNe(byte[] value) { return new ValueCondition(Kind.VALUE, Mode.NE, value); }

  // Factory methods: digest-based
  public static ValueCondition digestEq(String hex16) { return new ValueCondition(Kind.DIGEST, Mode.EQ, hex16); }
  public static ValueCondition digestNe(String hex16) { return new ValueCondition(Kind.DIGEST, Mode.NE, hex16); }
  public static ValueCondition digestEq(byte[] digest) { return new ValueCondition(Kind.DIGEST, Mode.EQ, digest); }
  public static ValueCondition digestNe(byte[] digest) { return new ValueCondition(Kind.DIGEST, Mode.NE, digest); }

  /**
   * Append this condition to the command arguments by emitting the appropriate keyword and payload.
   */
  public void addTo(CommandArguments args) {
    if (kind == Kind.VALUE) {
      args.add(mode == Mode.EQ ? Keyword.IFEQ : Keyword.IFNE).add(payload);
    } else { // DIGEST
      args.add(mode == Mode.EQ ? Keyword.IFDEQ : Keyword.IFDNE).add(payload);
    }
  }
}

