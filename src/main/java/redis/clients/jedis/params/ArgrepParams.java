package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Optional arguments for the {@code ARGREP} command.
 * <p>
 * Predicates are added in the order their fluent setters are invoked. Logical combinator
 * ({@code AND}/{@code OR}), {@code LIMIT} and {@code NOCASE} are emitted after the predicate list,
 * matching the order required by the Redis wire protocol. The {@code WITHVALUES} flag is not
 * exposed here: use {@code argrepWithValues} to request index/value pairs.
 */
public class ArgrepParams implements IParams {

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
  }

  private final List<Predicate> predicates = new ArrayList<>();
  private Keyword combinator;
  private Long limit;
  private boolean nocase;

  /**
   * Create a new empty {@link ArgrepParams}.
   * @return a fresh parameter object
   */
  public static ArgrepParams argrepParams() {
    return new ArgrepParams();
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
    return nocase == that.nocase && Objects.equals(combinator, that.combinator)
        && Objects.equals(limit, that.limit)
        && Objects.equals(predicates.size(), that.predicates.size());
  }

  @Override
  public int hashCode() {
    return Objects.hash(predicates.size(), combinator, limit, nocase);
  }
}
