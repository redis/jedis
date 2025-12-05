package redis.clients.jedis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import redis.clients.jedis.exceptions.JedisValidationException;

/**
 * Immutable class representing driver information for Redis client identification.
 * <p>
 * This class is used to identify the client library and any upstream drivers (such as Spring Data
 * Redis or Spring Session) when connecting to Redis. The information is sent via the
 * {@code CLIENT SETINFO} command.
 * <p>
 * The formatted name follows the pattern: {@code name(driver1_vVersion1;driver2_vVersion2)}
 * @see ClientSetInfoConfig
 * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
 */
public final class DriverInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Regex pattern for driver name validation. The name must start with a lowercase letter and
   * contain only lowercase letters, digits, hyphens, and underscores. Dots are only allowed after
   * digits (for Scala cross-version naming like akka-redis_2.13). Follows Maven artifactId naming
   * conventions but also allows underscores and version-specific dots.
   * @see <a href="https://maven.apache.org/guides/mini/guide-naming-conventions.html">Maven Naming
   *      Conventions</a>
   */
  private static final String DRIVER_NAME_PATTERN = "^[a-z][a-z0-9_-]*(?:[0-9]\\.[0-9]+)?$";

  /**
   * Set of brace characters that are not allowed in driver names or versions. These characters are
   * used to delimit the driver information in the formatted output and would break parsing.
   */
  private static final Set<Character> BRACES = Collections
      .unmodifiableSet(new HashSet<>(Arrays.asList('(', ')', '[', ']', '{', '}')));

  private final String name;

  private final List<String> upstreamDrivers;

  private DriverInfo(String name, List<String> upstreamDrivers) {
    this.name = name;
    this.upstreamDrivers = Collections.unmodifiableList(upstreamDrivers);
  }

  /**
   * Creates a new {@link Builder} with default values.
   * <p>
   * The default name is "Jedis" (from {@link JedisMetaInfo#getArtifactId()}).
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new {@link Builder} initialized with values from an existing {@link DriverInfo}.
   * @param driverInfo the existing driver info to copy from, must not be {@code null}
   * @return a new builder instance initialized with the existing values
   * @throws JedisValidationException if driverInfo is {@code null}
   */
  public static Builder builder(DriverInfo driverInfo) {
    if (driverInfo == null) {
      throw new JedisValidationException("DriverInfo must not be null");
    }
    return new Builder(driverInfo);
  }

  /**
   * Returns the formatted name including upstream drivers or legacy suffix.
   * <p>
   * If a legacy suffix is set, returns the name followed by the suffix in parentheses. Otherwise,
   * if upstream drivers are present, returns the name followed by upstream drivers in parentheses,
   * separated by semicolons. If neither is set, returns just the name.
   * <p>
   * Examples:
   * <ul>
   * <li>{@code "jedis"} - no upstream drivers or suffix</li>
   * <li>{@code "jedis(my-suffix)"} - legacy suffix mode</li>
   * <li>{@code "jedis(spring-data-redis_v3.2.0)"} - one upstream driver</li>
   * <li>{@code "jedis(spring-session_v3.3.0;spring-data-redis_v3.2.0)"} - multiple upstream
   * drivers</li>
   * </ul>
   * @return the formatted name for use in CLIENT SETINFO
   */
  public String getFormattedName() {
    if (upstreamDrivers.isEmpty()) {
      return name;
    }
    return String.format("%s(%s)", name, String.join(";", upstreamDrivers));
  }

  /**
   * Returns the base library name without upstream driver information.
   * @return the library name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the formatted upstream drivers string (without the base library name).
   * <p>
   * Multiple drivers are separated by semicolons, with the most recently added driver appearing
   * first.
   * <p>
   * Examples:
   * <ul>
   * <li>{@code "spring-data-redis_v3.2.0"} - single upstream driver</li>
   * <li>{@code "spring-session_v3.3.0;spring-data-redis_v3.2.0"} - multiple upstream drivers</li>
   * </ul>
   * @return the formatted upstream drivers string, or {@code null} if no upstream drivers are set
   */
  public String getUpstreamDrivers() {
    if (upstreamDrivers.isEmpty()) {
      return null;
    }
    return String.join(";", upstreamDrivers);
  }

  @Override
  public String toString() {
    return getFormattedName();
  }

  /**
   * Builder for creating {@link DriverInfo} instances.
   */
  public static class Builder {

    private String name;

    private final List<String> upstreamDrivers;

    private Builder() {
      this.name = JedisMetaInfo.getArtifactId();
      this.upstreamDrivers = new ArrayList<>();
    }

    private Builder(DriverInfo driverInfo) {
      this.name = driverInfo.name;
      this.upstreamDrivers = new ArrayList<>(driverInfo.upstreamDrivers);
    }

    /**
     * Sets the base library name.
     * <p>
     * This overrides the default name ("Jedis"). Use this when you want to completely customize the
     * library identification.
     * @param name the library name, must not be {@code null}
     * @return this builder
     * @throws JedisValidationException if name is {@code null}
     */
    public Builder name(String name) {
      if (name == null) {
        throw new JedisValidationException("Name must not be null");
      }
      this.name = name;
      return this;
    }

    /**
     * Adds an upstream driver to the driver information.
     * <p>
     * Upstream drivers are prepended to the list, so the most recently added driver appears first
     * in the formatted output.
     * <p>
     * The driver name must follow Maven artifactId naming conventions: lowercase letters, digits,
     * hyphens, and underscores only, starting with a lowercase letter. Dots are only allowed after
     * digits (for Scala cross-version naming like akka-redis_2.13).
     * <p>
     * Both values must not contain spaces, newlines, non-printable characters, or brace characters
     * as these would violate the format of the Redis CLIENT LIST reply.
     * @param driverName the name of the upstream driver (e.g., "spring-data-redis"), must not be
     *          {@code null}
     * @param driverVersion the version of the upstream driver (e.g., "3.2.0"), must not be
     *          {@code null}
     * @return this builder
     * @throws JedisValidationException if the driver name or version is {@code null} or has invalid
     *           format
     * @see <a href="https://maven.apache.org/guides/mini/guide-naming-conventions.html">Maven
     *      Naming Conventions</a>
     * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
     */
    public Builder addUpstreamDriver(String driverName, String driverVersion) {
      if (driverName == null) {
        throw new JedisValidationException("Driver name must not be null");
      }
      if (driverVersion == null) {
        throw new JedisValidationException("Driver version must not be null");
      }
      validateDriverName(driverName);
      validateDriverVersion(driverVersion);
      String formattedDriverInfo = formatDriverInfo(driverName, driverVersion);
      this.upstreamDrivers.add(0, formattedDriverInfo);
      return this;
    }

    public Builder addUpstreamDriver(String driverName) {
      if (driverName == null) {
        throw new JedisValidationException("Driver name must not be null");
      }
      validateDriverName(driverName);
      this.upstreamDrivers.add(0, driverName);
      return this;
    }

    /**
     * Builds and returns a new immutable {@link DriverInfo} instance.
     * @return a new DriverInfo instance
     */
    public DriverInfo build() {
      return new DriverInfo(name, upstreamDrivers);
    }
  }

  /**
   * Validates that the driver name follows Maven artifactId naming conventions: lowercase letters,
   * digits, hyphens, and underscores only, starting with a lowercase letter. Dots are only allowed
   * after digits (for Scala cross-version naming like akka-redis_2.13).
   * <p>
   * Additionally validates Redis CLIENT LIST constraints: no spaces, newlines, non-printable
   * characters, or braces.
   * @param driverName the driver name to validate
   * @throws JedisValidationException if the driver name does not follow the expected naming
   *           conventions
   * @see <a href="https://maven.apache.org/guides/mini/guide-naming-conventions.html">Maven Naming
   *      Conventions</a>
   * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
   */
  private static void validateDriverName(String driverName) {
    if (driverName.trim().isEmpty()) {
      throw new JedisValidationException("Driver name must not be empty");
    }

    validateNoInvalidCharacters(driverName, "Driver name");

    if (!driverName.matches(DRIVER_NAME_PATTERN)) {
      throw new JedisValidationException(
          "Upstream driver name must follow Maven artifactId naming conventions: "
              + "lowercase letters, digits, hyphens, and underscores only, starting with a lowercase letter "
              + "(e.g., 'spring-data-redis', 'lettuce-core', 'akka-redis_2.13')");
    }
  }

  /**
   * Validates that the driver version does not contain characters that would violate the format of
   * the Redis CLIENT LIST reply: no spaces, newlines, non-printable characters, or brace
   * characters.
   * @param driverVersion the driver version to validate
   * @throws JedisValidationException if the driver version contains invalid characters
   * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
   */
  private static void validateDriverVersion(String driverVersion) {
    if (driverVersion.trim().isEmpty()) {
      throw new JedisValidationException("Driver version must not be empty");
    }

    validateNoInvalidCharacters(driverVersion, "Driver version");
  }

  /**
   * Validates that the value does not contain characters that would violate the format of the Redis
   * CLIENT LIST reply: non-printable characters, spaces, or brace characters.
   * <p>
   * Only printable ASCII characters (0x21-0x7E, i.e., '!' to '~') are allowed, excluding braces.
   * @param value the value to validate
   * @param fieldName the name of the field for error messages
   * @throws JedisValidationException if the value contains invalid characters
   * @see <a href="https://redis.io/docs/latest/commands/client-setinfo/">CLIENT SETINFO</a>
   */
  private static void validateNoInvalidCharacters(String value, String fieldName) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < '!' || c > '~' || BRACES.contains(c)) {
        throw new JedisValidationException(
            fieldName + " must not contain spaces, newlines, non-printable characters, or braces");
      }
    }
  }

  private static String formatDriverInfo(String driverName, String driverVersion) {
    return driverName + "_v" + driverVersion;
  }
}
