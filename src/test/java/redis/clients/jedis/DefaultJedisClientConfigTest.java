package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultJedisClientConfigTest {

  @Nested
  class BuilderTests {

    @Test
    void builderFromUri_credentials() {
      URI uri = URI.create("redis://testuser:testpass@localhost:6379/0");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      assertThat(config.getUser(), equalTo("testuser"));
      assertThat(config.getPassword(), equalTo("testpass"));
      assertThat(config.getDatabase(), equalTo(0));
    }

    @Test
    void builderFromUri_database() {
      URI uri = URI.create("redis://localhost:6379/5");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      assertThat(config.getDatabase(), equalTo(5));
    }

    @Test
    void builderFromUri_ssl() {
      URI uri = URI.create("rediss://localhost:6380");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      assertThat(config.isSsl(), equalTo(true));
    }

    @Test
    void builderFromUri_protocol() {
      URI uri = URI.create("redis://localhost:6379?protocol=3");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      assertThat(config.getRedisProtocol(), equalTo(RedisProtocol.RESP3));
    }

    @Test
    void builderFromUri_combined() {
      URI uri = URI.create("rediss://admin:secret123@localhost:6380/2?protocol=3");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      assertThat(config.getUser(), equalTo("admin"));
      assertThat(config.getPassword(), equalTo("secret123"));
      assertThat(config.getDatabase(), equalTo(2));
      assertThat(config.isSsl(), equalTo(true));
      assertThat(config.getRedisProtocol(), equalTo(RedisProtocol.RESP3));
    }

    @Test
    void builderFromUri_noCredentials() {
      URI uri = URI.create("redis://localhost:6379");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      // Should have default/null credentials
      assertThat(config.getDatabase(), equalTo(Protocol.DEFAULT_DATABASE));
      assertThat(config.isSsl(), equalTo(false));
    }

    @Test
    void builderFromUri_onlyUsername() {
      URI uri = URI.create("redis://onlyuser@localhost:6379");

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder(uri).build();

      assertThat(config.getUser(), equalTo("onlyuser"));
      // Password should be null when not provided
    }
  }
}
