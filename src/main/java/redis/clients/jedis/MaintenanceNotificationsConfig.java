package redis.clients.jedis;

import java.time.Duration;

public class MaintenanceNotificationsConfig {

  /** Default upper bound on the relaxed-timeout window started by MIGRATING/FAILING_OVER/MOVING. */
  public static final Duration DEFAULT_RELAXED_WINDOW_MAX_DURATION = Duration.ofSeconds(60);

  public static final MaintenanceNotificationsConfig DEFAULT = new MaintenanceNotificationsConfig();

  /**
   * Endpoint types for maintenance event notifications.
   * <p>
   * Determines the format of endpoint addresses returned in MOVING notifications.
   * @since 8.0
   */
  public enum EndpointType {
    /** Internal IP address (for private network connections) */
    INTERNAL_IP,
    /** Internal fully qualified domain name (for private network connections with TLS) */
    INTERNAL_FQDN,
    /** External IP address (for public network connections) */
    EXTERNAL_IP,
    /** External fully qualified domain name (for public network connections with TLS) */
    EXTERNAL_FQDN
  }

  /**
   * Mode for maintenance event notifications.
   * <ul>
   * <li>ENABLED - Maintenance notifications are explicitly enabled. Both timeout relaxation and
   * proactive rebind are activated. Server must support the feature.</li>
   * <li>DISABLED - Maintenance notifications are explicitly disabled.</li>
   * <li>AUTO - Maintenance notifications are automatically enabled if the server supports them.
   * Silently falls back if not supported. Both timeout relaxation and proactive rebind are
   * activated when successful.</li>
   * </ul>
   */
  public enum Mode {
    ENABLED, DISABLED, AUTO
  }

  EndpointType endpointType = EndpointType.EXTERNAL_IP;
  Mode mode = Mode.AUTO;
  Duration relaxedWindowMaxDuration = DEFAULT_RELAXED_WINDOW_MAX_DURATION;
  int relaxedTimeout = 0;
  int relaxedBlockingTimeout = 0;

  public EndpointType getEndpointType() {
    return endpointType;
  }

  public Mode getMode() {
    return mode;
  }

  /**
   * Upper bound on the relaxed-timeout window started by MIGRATING/FAILING_OVER/MOVING. The window
   * reverts automatically after this duration even if the matching closing notification is never
   * received. Safety net against missed events or misbehaving servers.
   */
  public Duration getRelaxedWindowMaxDuration() {
    return relaxedWindowMaxDuration;
  }

  /**
   * Returns whether maintenance event notifications are enabled. When enabled, both timeout
   * relaxation and proactive rebind features are activated.
   * @return true if mode is ENABLED or AUTO, false if DISABLED
   */
  public boolean isEnabledOrAuto() {
    return mode == Mode.ENABLED || mode == Mode.AUTO;
  }

  public int relaxedTimeout() {
    return relaxedTimeout;
  }

  public int relaxedBlockingTimeout() {
    return relaxedBlockingTimeout;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private EndpointType endpointType = EndpointType.EXTERNAL_IP;
    private Mode mode = Mode.AUTO;
    private Duration relaxedWindowMaxDuration = DEFAULT_RELAXED_WINDOW_MAX_DURATION;
    private int relaxedTimeout = 0;
    private int relaxedBlockingTimeout = 0;

    public Builder endpointType(EndpointType endpointType) {
      this.endpointType = endpointType;
      return this;
    }

    public Builder mode(Mode mode) {
      this.mode = mode;
      return this;
    }

    public Builder relaxedTimeout(int timeout) {
      this.relaxedTimeout = timeout;
      return this;
    }

    public Builder relaxedBlockingTimeout(int blockingTimeout) {
      this.relaxedBlockingTimeout = blockingTimeout;
      return this;
    }

    /**
     * Upper bound on relaxation triggered by MIGRATING/FAILING_OVER/MOVING. Acts as a safety net:
     * the relaxed window reverts after this duration even if the matching closing notification is
     * lost. Defaults to {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_WINDOW_MAX_DURATION}.
     */
    public Builder relaxedWindowMaxDuration(Duration duration) {
      if (duration == null) throw new IllegalArgumentException("duration must not be null");
      this.relaxedWindowMaxDuration = duration;
      return this;
    }

    public MaintenanceNotificationsConfig build() {
      MaintenanceNotificationsConfig config = new MaintenanceNotificationsConfig();
      config.endpointType = this.endpointType;
      config.mode = this.mode;
      config.relaxedWindowMaxDuration = this.relaxedWindowMaxDuration;
      config.relaxedTimeout = this.relaxedTimeout;
      config.relaxedBlockingTimeout = this.relaxedBlockingTimeout;
      return config;
    }
  }

}
