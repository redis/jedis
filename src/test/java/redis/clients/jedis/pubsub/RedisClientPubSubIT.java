package redis.clients.jedis.pubsub;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;

/**
 * Integration tests for RedisClient pub/sub functionality.
 * <p>
 * These tests run against a real Redis server. Tests are organized in nested classes for RESP2 and
 * RESP3 protocols.
 * </p>
 */
@Tag("integration")
public class RedisClientPubSubIT {

  private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");

  /**
   * RESP2 protocol tests.
   */
  @Nested
  public class Resp2Tests extends RedisClientPubSubTestBase {

    @Override
    protected RedisClient createClient(RedisProtocol protocol) {
      return RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
          .clientConfig(endpoint.getClientConfigBuilder().protocol(protocol).build()).build();
    }

    @Override
    protected RedisProtocol getProtocol() {
      return RedisProtocol.RESP2;
    }
  }

  /**
   * RESP3 protocol tests.
   */
  @Nested
  public class Resp3Tests extends RedisClientPubSubTestBase {

    @Override
    protected RedisClient createClient(RedisProtocol protocol) {
      return RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
          .clientConfig(endpoint.getClientConfigBuilder().protocol(protocol).build()).build();
    }

    @Override
    protected RedisProtocol getProtocol() {
      return RedisProtocol.RESP3;
    }
  }
}
