package redis.clients.jedis;

interface TimeoutSupplierChain {

  TimeoutInfo get();

  void overrideWith(TimeoutSupplierChain other);

  class TimeoutInfo {

    final int timeout;
    final int blockingTimeout;

    TimeoutInfo(int timeoutMillis, int blockingTimeoutMillis) {
      this.timeout = timeoutMillis;
      this.blockingTimeout = blockingTimeoutMillis;
    }
  }
}
