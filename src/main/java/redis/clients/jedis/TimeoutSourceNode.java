package redis.clients.jedis;

class TimeoutSourceNode implements TimeoutSource {

  private volatile TimeoutSource override;
  private final TimeoutInfo info;

  TimeoutSourceNode(TimeoutInfo info) {
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

  public synchronized void overrideWith(TimeoutSource other) {
    if (override == null) {
      override = other;
    } else {
      override.overrideWith(other);
    }
  }

}
