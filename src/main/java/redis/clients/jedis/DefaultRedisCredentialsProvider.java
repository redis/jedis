package redis.clients.jedis;

class DefaultRedisCredentialsProvider implements RedisCredentialsProvider {

  private volatile RedisCredentials credentials;

  DefaultRedisCredentialsProvider(RedisCredentials credentials) {
    this.credentials = credentials;
  }

  void setCredentials(RedisCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public RedisCredentials get() {
    return this.credentials;
  }
}
