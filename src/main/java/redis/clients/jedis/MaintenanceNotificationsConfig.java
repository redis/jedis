package redis.clients.jedis;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;

import redis.clients.jedis.util.JedisAsserts;

/**
 * Configuration of the {@code CLIENT MAINT_NOTIFICATIONS} feature: how the client reacts to server
 * maintenance push notifications — timeout relaxation and proactive endpoint handoff.
 * <p>
 * Maintenance notifications require RESP3: on a RESP2 connection {@link Mode#AUTO} (the default)
 * leaves the feature inactive and {@link Mode#ENABLED} fails the connection setup.
 * @since 8.1
 */
public class MaintenanceNotificationsConfig {

  /**
   * Default upper bound on the relaxed-timeout window started by MIGRATING/FAILING_OVER when the
   * matching closing notification is lost. MOVING windows end at their own grace period instead.
   * @since 8.1
   */
  public static final Duration DEFAULT_RELAXED_WINDOW_MAX_DURATION = Duration.ofSeconds(60);

  /**
   * Default relaxed socket timeout in milliseconds.
   * @since 8.1
   */
  public static final int DEFAULT_RELAXED_SOCKET_TIMEOUT_MS = 10_000;

  /**
   * Default relaxed blocking socket timeout in milliseconds; {@code 0} means infinite.
   * @since 8.1
   */
  public static final int DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS = 0;

  private MaintenanceNotificationsConfig(Builder builder) {
    this.endpointTypeResolver = builder.endpointTypeResolver;
    this.mode = builder.mode;
    this.relaxedWindowMaxDuration = builder.relaxedWindowMaxDuration;
    this.relaxedTimeout = builder.relaxedTimeout;
    this.relaxedBlockingTimeout = builder.relaxedBlockingTimeout;
  }

  /**
   * Endpoint types for maintenance event notifications.
   * <p>
   * Determines the format of endpoint addresses returned in MOVING notifications.
   * @since 8.1
   */
  public enum EndpointType {
    /** Internal IP address (for private network connections) */
    INTERNAL_IP,
    /** Internal fully qualified domain name (for private network connections with TLS) */
    INTERNAL_FQDN,
    /** External IP address (for public network connections) */
    EXTERNAL_IP,
    /** External fully qualified domain name (for public network connections with TLS) */
    EXTERNAL_FQDN,
    /**
     * No endpoint: MOVING carries a null target. The client does not remap; it reconnects to the
     * currently-configured endpoint at half the grace period.
     * @since 8.1
     */
    NONE
  }

  /**
   * Strategy determining the {@link EndpointType} to request in MOVING notifications, evaluated per
   * connection at handshake time.
   * @since 8.1
   */
  public interface EndpointTypeResolver {

    /**
     * Determines the endpoint type based on connection characteristics.
     * @param remoteAddress the remote socket address of the connection
     * @param sslEnabled whether TLS/SSL is enabled for the connection
     * @return the {@link EndpointType} to request
     */
    EndpointType getEndpointType(SocketAddress remoteAddress, boolean sslEnabled);
  }

  /**
   * Auto-resolves from connection characteristics: a private remote IP (loopback, link-local,
   * site-local, or IPv6 unique-local) selects {@code INTERNAL_*}, a public one {@code EXTERNAL_*};
   * TLS selects {@code *_FQDN}, plaintext {@code *_IP}.
   */
  private static final class AutoEndpointTypeResolver implements EndpointTypeResolver {

    static final AutoEndpointTypeResolver INSTANCE = new AutoEndpointTypeResolver();

    @Override
    public EndpointType getEndpointType(SocketAddress remoteAddress, boolean sslEnabled) {
      if (isPrivateIp(remoteAddress)) {
        return sslEnabled ? EndpointType.INTERNAL_FQDN : EndpointType.INTERNAL_IP;
      }
      return sslEnabled ? EndpointType.EXTERNAL_FQDN : EndpointType.EXTERNAL_IP;
    }

