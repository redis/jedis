package redis.clients.jedis;

class TimeoutSupplierDecorator implements TimeoutSupplierChain {

  private volatile TimeoutSupplierChain override;
  private final TimeoutInfo info;

  TimeoutSupplierDecorator(TimeoutInfo info) {
    this.info = info;
  }

  public TimeoutInfo get() {
    if (override != null) {
      TimeoutInfo info = override.get();
      if (info != null) {
        return info;
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
