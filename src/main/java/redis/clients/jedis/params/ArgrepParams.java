package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Arguments for the {@code ARGREP} command. Every instance carries the mandatory {@code start end}
 * bounds of the search range; instances are obtained through one of the static factories
 * ({@link #unbounded()}, {@link #range(long, long)}, {@link #from(long)}, {@link #to(long)}) and
 * may be flipped with {@link #reversed()} to traverse in descending index order. Predicates,
 * combinator ({@code AND}/{@code OR}), {@code LIMIT} and {@code NOCASE} are appended via fluent
 * setters in the order required by the wire protocol. The {@code WITHVALUES} flag is not exposed
 * here: use {@code argrepWithValues} to request index/value pairs.
 */
public class ArgrepParams implements IParams {

  private static final byte[] MIN = new byte[] { '-' };
  private static final byte[] MAX = new byte[] { '+' };

  private enum PredicateType {
    EXACT(Keyword.EXACT), MATCH(Keyword.MATCH), GLOB(Keyword.GLOB), RE(Keyword.RE);

    final Keyword keyword;

    PredicateType(Keyword keyword) {
      this.keyword = keyword;
    }
  }

  private static final class Predicate {
    final PredicateType type;
    final byte[] value;

    Predicate(PredicateType type, byte[] value) {
      this.type = type;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Predicate that = (Predicate) o;
      return type == that.type && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hashCode(type) + Arrays.hashCode(value);
    }
  }

  private byte[] startRaw;
  private byte[] endRaw;
  private final List<Predicate> predicates = new ArrayList<>();
  private Keyword combinator;
  private Long limit;
  private boolean nocase;

  private ArgrepParams() {
  }

  /**
   * Search the entire array (wire bounds {@code - +}).
   * @return a new {@link ArgrepParams} with start={@code -}, end={@code +}
   */
  public static ArgrepParams unbounded() {
    ArgrepParams p = new ArgrepParams();
    p.startRaw = MIN;
    p.endRaw = MAX;
    return p;
  }

  /**
   * Search the inclusive index range {@code [start, end]}.
   * @param start the zero-based start index (inclusive)
   * @param end the zero-based end index (inclusive)
   * @return a new {@link ArgrepParams} with concrete numeric bounds
   */
  public static ArgrepParams range(long start, long end) {
    ArgrepParams p = new ArgrepParams();
    p.startRaw = Protocol.toByteArray(start);
    p.endRaw = Protocol.toByteArray(end);
    return p;
  }

  /**
   * Search from a concrete start index to the end of the array (wire end {@code +}).
   * @param start the zero-based start index (inclusive)
   * @return a new {@link ArgrepParams} with start=numeric, end={@code +}
   */
  public static ArgrepParams from(long start) {
    ArgrepParams p = new ArgrepParams();
    p.startRaw = Protocol.toByteArray(start);
    p.endRaw = MAX;
    return p;
  }

  /**
   * Search from the start of the array to a concrete end index (wire start {@code -}).
   * @param end the zero-based end index (inclusive)
   * @return a new {@link ArgrepParams} with start={@code -}, end=numeric
   */
  public static ArgrepParams to(long end) {
    ArgrepParams p = new ArgrepParams();
    p.startRaw = MIN;
    p.endRaw = Protocol.toByteArray(end);
    return p;
  }

  /**
   * Swap the start and end bounds. Because the server interprets {@code start > end} as a
   * descending traversal, this returns matches in reverse index order.
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams reversed() {
    byte[] tmp = this.startRaw;
    this.startRaw = this.endRaw;
    this.endRaw = tmp;
    return this;
  }

  /**
   * Add an {@code EXACT} predicate: matches elements whose value equals the given string exactly.
   * @param value the literal value to match against
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams exact(String value) {
    predicates.add(new Predicate(PredicateType.EXACT, encode(value)));
    return this;
  }

  /**
   * Add an {@code EXACT} predicate: matches elements whose value equals the given bytes exactly.
   * @param value the literal value to match against
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams exact(byte[] value) {
    predicates.add(new Predicate(PredicateType.EXACT, value));
    return this;
  }

  /**
   * Add a {@code MATCH} predicate: matches elements that contain the given string as a substring.
   * @param value the substring to search for
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams match(String value) {
    predicates.add(new Predicate(PredicateType.MATCH, encode(value)));
    return this;
  }

  /**
   * Add a {@code MATCH} predicate: matches elements that contain the given bytes as a substring.
   * @param value the substring to search for
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams match(byte[] value) {
    predicates.add(new Predicate(PredicateType.MATCH, value));
    return this;
  }

  /**
   * Add a {@code GLOB} predicate: matches elements against a glob-style pattern (supports
   * {@code *}, {@code ?} and {@code [...]}).
   * @param pattern the glob pattern
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams glob(String pattern) {
    predicates.add(new Predicate(PredicateType.GLOB, encode(pattern)));
    return this;
  }

  /**
   * Add a {@code GLOB} predicate: matches elements against a glob-style pattern (supports
   * {@code *}, {@code ?} and {@code [...]}).
   * @param pattern the glob pattern as raw bytes
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams glob(byte[] pattern) {
    predicates.add(new Predicate(PredicateType.GLOB, pattern));
    return this;
  }

  /**
   * Add a {@code RE} predicate: matches elements against a regular expression.
   * @param pattern the regular expression
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams re(String pattern) {
    predicates.add(new Predicate(PredicateType.RE, encode(pattern)));
    return this;
  }

  /**
   * Add a {@code RE} predicate: matches elements against a regular expression.
   * @param pattern the regular expression as raw bytes
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams re(byte[] pattern) {
    predicates.add(new Predicate(PredicateType.RE, pattern));
    return this;
  }

  /**
   * Combine multiple predicates with logical AND.
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams and() {
    this.combinator = Keyword.AND;
    return this;
  }

  /**
   * Combine multiple predicates with logical OR (the default if neither {@link #and()} nor
   * {@link #or()} is set).
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams or() {
    this.combinator = Keyword.OR;
    return this;
  }

  /**
   * Cap the number of matches returned.
   * @param limit the maximum number of matches to return
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams limit(long limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Perform all predicate comparisons case-insensitively.
   * @return this {@link ArgrepParams}
   */
  public ArgrepParams nocase() {
    this.nocase = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (startRaw == null || endRaw == null) {
      throw new IllegalStateException(
          "ArgrepParams must be created via unbounded(), range(), from() or to()");
    }
    args.add(startRaw).add(endRaw);
    for (Predicate p : predicates) {
      args.add(p.type.keyword).add(p.value);
    }
    if (combinator != null) {
      args.add(combinator);
    }
    if (limit != null) {
      args.add(Keyword.LIMIT).add(limit);
    }
    if (nocase) {
      args.add(Keyword.NOCASE);
    }
  }

  private static byte[] encode(String s) {
    return redis.clients.jedis.util.SafeEncoder.encode(s);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArgrepParams that = (ArgrepParams) o;
    return nocase == that.nocase && Arrays.equals(startRaw, that.startRaw)
        && Arrays.equals(endRaw, that.endRaw) && Objects.equals(combinator, that.combinator)
        && Objects.equals(limit, that.limit) && Objects.equals(predicates, that.predicates);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(predicates, combinator, limit, nocase);
    result = 31 * result + Arrays.hashCode(startRaw);
    result = 31 * result + Arrays.hashCode(endRaw);
    return result;
  }
}
