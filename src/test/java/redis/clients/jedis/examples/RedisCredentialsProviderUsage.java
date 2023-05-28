package redis.clients.jedis.examples;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.DefaultRedisCredentialsProvider;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

public class RedisCredentialsProviderUsage {

  public static void main(String[] args) {

    DefaultRedisCredentials initialCredentials
        = new DefaultRedisCredentials("<INITIAL_USERNAME>", "<INITIAL_PASSWORD>");

    DefaultRedisCredentialsProvider credentialsProvider
        = new DefaultRedisCredentialsProvider(initialCredentials);

    final String host = "<HOST>";
    final int port = 6379;
    final HostAndPort address = new HostAndPort(host, port);
    final DefaultJedisClientConfig clientConfig
        = DefaultJedisClientConfig.builder()
            .credentialsProvider(credentialsProvider).build();

    JedisPooled jedis = new JedisPooled(address, clientConfig);

    // ...
    // do operations with jedis
    // ...

    // when credentials is required to be updated
    DefaultRedisCredentials updatedCredentials
        = new DefaultRedisCredentials("<UPDATED_USERNAME>", "<UPDATED_PASSWORD>");

    credentialsProvider.setCredentials(updatedCredentials);

    // ...
    // continue doing operations with jedis
    // ...

    jedis.close();
  }
}
