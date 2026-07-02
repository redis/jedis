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

  void setDefaults(int timeout, int blockingTimeout) {
    ownInfo = new TimeoutInfo(timeout, blockingTimeout);
  }

  TimeoutInfo getDefaults() {
    return ownInfo;
  }
}