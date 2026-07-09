package redis.clients.jedis;

/** Provider of the socket read timeouts consulted by {@link Connection} before protocol reads. */
interface TimeoutSource {

  /**
   * The timeouts this source wants applied right now, or {@code null} when it has no opinion so
   * that a chained caller falls back to the next source.
   */
  TimeoutInfo get();

  /** Immutable pair of socket read timeouts in milliseconds; {@code 0} means infinite. */
  class TimeoutInfo {

    final int timeout;
    final int blockingTimeout;

    TimeoutInfo(int timeoutMillis, int blockingTimeoutMillis) {
      this.timeout = timeoutMillis;
      this.blockingTimeout = blockingTimeoutMillis;
    }
  }
}