    @Override
    public String toString() {
      return "AutoEndpointTypeResolver";
    }
  }

  /** Always requests the user-chosen endpoint type, ignoring connection characteristics. */
  private static final class FixedEndpointTypeResolver implements EndpointTypeResolver {

    private final EndpointType endpointType;

    FixedEndpointTypeResolver(EndpointType endpointType) {
      this.endpointType = endpointType;
    }

    @Override
    public EndpointType getEndpointType(SocketAddress remoteAddress, boolean sslEnabled) {
      return endpointType;
    }

    @Override
    public String toString() {
      return "FixedEndpointTypeResolver(" + endpointType + ")";
    }
  }

  /**
   * True if the address is a resolved private IP: loopback, link-local, site-local, or IPv6
   * unique-local.
   */
  private static boolean isPrivateIp(SocketAddress socketAddress) {
    if (!(socketAddress instanceof InetSocketAddress)) {
      return false;
    }

    InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
    if (address == null || address.isAnyLocalAddress()) {
      return false;
    }

    return address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || isUniqueLocalAddress(address);
  }

  // https://datatracker.ietf.org/doc/html/rfc4193
  private static boolean isUniqueLocalAddress(InetAddress address) {
    if (!(address instanceof Inet6Address)) {
      return false;
    }
    byte[] bytes = address.getAddress();
    return (bytes[0] & (byte) 0xfe) == (byte) 0xfc; // fc00::/7
  }

  /**
   * Mode for maintenance event notifications.
   * <ul>
   * <li>ENABLED - the handshake must succeed: connection setup fails if the server rejects
   * {@code CLIENT MAINT_NOTIFICATIONS} or the connection is not RESP3.</li>
   * <li>DISABLED - the handshake is not attempted; the feature is off.</li>
   * <li>AUTO - the handshake is attempted on RESP3 connections and the feature is quietly disabled
   * if the server rejects it (or the connection is not RESP3).</li>
   * </ul>
   * @since 8.1
   */
  public enum Mode {
    ENABLED, DISABLED, AUTO
  }

  private final EndpointTypeResolver endpointTypeResolver;
  private final Mode mode;
  private final Duration relaxedWindowMaxDuration;
  private final int relaxedTimeout;
  private final int relaxedBlockingTimeout;

  /**
   * The strategy that decides which endpoint type to request in MOVING notifications; defaults to
   * auto-resolution from connection characteristics.
   * @since 8.1
   */
  public EndpointTypeResolver getEndpointTypeResolver() {
    return endpointTypeResolver;
  }

  /**
   * The configured mode.
   * @since 8.1
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Upper bound on the relaxed-timeout window started by MIGRATING/FAILING_OVER. The window reverts
   * automatically after this duration even if the matching closing notification is never received.
   * Safety net against missed events or misbehaving servers. MOVING windows end at their own grace
   * period instead.
   * @since 8.1
   */
  public Duration getRelaxedWindowMaxDuration() {
    return relaxedWindowMaxDuration;
  }

  /**
   * True when the client will attempt the maintenance-notifications handshake (mode ENABLED or
   * AUTO); with AUTO the feature still depends on server support.
   * @since 8.1
   */
  public boolean isEnabledOrAuto() {
    return mode == Mode.ENABLED || mode == Mode.AUTO;
  }

  /**
   * Relaxed socket timeout in milliseconds applied while a maintenance window is open. The
   * connection uses the looser of this value and the configured socket timeout ({@code 0} =
   * infinite is the loosest), so relaxation never tightens a timeout.
   * @since 8.1
   */
  public int getRelaxedTimeout() {
    return relaxedTimeout;
  }

  /**
   * Relaxed blocking socket timeout in milliseconds; {@code 0} means infinite. The connection uses
   * the looser of this value and the configured blocking socket timeout, so relaxation never
   * tightens a timeout.
   * @since 8.1
   */
  public int getRelaxedBlockingTimeout() {
    return relaxedBlockingTimeout;
  }

