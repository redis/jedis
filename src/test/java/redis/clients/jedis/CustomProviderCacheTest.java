package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;

class CustomProviderCacheTest {

  private CommandExecutor exec;

  @BeforeEach
  void setUp() {
    exec = new CommandExecutor() {
      @Override
      public <T> T executeCommand(CommandObject<T> commandObject) {
        return null;
      }

      @Override
      public void close() {
      }
    };
  }

  @Test
  void redisClientWithCustomProviderExposesCache() {
    Cache cache = CacheFactory.getCache(CacheConfig.builder().build());

    ConnectionProvider provider = new ConnectionProvider() {
      @Override
      public Connection getConnection() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Connection getConnection(CommandArguments args) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() {
      }

      @Override
      public Cache getCache() {
        return cache;
      }
    };

    try (RedisClient client = RedisClient.builder()
        .commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3).build())
        .build()) {

      assertNotNull(client.getCache(), "Cache should not be null when provider carries one");
      assertSame(cache, client.getCache(),
          "getCache() should return the same Cache instance the provider holds");
    }
  }

  @Test
  void multiDbClientWithCustomProviderExposesCache() {
    Cache cache = CacheFactory.getCache(CacheConfig.builder().build());

    ConnectionProvider provider = new ConnectionProvider() {
      @Override
      public Connection getConnection() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Connection getConnection(CommandArguments args) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() {
      }

      @Override
      public Cache getCache() {
        return cache;
      }
    };

    try (MultiDbClient client = MultiDbClient.builder()
        .commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3).build())
        .build()) {

      assertNotNull(client.getCache(), "Cache should not be null when provider carries one");
      assertSame(cache, client.getCache(),
          "getCache() should return the same Cache instance the provider holds");
    }
  }
}
