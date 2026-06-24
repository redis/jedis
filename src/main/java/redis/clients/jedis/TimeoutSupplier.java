package redis.clients.jedis;

import java.util.function.LongSupplier;

interface TimeoutSupplier {

  static LongSupplier clockNanos = System::nanoTime;

  TimeoutCard get();

  TimeoutCard push(TimeoutInfo info);

  void remove(TimeoutCard card);

  class TimeoutInfo {

    final int timeout;
    final int blockingTimeout;
    final long expiration;

    TimeoutInfo(int timeout, int blockingTimeout, long expiration) {
      this.timeout = timeout;
      this.blockingTimeout = blockingTimeout;
      this.expiration = expiration;

    }

    boolean isValid() {
      return expiration > clockNanos.getAsLong();
    }

    static TimeoutInfo ofDuration(int timeout, int blockingTimeout, long duration) {
      return new TimeoutInfo(timeout, blockingTimeout, clockNanos.getAsLong() + duration);
    }
  }

  class TimeoutCard {

    private final TimeoutInfo info;
    private volatile boolean consumed = false;

    TimeoutCard(TimeoutInfo info) {
      this.info = info;
    }

    boolean isConsumed() {
      return consumed;
    }

    boolean isValid() {
      return getInfo().isValid();
    }

    void readyConsume() {
      consumed = false;
    }

    void markConsumed() {
      consumed = true;
    }

    TimeoutInfo getInfo() {
      return info;
    }
  }

  class DefaultTimeoutCard extends TimeoutCard {
    private TimeoutInfo info;

    DefaultTimeoutCard(int timeout, int blockingTimeout) {
      super(new TimeoutInfo(timeout, blockingTimeout, Long.MAX_VALUE));
      this.info = super.getInfo();
    }

    @Override
    TimeoutInfo getInfo() {
      return info;
    }

    void set(int timeout, int blockingTimeout) {
      this.info = new TimeoutInfo(timeout, blockingTimeout, Long.MAX_VALUE);
      readyConsume();
    }

  }
}
