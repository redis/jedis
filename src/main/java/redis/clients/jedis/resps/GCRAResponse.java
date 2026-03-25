package redis.clients.jedis.resps;

import redis.clients.jedis.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the response from the Redis <a href="https://redis.io/commands/gcra">GCRA</a> command.
 * <p>
 * The GCRA (Generic Cell Rate Algorithm) command is used for rate limiting. The response contains
 * information about whether the request was rate-limited and the current state of the rate limiter.
 * <p>
 * Response array:
 * <ol>
 * <li>{@code limited} - 0 or 1 indicating if the request was rate-limited</li>
 * <li>{@code maxRequests} - the maximum number of requests allowed (max_burst + 1)</li>
 * <li>{@code availableRequests} - the number of requests still available</li>
 * <li>{@code retryAfter} - seconds until the request should be retried (-1 if not limited)</li>
 * <li>{@code fullBurstAfter} - seconds until full burst capacity is restored</li>
 * </ol>
 */
public class GCRAResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final int EXPECTED_SIZE = 5;

  private final boolean limited;
  private final long maxRequests;
  private final long availableRequests;
  private final long retryAfter;
  private final long fullBurstAfter;

  public GCRAResponse(boolean limited, long maxRequests, long availableRequests, long retryAfter,
      long fullBurstAfter) {
    this.limited = limited;
    this.maxRequests = maxRequests;
    this.availableRequests = availableRequests;
    this.retryAfter = retryAfter;
    this.fullBurstAfter = fullBurstAfter;
  }

  /**
   * @return {@code true} if the request was rate-limited, {@code false} otherwise.
   */
  public boolean isLimited() {
    return limited;
  }

  /**
   * @return the maximum number of requests allowed in the period (max_burst + 1).
   */
  public long getMaxRequests() {
    return maxRequests;
  }

  /**
   * @return the number of requests still available in the current period.
   */
  public long getAvailableRequests() {
    return availableRequests;
  }

  /**
   * @return seconds until the request should be retried. Returns -1 if the request was not limited.
   */
  public long getRetryAfter() {
    return retryAfter;
  }

  /**
   * @return seconds until the full burst capacity is restored.
   */
  public long getFullBurstAfter() {
    return fullBurstAfter;
  }

  @Override
  public String toString() {
    return "GCRAResponse{" + "limited=" + limited + ", maxRequests=" + maxRequests
        + ", availableRequests=" + availableRequests + ", retryAfter=" + retryAfter
        + ", fullBurstAfter=" + fullBurstAfter + '}';
  }

  public static final Builder<GCRAResponse> GCRA_RESPONSE_BUILDER = new Builder<GCRAResponse>() {
    @Override
    @SuppressWarnings("unchecked")
    public GCRAResponse build(Object data) {
      if (data == null) {
        return null;
      }

      List<Long> list = (List<Long>) data;
      if (list.size() < EXPECTED_SIZE) {
        throw new IllegalArgumentException("GCRA response expected at least " + EXPECTED_SIZE
            + " elements but got " + list.size());
      }

      boolean limited = list.get(0) != 0;
      long maxRequests = list.get(1);
      long availableRequests = list.get(2);
      long retryAfter = list.get(3);
      long fullBurstAfter = list.get(4);

      return new GCRAResponse(limited, maxRequests, availableRequests, retryAfter, fullBurstAfter);
    }

    @Override
    public String toString() {
      return "GCRAResponse";
    }
  };
}
