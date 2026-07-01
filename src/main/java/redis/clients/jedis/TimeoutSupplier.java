package redis.clients.jedis;

interface TimeoutSupplier {

  TimeoutCard get();

  TimeoutCard push(TimeoutInfo info);

  void remove(TimeoutCard card);

  void setDefaults(int timeout, int blockingTimeout);

  TimeoutCard getDefaults();

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
      return expiration > NanoClock.INSTANCE.getAsLong();
    }

    static TimeoutInfo ofDuration(int timeout, int blockingTimeout, long duration) {
      return new TimeoutInfo(timeout, blockingTimeout, NanoClock.INSTANCE.getAsLong() + duration);
    }
  }

  class TimeoutCard {

    private final TimeoutInfo info;

    TimeoutCard(TimeoutInfo info) {
      this.info = info;
    }

    boolean isValid() {
      return getInfo().isValid();
    }

    TimeoutInfo getInfo() {
      return info;
    }
  }

  class DefaultTimeoutCard extends TimeoutCard {
    private TimeoutInfo info;

    DefaultTimeoutCard(int timeout, int blockingTimeout) {
      super(notExpiringInfo(timeout, blockingTimeout));
      this.info = super.getInfo();
    }

    @Override
    TimeoutInfo getInfo() {
      return info;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    void set(int timeout, int blockingTimeout) {
      this.info = notExpiringInfo(timeout, blockingTimeout);
    }

    private static TimeoutInfo notExpiringInfo(int timeout, int blockingTimeout) {
      return new TimeoutInfo(timeout, blockingTimeout, Long.MAX_VALUE) {
        boolean isValid() {
          return true;
        }
      };
    }
  }
}
