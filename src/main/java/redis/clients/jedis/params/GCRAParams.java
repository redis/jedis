package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.util.JedisAsserts;

import java.util.Objects;

/**
 * Parameters for the Redis <a href="https://redis.io/commands/gcra">GCRA</a> command.
 * <p>
 * GCRA (Generic Cell Rate Algorithm) provides rate limiting. Use the static factory method
 * {@link #gcraParams(long, long, double)} to create an instance with the required parameters.
 * <p>
 * Command syntax: {@code GCRA key max_burst tokens_per_period period [TOKENS count]}
 */
public class GCRAParams implements IParams {

  private long maxBurst;

  private long tokensPerPeriod;

  private double period;

  private Long tokens;

  public GCRAParams() {
  }

  /**
   * Creates new {@link GCRAParams} with the required parameters.
   * @param maxBurst maximum number of tokens allowed as a burst. Min: 0.
   * @param tokensPerPeriod number of tokens allowed per period. Min: 1.
   * @param period period in seconds. Min: 1.0, Max: 1e12.
   * @return new {@link GCRAParams} with required parameters set.
   */
  public static GCRAParams gcraParams(long maxBurst, long tokensPerPeriod, double period) {
    return new GCRAParams().maxBurst(maxBurst).tokensPerPeriod(tokensPerPeriod).period(period);
  }

  /**
   * Set the maximum number of tokens allowed as a burst.
   * @param maxBurst maximum burst size. Min: 0.
   * @return {@code this} {@link GCRAParams}.
   */
  public GCRAParams maxBurst(long maxBurst) {
    JedisAsserts.isTrue(maxBurst >= 0, "maxBurst must be >= 0");
    this.maxBurst = maxBurst;
    return this;
  }

  /**
   * Set the number of tokens allowed per period.
   * @param tokensPerPeriod tokens per period. Min: 1.
   * @return {@code this} {@link GCRAParams}.
   */
  public GCRAParams tokensPerPeriod(long tokensPerPeriod) {
    JedisAsserts.isTrue(tokensPerPeriod >= 1, "tokensPerPeriod must be >= 1");
    this.tokensPerPeriod = tokensPerPeriod;
    return this;
  }

  /**
   * Set the period in seconds.
   * @param period period in seconds. Min: 1.0, Max: 1e12.
   * @return {@code this} {@link GCRAParams}.
   */
  public GCRAParams period(double period) {
    JedisAsserts.isTrue(period >= 1.0, "period must be >= 1.0");
    JedisAsserts.isTrue(period <= 1e12, "period must be <= 1e12");
    this.period = period;
    return this;
  }

  /**
   * Set the cost/weight of this request. Defaults to 1 if not specified.
   * @param tokens cost of the request in tokens. Min: 1.
   * @return {@code this} {@link GCRAParams}.
   */
  public GCRAParams tokens(long tokens) {
    JedisAsserts.isTrue(tokens >= 1, "tokens must be >= 1");
    this.tokens = tokens;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(maxBurst);
    args.add(tokensPerPeriod);
    args.add(period);

    if (tokens != null) {
      args.add(Keyword.TOKENS);
      args.add(tokens);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GCRAParams that = (GCRAParams) o;
    return maxBurst == that.maxBurst && tokensPerPeriod == that.tokensPerPeriod
        && Double.compare(that.period, period) == 0 && Objects.equals(tokens, that.tokens);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxBurst, tokensPerPeriod, period, tokens);
  }
}
