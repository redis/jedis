package redis.clients.jedis;

class ExpiringTimeoutSupplier extends TimeoutSupplierDecorator {

  private volatile long expirationTime;

  ExpiringTimeoutSupplier(TimeoutInfo info) {
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
