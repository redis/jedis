package redis.clients.jedis;

/**
 * Terminal chain link holding the configured baseline timeouts; always has an opinion. Overrides
 * chained onto this source are maintenance relaxations, so their values are reduced per field to
 * the looser of the configured and the relaxed value — {@code 0} (infinite) being the loosest —
 * ensuring a relaxation can only ever loosen the configured deadline, never tighten it.
 */
class DefaultTimeoutSource extends ChainedTimeoutSource {

  private volatile TimeoutInfo ownInfo;

  /**
   * Memo of the last fused, keyed by the exact (defaults, relaxed) instance pair. Both inputs are
   * reference-stable between configuration changes, so the pre-read hot path stays allocation-free.
   */
  private volatile FusedInfo fusedInfo;

  DefaultTimeoutSource(TimeoutInfo info) {
    super(info);
    ownInfo = info;
  }

  @Override
  public TimeoutInfo get() {
    TimeoutInfo defaults = ownInfo;
    TimeoutInfo relaxed = getOverrideInfo();
    if (relaxed == null) {
      return defaults;
    }
    FusedInfo current = fusedInfo;
    if (current != null && current.defaults == defaults && current.relaxed == relaxed) {
      return current.fused;
    }
    fusedInfo = new FusedInfo(defaults, relaxed);
    return fusedInfo.fused;
  }

  private static final class FusedInfo {

    private final TimeoutInfo defaults;
    private final TimeoutInfo relaxed;
    private final TimeoutInfo fused;

    FusedInfo(TimeoutInfo defaults, TimeoutInfo relaxed) {
      this.defaults = defaults;
      this.relaxed = relaxed;
      this.fused = new TimeoutInfo(loosest(defaults.timeout, relaxed.timeout),
          loosest(defaults.blockingTimeout, relaxed.blockingTimeout));
    }

    /** {@code 0} means infinite, so it beats any finite value. */
    private static int loosest(int configured, int relaxed) {
      return (configured == 0 || relaxed == 0) ? 0 : Math.max(configured, relaxed);
    }
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
