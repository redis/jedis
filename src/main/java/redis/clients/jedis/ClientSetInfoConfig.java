package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisValidationException;

/**
 * Configuration for CLIENT SETINFO command behaviour.
 * <p>
 * This class supports two modes of operation:
 * <ul>
 * <li>Legacy mode: Using {@link #withLibNameSuffix(String)} for simple suffix customization</li>
 * <li>Driver info mode: Using {@link #ClientSetInfoConfig(DriverInfo)} for structured driver information
 * with upstream drivers</li>
 * </ul>
 * <p>
 * For backward compatibility, {@link #getUpstreamDrivers()} returns the upstream drivers string when
 * using driver info mode.
 *
 * @see DriverInfo
 * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
 */
public final class ClientSetInfoConfig {

  private final boolean disabled;

  private final DriverInfo driverInfo;

  /**
   * Creates a new ClientSetInfoConfig with default settings.
   * <p>
   * The default configuration uses the "jedis" library name without any upstream drivers.
   */
  public ClientSetInfoConfig() {
    this(false);
  }

  /**
   * Creates a new ClientSetInfoConfig with the specified disabled state.
   * <p>
   * When disabled, the CLIENT SETINFO command will not be sent to Redis.
   *
   * @param disabled {@code true} to disable CLIENT SETINFO, {@code false} otherwise
   */
  public ClientSetInfoConfig(boolean disabled) {
    this.disabled = disabled;
    this.driverInfo = DriverInfo.builder().build();
  }

  /**
   * Creates a new ClientSetInfoConfig with a library name suffix.
   * <p>
   * This constructor is for legacy compatibility. The suffix will be appended to "jedis" in parentheses,
   * resulting in a format like: {@code jedis(suffix)}.
   * <p>
   * For adding upstream driver information, use {@link #ClientSetInfoConfig(DriverInfo)} with a
   * {@link DriverInfo} that has upstream drivers.
   *
   * @param libNameSuffix the suffix to append to "jedis" (will be placed in parentheses)
   * @throws JedisValidationException if libNameSuffix contains braces
   */
  public ClientSetInfoConfig(String libNameSuffix) {
    this.disabled = false;
    this.driverInfo = DriverInfo.builder().addUpstreamDriver(libNameSuffix).build();
  }

  /**
   * Creates a new ClientSetInfoConfig with the specified driver information.
   * <p>
   * This is the recommended constructor for setting up driver information with upstream drivers.
   * The driver information can optionally override the library name completely.
   *
   * @param driverInfo the driver information, must not be {@code null}
   * @throws JedisValidationException if driverInfo is {@code null}
   */
  public ClientSetInfoConfig(DriverInfo driverInfo) {
    if (driverInfo == null) {
      throw new JedisValidationException("DriverInfo must not be null");
    }
    this.disabled = false;
    this.driverInfo = driverInfo;
  }

  /**
   * Returns whether CLIENT SETINFO is disabled.
   *
   * @return {@code true} if CLIENT SETINFO is disabled, {@code false} otherwise
   */
  public boolean isDisabled() {
    return disabled;
  }

  /**
   * Returns the driver information.
   *
   * @return the driver information
   */
  public DriverInfo getDriverInfo() {
    return driverInfo;
  }

  /**
   * Returns the formatted upstream drivers string.
   * <p>
   * Multiple drivers are separated by semicolons, with the most recently added driver appearing first.
   * <p>
   * Examples:
   * <ul>
   * <li>{@code "spring-data-redis_v3.2.0"} - single upstream driver</li>
   * <li>{@code "lettuce-core_v6.4.1;spring-data-redis_v3.2.0"} - multiple upstream drivers</li>
   * </ul>
   *
   * @return the formatted upstream drivers string, or {@code null} if no upstream drivers are set
   */
  public String getUpstreamDrivers() {
    return driverInfo.getUpstreamDrivers();
  }

  /**
   * Default configuration that uses the Jedis library name without any upstream drivers.
   */
  public static final ClientSetInfoConfig DEFAULT = new ClientSetInfoConfig();

  /**
   * Configuration that disables CLIENT SETINFO command.
   */
  public static final ClientSetInfoConfig DISABLED = new ClientSetInfoConfig(true);

  /**
   * Creates a new ClientSetInfoConfig with a library name suffix.
   * <p>
   * This is the legacy method for simple name customization. The provided suffix will be appended
   * to "jedis" in parentheses, resulting in a format like: {@code jedis(suffix)}.
   * For adding upstream driver information, use {@link #ClientSetInfoConfig(DriverInfo)} with a
   *    * {@link DriverInfo} that has upstream drivers.
   *
   * @param suffix the suffix to append to "jedis" (will be placed in parentheses)
   * @return a new ClientSetInfoConfig with the library name suffix
   */
  public static ClientSetInfoConfig withLibNameSuffix(String suffix) {
    return new ClientSetInfoConfig(suffix);
  }
}
