package redis.clients.jedis;

import redis.clients.jedis.TimeoutSource.UnplugableSource;

class TimeoutSourceNode implements TimeoutSource, UnplugableSource<TimeoutSource> {

  private volatile TimeoutSource override;
  private final TimeoutInfo info;

  TimeoutSourceNode(TimeoutInfo info) {
    this.info = info;
  }

  @Override
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

  @Override
  public synchronized void overrideWith(TimeoutSource other) {
    if (override == null) {
      override = other;
    } else {
      override.overrideWith(other);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized void unplug(UnplugableSource<TimeoutSource> other) {
    if (override == other) {
      override = other.getOverride();
    } else if (override != null && override instanceof UnplugableSource) {
      ((UnplugableSource<TimeoutSource>) override).unplug(other);
    }
  }

  @Override
  public TimeoutSource getOverride() {
    return override;
  }


}
