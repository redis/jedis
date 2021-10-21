package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

public interface JedisConnectionProvider extends AutoCloseable {

  Connection getConnection(CommandArguments args);
}
