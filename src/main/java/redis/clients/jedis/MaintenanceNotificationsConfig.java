package redis.clients.jedis;

import java.net.SocketAddress;
import java.time.Duration;

import redis.clients.jedis.util.JedisAsserts;
import redis.clients.jedis.util.NetUtils;

public class MaintenanceNotificationsConfig {

  /** Default upper bound on the relaxed-timeout window started by MIGRATING/FAILING_OVER/MOVING. */
  public static final Duration DEFAULT_RELAXED_WINDOW_MAX_DURATION = Duration.ofSeconds(60);

  public static final int DEFAULT_RELAXED_SOCKET_TIMEOUT_MS = 10_000;

  public static final int DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS = 0;

  private MaintenanceNotificationsConfig(Builder builder) {
    this.endpointTypeSource = builder.endpointTypeSource;
    this.mode = builder.mode;
    this.relaxedWindowMaxDuration = builder.relaxedWindowMaxDuration;
    this.relaxedTimeout = builder.relaxedTimeout;
    this.relaxedBlockingTimeout = builder.relaxedBlockingTimeout;
  }

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
   * Strategy determining the {@link EndpointType} to request in MOVING notifications, evaluated per
   * connection at handshake time.
   * @since 8.0
   */
  public interface EndpointTypeSource {

    /**
     * Determines the endpoint type based on connection characteristics.
     * @param remoteAddress the remote socket address of the connection
     * @param sslEnabled whether TLS/SSL is enabled for the connection
     * @return the {@link EndpointType} to request
     */
    EndpointType getEndpointType(SocketAddress remoteAddress, boolean sslEnabled);
  }

  /**
   * Auto-resolves from connection characteristics: private remote IP (see
   * {@link NetUtils#isPrivateIp}) selects {@code INTERNAL_*}, public {@code EXTERNAL_*}; TLS
   * selects {@code *_FQDN}, plaintext {@code *_IP}.
   */
  private static final class AutoResolveEndpointTypeSource implements EndpointTypeSource {

    static final AutoResolveEndpointTypeSource INSTANCE = new AutoResolveEndpointTypeSource();

    @Override
    public EndpointType getEndpointType(SocketAddress remoteAddress, boolean sslEnabled) {
      if (NetUtils.isPrivateIp(remoteAddress)) {
        return sslEnabled ? EndpointType.INTERNAL_FQDN : EndpointType.INTERNAL_IP;
      }
      return sslEnabled ? EndpointType.EXTERNAL_FQDN : EndpointType.EXTERNAL_IP;
    }

    @Override
    public String toString() {
      return "AutoResolveEndpointTypeSource";
    }
  }

  /** Always requests the user-chosen endpoint type, ignoring connection characteristics. */
  private static final class FixedEndpointTypeSource implements EndpointTypeSource {

    private final EndpointType endpointType;

    FixedEndpointTypeSource(EndpointType endpointType) {
      this.endpointType = endpointType;
    }

    @Override
    public EndpointType getEndpointType(SocketAddress remoteAddress, boolean sslEnabled) {
      return endpointType;
    }

    @Override
    public String toString() {
      return "FixedEndpointTypeSource(" + endpointType + ")";
    }
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

  private final EndpointTypeSource endpointTypeSource;
  private final Mode mode;
  private final Duration relaxedWindowMaxDuration;
  private final int relaxedTimeout;
  private final int relaxedBlockingTimeout;

  /**
   * The strategy that decides which endpoint type to request in MOVING notifications; defaults to
   * auto-resolution from connection characteristics.
   * @since 8.0
   */
  public EndpointTypeSource getEndpointTypeSource() {
    return endpointTypeSource;
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

  public int getRelaxedTimeout() {
    return relaxedTimeout;
  }

  public int getRelaxedBlockingTimeout() {
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
    private EndpointTypeSource endpointTypeSource = AutoResolveEndpointTypeSource.INSTANCE;
    private Mode mode = Mode.AUTO;
    private Duration relaxedWindowMaxDuration = DEFAULT_RELAXED_WINDOW_MAX_DURATION;
    private int relaxedTimeout = DEFAULT_RELAXED_SOCKET_TIMEOUT_MS;
    private int relaxedBlockingTimeout = DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS;

    /**
     * Requests a fixed endpoint type for all MOVING notifications. Mutually exclusive with
     * {@link #autoResolveEndpointType()}; the last call wins.
     * @since 8.0
     */
    public Builder endpointType(EndpointType endpointType) {
      JedisAsserts.notNull(endpointType, "endpointType must not be null");
      this.endpointTypeSource = new FixedEndpointTypeSource(endpointType);
      return this;
    }

    /**
     * Requests the endpoint type resolved per connection from its characteristics: private remote
     * IP selects {@code INTERNAL_*}, public {@code EXTERNAL_*}; TLS selects {@code *_FQDN},
     * plaintext {@code *_IP}. This is the default. Mutually exclusive with
     * {@link #endpointType(EndpointType)}; the last call wins.
     * @since 8.0
     */
    public Builder autoResolveEndpointType() {
      this.endpointTypeSource = AutoResolveEndpointTypeSource.INSTANCE;
      return this;
    }

    public Builder mode(Mode mode) {
      this.mode = mode;
      return this;
    }

    public Builder relaxedTimeout(int millis) {
      this.relaxedTimeout = millis;
      return this;
    }

    public Builder relaxedBlockingTimeout(int millis) {
      this.relaxedBlockingTimeout = millis;
      return this;
    }

    /**
     * Upper bound on relaxation triggered by MIGRATING/FAILING_OVER/MOVING. Acts as a safety net:
     * the relaxed window reverts after this duration even if the matching closing notification is
     * lost. Defaults to {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_WINDOW_MAX_DURATION}.
     */
    public Builder relaxedWindowMaxDuration(Duration duration) {
      JedisAsserts.notNull(duration, "duration must not be null");
      this.relaxedWindowMaxDuration = duration;
      return this;
    }

    public MaintenanceNotificationsConfig build() {
      return new MaintenanceNotificationsConfig(this);
    }
  }

}
