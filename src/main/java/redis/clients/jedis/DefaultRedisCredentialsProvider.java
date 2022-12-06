package redis.clients.jedis;

class DefaultRedisCredentialsProvider implements RedisCredentialsProvider {

  private RedisCredentials credentials;

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

  @Override
  public void prepare() {
  }

  @Override
  public void cleanUp() {
  }
}
