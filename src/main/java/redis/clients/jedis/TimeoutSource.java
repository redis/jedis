package redis.clients.jedis;

interface TimeoutSource {

  TimeoutInfo get();

  void overrideWith(TimeoutSource other);

  class TimeoutInfo {

    final int timeout;
    final int blockingTimeout;

    TimeoutInfo(int timeoutMillis, int blockingTimeoutMillis) {
      this.timeout = timeoutMillis;
      this.blockingTimeout = blockingTimeoutMillis;
    }
  }
}
