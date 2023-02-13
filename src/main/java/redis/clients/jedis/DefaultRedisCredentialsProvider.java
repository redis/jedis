package redis.clients.jedis;

public final class DefaultRedisCredentialsProvider implements RedisCredentialsProvider {

  private volatile RedisCredentials credentials;

  public DefaultRedisCredentialsProvider(RedisCredentials credentials) {
    this.credentials = credentials;
  }

  public void setCredentials(RedisCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public RedisCredentials get() {
    return this.credentials;
  }
}
