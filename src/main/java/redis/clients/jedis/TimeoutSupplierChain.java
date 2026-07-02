package redis.clients.jedis;

interface TimeoutSupplierChain {

  TimeoutInfo get();

  void overrideWith(TimeoutSupplierChain other);

  class TimeoutInfo {

    final int timeout;
    final int blockingTimeout;

    TimeoutInfo(int timeout, int blockingTimeout) {
      this.timeout = timeout;
      this.blockingTimeout = blockingTimeout;
    }
  }
}
