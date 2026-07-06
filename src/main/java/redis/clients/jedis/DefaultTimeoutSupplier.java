package redis.clients.jedis;

class DefaultTimeoutSupplier extends TimeoutSupplierDecorator {

  TimeoutInfo ownInfo;

  DefaultTimeoutSupplier(TimeoutInfo info) {
    super(info);
    ownInfo = info;
  }

  @Override
  protected boolean isValid() {
    return true;
  }

  @Override
  protected TimeoutInfo getOwnInfo() {
    return ownInfo;
  }

  void setDefaults(int timeoutMillis, int blockingTimeoutMillis) {
    ownInfo = new TimeoutInfo(timeoutMillis, blockingTimeoutMillis);
  }

  TimeoutInfo getDefaults() {
    return ownInfo;
  }
}
