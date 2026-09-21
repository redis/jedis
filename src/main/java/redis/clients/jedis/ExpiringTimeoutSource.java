package redis.clients.jedis;

class ExpiringTimeoutSource extends ChainedTimeoutSource {

  private volatile long expirationTime;

  ExpiringTimeoutSource(TimeoutInfo info) {
    super(info);
    this.expirationTime = 0;
  }

  @Override
  protected boolean isValid() {
    return expirationTime != 0 && expirationTime > NanoClock.INSTANCE.getAsLong();
  }

  public void setExpirationTime(long expirationTime) {
    this.expirationTime = expirationTime;
  }
}