  /**
   * A builder preloaded with the defaults: {@link Mode#AUTO}, auto-resolved endpoint type, relaxed
   * timeouts {@link #DEFAULT_RELAXED_SOCKET_TIMEOUT_MS} /
   * {@link #DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS}.
   * @since 8.1
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Maintenance notifications disabled. To enable — optionally with custom relaxed timeouts — use
   * {@link #builder()} with mode {@link Mode#AUTO} or {@link Mode#ENABLED}.
   * @since 8.1
   */
  public static final MaintenanceNotificationsConfig DISABLED = builder().mode(Mode.DISABLED)
      .build();

  /**
   * Builder for {@link MaintenanceNotificationsConfig}.
   * @since 8.1
   */
  public static class Builder {
    private EndpointTypeResolver endpointTypeResolver = AutoEndpointTypeResolver.INSTANCE;
    private Mode mode = Mode.AUTO;
    private Duration relaxedWindowMaxDuration = DEFAULT_RELAXED_WINDOW_MAX_DURATION;
    private int relaxedTimeout = DEFAULT_RELAXED_SOCKET_TIMEOUT_MS;
    private int relaxedBlockingTimeout = DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS;

    /**
     * Requests a fixed endpoint type for all MOVING notifications. Mutually exclusive with
     * {@link #autoResolveEndpointType()}; the last call wins.
     * @since 8.1
     */
    public Builder endpointType(EndpointType endpointType) {
      JedisAsserts.notNull(endpointType, "endpointType must not be null");
      this.endpointTypeResolver = new FixedEndpointTypeResolver(endpointType);
      return this;
    }

    /**
     * Requests the endpoint type resolved per connection from its characteristics: private remote
     * IP selects {@code INTERNAL_*}, public {@code EXTERNAL_*}; TLS selects {@code *_FQDN},
     * plaintext {@code *_IP}. This is the default. Mutually exclusive with
     * {@link #endpointType(EndpointType)}; the last call wins.
     * @since 8.1
     */
    public Builder autoResolveEndpointType() {
      this.endpointTypeResolver = AutoEndpointTypeResolver.INSTANCE;
      return this;
    }

    /**
     * The notifications mode; defaults to {@link Mode#AUTO}.
     * @since 8.1
     */
    public Builder mode(Mode mode) {
      this.mode = mode;
      return this;
    }

    /**
     * Socket timeout in milliseconds applied while a maintenance window is open; the connection
     * uses the looser of this value and the configured socket timeout. Defaults to
     * {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_SOCKET_TIMEOUT_MS}.
     * @since 8.1
     */
    public Builder relaxedTimeout(int millis) {
      this.relaxedTimeout = millis;
      return this;
    }

    /**
     * Blocking-command socket timeout in milliseconds applied while a maintenance window is open;
     * {@code 0} means infinite. The connection uses the looser of this value and the configured
     * blocking socket timeout. Defaults to
     * {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS}.
     * @since 8.1
     */
    public Builder relaxedBlockingTimeout(int millis) {
      this.relaxedBlockingTimeout = millis;
      return this;
    }

    /**
     * Upper bound on relaxation triggered by MIGRATING/FAILING_OVER. Acts as a safety net: the
     * relaxed window reverts after this duration even if the matching closing notification is lost.
     * Defaults to {@link MaintenanceNotificationsConfig#DEFAULT_RELAXED_WINDOW_MAX_DURATION}.
     * @since 8.1
     */
    public Builder relaxedWindowMaxDuration(Duration duration) {
      JedisAsserts.notNull(duration, "duration must not be null");
      this.relaxedWindowMaxDuration = duration;
      return this;
    }

    /**
     * Builds the immutable configuration.
     * @since 8.1
     */
    public MaintenanceNotificationsConfig build() {
      return new MaintenanceNotificationsConfig(this);
    }
  }

}
