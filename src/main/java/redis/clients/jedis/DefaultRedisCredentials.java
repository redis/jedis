package redis.clients.jedis;

class DefaultRedisCredentials implements RedisCredentials {

  private final String user;
  private final char[] password;

  DefaultRedisCredentials(String user, char[] password) {
    this.user = user;
    this.password = password;
  }

  DefaultRedisCredentials(String user, String password) {
    this.user = user;
    this.password = password == null ? null : password.toCharArray();
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public char[] getPassword() {
    return password;
  }
}
