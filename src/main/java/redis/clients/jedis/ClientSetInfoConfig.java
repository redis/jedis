package redis.clients.jedis;

import java.util.Arrays;
import java.util.HashSet;
import redis.clients.jedis.exceptions.JedisValidationException;

public final class ClientSetInfoConfig {

  private final boolean disabled;

  private final String libNameSuffix;

  public ClientSetInfoConfig() {
    this(false, null);
  }

  public ClientSetInfoConfig(boolean disabled) {
    this(disabled, null);
  }

  /**
   * @param libNameSuffix must not have braces ({@code ()[]{}}) and spaces will be replaced with hyphens
   */
  public ClientSetInfoConfig(String libNameSuffix) {
    this(false, libNameSuffix);
  }

  private ClientSetInfoConfig(boolean disabled, String libNameSuffix) {
    this.disabled = disabled;
    this.libNameSuffix = validateLibNameSuffix(libNameSuffix);
  }

  private static final HashSet<Character> BRACES = new HashSet<>(Arrays.asList('(', ')', '[', ']', '{', '}'));

  private static String validateLibNameSuffix(String suffix) {
    if (suffix == null || suffix.trim().isEmpty()) {
      return null;
    }

    for (int i = 0; i < suffix.length(); i++) {
      char c = suffix.charAt(i);
      if (c < ' ' || c > '~' || BRACES.contains(c)) {
        throw new JedisValidationException("lib-name suffix cannot contain braces, newlines or "
            + "special characters.");
      }
    }

    return suffix.replaceAll("\\s", "-");
  }

  public final boolean isDisabled() {
    return disabled;
  }

  public final String getLibNameSuffix() {
    return libNameSuffix;
  }

  public static final ClientSetInfoConfig DEFAULT = new ClientSetInfoConfig();

  public static final ClientSetInfoConfig DISABLED = new ClientSetInfoConfig(true);

  /**
   * @param suffix must not have braces ({@code ()[]{}}) and spaces will be replaced with hyphens
   * @return config
   */
  public static ClientSetInfoConfig withLibNameSuffix(String suffix) {
    return new ClientSetInfoConfig(suffix);
  }
}
