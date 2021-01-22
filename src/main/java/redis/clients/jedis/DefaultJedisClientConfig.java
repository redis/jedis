package redis.clients.jedis;

public class DefaultJedisClientConfig implements JedisClientConfig {

  private final int infiniteSoTimeout;
  private final String user;
  private final String password;
  private final int database;
  private final String clientName;

  private DefaultJedisClientConfig(int infiniteSoTimeout,
      String user, String password, int database, String clientName) {
    this.infiniteSoTimeout = infiniteSoTimeout;
    this.user = user;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int getInfiniteSoTimeout() {
    return infiniteSoTimeout;
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public int getDatabase() {
    return database;
  }

  @Override
  public String getClientName() {
    return clientName;
  }

  public static class Builder {

    private int infiniteSoTimeout = 0;

    private String user = null;
    private String password = null;
    private int databse = Protocol.DEFAULT_DATABASE;
    private String clinetName = null;

    private Builder() {
    }

    public DefaultJedisClientConfig build() {
      return new DefaultJedisClientConfig(infiniteSoTimeout, user, password, databse, clinetName);
    }

    public Builder withInfiniteSoTimeout(int infiniteSoTimeout) {
      this.infiniteSoTimeout = infiniteSoTimeout;
      return this;
    }

    public Builder withUser(String user) {
      this.user = user;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder withDatabse(int databse) {
      this.databse = databse;
      return this;
    }

    public Builder withClinetName(String clinetName) {
      this.clinetName = clinetName;
      return this;
    }

    public int getInfiniteSoTimeout() {
      return infiniteSoTimeout;
    }

    public String getUser() {
      return user;
    }

    public String getPassword() {
      return password;
    }

    public int getDatabse() {
      return databse;
    }

    public String getClinetName() {
      return clinetName;
    }

  }
}
