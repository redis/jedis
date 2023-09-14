package redis.clients.jedis;

public final class ClientSetInfoConfig {

  private final boolean disabled;

  private final String libNameSuffix;

  public ClientSetInfoConfig() {
    this(false, null);
  }

  public ClientSetInfoConfig(boolean disabled) {
    this(disabled, null);
  }

  public ClientSetInfoConfig(String libNameSuffix) {
    this(false, libNameSuffix);
  }

  private ClientSetInfoConfig(boolean disabled, String libNameSuffix) {
    this.disabled = disabled;
    this.libNameSuffix = libNameSuffix;
  }

  public final boolean isDisabled() {
    return disabled;
  }

  public final String getLibNameSuffix() {
    return libNameSuffix;
  }

  public static final ClientSetInfoConfig DEFAULT = new ClientSetInfoConfig();

  public static final ClientSetInfoConfig DISABLED = new ClientSetInfoConfig(true);

  public static ClientSetInfoConfig withLibNameSuffix(String suffix) {
    return new ClientSetInfoConfig(suffix);
  }
}
