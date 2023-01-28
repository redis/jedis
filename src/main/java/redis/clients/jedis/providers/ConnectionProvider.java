package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

public interface ConnectionProvider extends AutoCloseable {

  Connection getConnection();

  Connection getConnection(CommandArguments args);
}
