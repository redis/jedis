package redis.clients.jedis;

import java.time.Duration;

public class MaintenanceNotificationsConfig {

  /** Default upper bound on the relaxed-timeout window started by MIGRATING/FAILING_OVER/MOVING. */
  public static final Duration DEFAULT_RELAXED_WINDOW_MAX_DURATION = Duration.ofSeconds(60);

  /** Default per-command socket timeout (ms) applied while a relaxation window is active. */
  public static final int DEFAULT_RELAXED_SOCKET_TIMEOUT_MS = 10_000;

  /** Default relaxed blocking-command timeout (ms): unset, i.e. inherit the configured one. */
  public static final int DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS = JedisClientConfig.UNSET_TIMEOUT_MS;

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
  int relaxedTimeout = DEFAULT_RELAXED_SOCKET_TIMEOUT_MS;
  int relaxedBlockingTimeout = DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS;

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
   * Per-command socket timeout (ms) applied while a maintenance relaxation window is active
   * (MIGRATING / FAILING_OVER / MOVING). {@link JedisClientConfig#UNSET_TIMEOUT_MS} inherits the
   * connection's configured socket timeout during the window.
   */
  public int getRelaxedSocketTimeoutMillis() {
    return relaxedSocketTimeoutMillis;
  }

  /**
   * Per-command timeout (ms) applied to blocking commands while a relaxation window is active.
   * {@link JedisClientConfig#UNSET_TIMEOUT_MS} inherits the configured blocking timeout.
   */
  public int getRelaxedBlockingSocketTimeoutMillis() {
    return relaxedBlockingSocketTimeoutMillis;
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

  /**
   * Maintenance notifications disabled. To enable — optionally with custom relaxed timeouts — use
   * {@link #builder()} with mode {@link Mode#AUTO} or {@link Mode#ENABLED}.
   */
  public static final MaintenanceNotificationsConfig DISABLED = builder().mode(Mode.DISABLED)
      .build();

  public static class Builder {
    private EndpointType endpointType = EndpointType.EXTERNAL_IP;
    private Mode mode = Mode.AUTO;
    private Duration relaxedWindowMaxDuration = DEFAULT_RELAXED_WINDOW_MAX_DURATION;
    private int relaxedTimeout = DEFAULT_RELAXED_SOCKET_TIMEOUT_MS;
    private int relaxedBlockingTimeout = DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS;

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

    /**
     * Per-command socket timeout (ms) applied while a relaxation window is active. Pass
     * {@link JedisClientConfig#UNSET_TIMEOUT_MS} to inherit the connection's configured socket
     * timeout. Defaults to
     * {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_SOCKET_TIMEOUT_MS}.
     */
    public Builder relaxedSocketTimeoutMillis(int millis) {
      this.relaxedSocketTimeoutMillis = millis;
      return this;
    }

    /**
     * Per-command timeout (ms) applied to blocking commands while a relaxation window is active.
     * Pass {@link JedisClientConfig#UNSET_TIMEOUT_MS} to inherit the configured blocking timeout.
     * Defaults to
     * {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS}.
     */
    public Builder relaxedBlockingSocketTimeoutMillis(int millis) {
      this.relaxedBlockingSocketTimeoutMillis = millis;
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
