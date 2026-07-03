package redis.clients.jedis;

class TimeoutSupplierDecorator implements TimeoutSupplierChain {

  private volatile TimeoutSupplierChain override;
  private final TimeoutInfo info;

  TimeoutSupplierDecorator(TimeoutInfo info) {
    this.info = info;
  }

  public TimeoutInfo get() {
    if (override != null) {
      TimeoutInfo fromOverride = override.get();
      if (fromOverride != null) {
        return fromOverride;
      }
    }
    return getOwnInfo();
  }

  protected TimeoutInfo getOwnInfo() {
    return isValid() ? info : null;
  }

  protected boolean isValid() {
    return true;
  }

  public synchronized void overrideWith(TimeoutSupplierChain other) {
    if (override == null) {
      override = other;
    } else {
      override.overrideWith(other);
    }
  }

}
