package redis.clients.jedis;

/**
 * Chain-of-responsibility link over {@link TimeoutSource}: {@link #get()} consults the overrides
 * first (deepest valid override wins) and falls back to this source's own value when none has an
 * opinion.
 */
class ChainedTimeoutSource implements TimeoutSource {

  private volatile ChainedTimeoutSource override;
  private final TimeoutInfo info;

  ChainedTimeoutSource(TimeoutInfo info) {
    this.info = info;
  }

  @Override
  public TimeoutInfo get() {
    TimeoutInfo fromOverride = getOverrideInfo();
    return fromOverride != null ? fromOverride : getOwnInfo();
  }

  /** The deepest valid override's timeouts, or {@code null} when no override has an opinion. */
  protected final TimeoutInfo getOverrideInfo() {
    ChainedTimeoutSource o = override;
    return o != null ? o.get() : null;
  }

  protected TimeoutInfo getOwnInfo() {
    return isValid() ? info : null;
  }

  protected boolean isValid() {
    return true;
  }

  /** Appends {@code other} to the end of the chain; the last added override takes precedence. */
  synchronized void addOverride(ChainedTimeoutSource other) {
    if (override == null) {
      override = other;
    } else {
      override.addOverride(other);
    }
  }

  /** Unlinks {@code other} from the chain, preserving any overrides added after it. */
  synchronized void removeOverride(ChainedTimeoutSource other) {
    if (override == other) {
      override = other.override;
    } else if (override != null) {
      override.removeOverride(other);
    }
  }
}