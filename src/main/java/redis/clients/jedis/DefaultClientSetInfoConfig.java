package redis.clients.jedis;

public final class DefaultClientSetInfoConfig implements ClientSetInfoConfig {

  private final boolean disabled;

  private final String libNameSuffix;

  private DefaultClientSetInfoConfig(boolean disabled, String libNameSuffix) {
    this.disabled = disabled;
    this.libNameSuffix = libNameSuffix;
  }

  @Override
  public boolean isDisabled() {
    return disabled;
  }

  @Override
  public String getLibNameSuffix() {
    return libNameSuffix;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean disable = false;

    private String libNameSuffix = null;

    private Builder() {
    }

    public DefaultClientSetInfoConfig build() {
      if (disable) {
        if (libNameSuffix != null) {
          throw new IllegalArgumentException("libNameSuffix cannot be used when internal "
              + "CLIENT SETINFO command is disabled.");
        }
      }

      return new DefaultClientSetInfoConfig(disable, libNameSuffix);
    }

    public Builder disable() {
      return disable(true);
    }

    public Builder disable(boolean disable) {
      this.disable = disable;
      return this;
    }

    public Builder libNameSuffix(String suffix) {
      this.libNameSuffix = suffix;
      return this;
    }
  }
}
