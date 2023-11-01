package redis.clients.jedis;

public final class DefaultRedisCredentials implements RedisCredentials {

  private final String user;
  private final char[] password;

  public DefaultRedisCredentials(String user, char[] password) {
    this.user = user;
    this.password = password;
  }

  public DefaultRedisCredentials(String user, CharSequence password) {
    this.user = user;
    this.password = password == null ? null
        : password instanceof String ? ((String) password).toCharArray()
            : toCharArray(password);
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public char[] getPassword() {
    return password;
  }

  private static char[] toCharArray(CharSequence seq) {
    final int len = seq.length();
    char[] arr = new char[len];
    for (int i = 0; i < len; i++) {
      arr[i] = seq.charAt(i);
    }
    return arr;
  }
}
