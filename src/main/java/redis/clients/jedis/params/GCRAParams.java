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
 * Command syntax: {@code GCRA key max_burst requests_per_period period [NUM_REQUESTS count]}
 */
public class GCRAParams implements IParams {

  private long maxBurst;

  private long requestsPerPeriod;

  private double period;

  private Long numRequests;

  public GCRAParams() {
  }

  /**
   * Creates new {@link GCRAParams} with the required parameters.
   * @param maxBurst maximum number of tokens allowed as a burst. Min: 0.
   * @param requestsPerPeriod number of requests allowed per period. Min: 1.
   * @param period period in seconds. Min: 1.0, Max: 1e12.
   * @return new {@link GCRAParams} with required parameters set.
   */
  public static GCRAParams gcraParams(long maxBurst, long requestsPerPeriod, double period) {
    return new GCRAParams().maxBurst(maxBurst).requestsPerPeriod(requestsPerPeriod).period(period);
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
   * Set the number of requests allowed per period.
   * @param requestsPerPeriod requests per period. Min: 1.
   * @return {@code this} {@link GCRAParams}.
   */
  public GCRAParams requestsPerPeriod(long requestsPerPeriod) {
    JedisAsserts.isTrue(requestsPerPeriod >= 1, "requestsPerPeriod must be >= 1");
    this.requestsPerPeriod = requestsPerPeriod;
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
   * @param numRequests cost of the request. Min: 1.
   * @return {@code this} {@link GCRAParams}.
   */
  public GCRAParams numRequests(long numRequests) {
    JedisAsserts.isTrue(numRequests >= 1, "numRequests must be >= 1");
    this.numRequests = numRequests;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(maxBurst);
    args.add(requestsPerPeriod);
    args.add(period);

    if (numRequests != null) {
      args.add(Keyword.NUM_REQUESTS);
      args.add(numRequests);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GCRAParams that = (GCRAParams) o;
    return maxBurst == that.maxBurst && requestsPerPeriod == that.requestsPerPeriod
        && Double.compare(that.period, period) == 0
        && Objects.equals(numRequests, that.numRequests);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxBurst, requestsPerPeriod, period, numRequests);
  }
}
