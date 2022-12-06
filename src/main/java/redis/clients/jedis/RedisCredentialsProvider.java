package redis.clients.jedis;

import java.util.function.Supplier;

public interface RedisCredentialsProvider extends Supplier<RedisCredentials> {

  void prepare();

  void cleanUp();
}
