package redis.clients.jedis;

/** Terminal chain link holding the configured baseline timeouts; always has an opinion. */
class DefaultTimeoutSource extends ChainedTimeoutSource {

  TimeoutInfo ownInfo;

  DefaultTimeoutSource(TimeoutInfo info) {
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