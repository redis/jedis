package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisConnection;

public class SimpleJedisConnectionProvider implements JedisConnectionProvider {

  private final JedisConnection connection;

  public SimpleJedisConnectionProvider(HostAndPort hap) {
    this(hap, DefaultJedisClientConfig.builder().build());
  }

  public SimpleJedisConnectionProvider(HostAndPort hap, JedisClientConfig config) {
    this.connection = new JedisConnection(hap, config);
  }

  public SimpleJedisConnectionProvider(JedisConnection connection) {
    this.connection = connection;
  }

  @Override
  public void close() {
    connection.disconnect();
  }

  @Override
  public JedisConnection getConnection(CommandArguments args) {
    return connection;
  }

}
