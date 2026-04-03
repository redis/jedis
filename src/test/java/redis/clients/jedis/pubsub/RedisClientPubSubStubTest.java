package redis.clients.jedis.pubsub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.server.stub.RedisServerStub;

/**
 * Mock tests for RedisClient pub/sub functionality using RedisServerStub.
 * <p>
 * These tests run against RedisServerStub (mock Redis) **ONLY with RESP3** since
 * RedisServerStub only supports RESP3 protocol.
 * </p>
 * <p>
 * RedisServerStub allows testing scenarios that are difficult to simulate with a real Redis server,
 * such as arbitrary push notifications sent during subscribed connections, connection failures,
 * and timing-sensitive behaviors.
 * </p>
 */
public class RedisClientPubSubStubTest {

  private static RedisServerStub serverStub;

  @BeforeAll
  public static void setUpAll() throws Exception {
    serverStub = new RedisServerStub();
    serverStub.start();
  }

  @AfterAll
  public static void tearDownAll() throws Exception {
    if (serverStub != null) {
      serverStub.stop();
    }
  }

  /**
   * RESP3 tests - RedisServerStub only supports RESP3.
   */
  @Nested
  public class Resp3Tests extends RedisClientPubSubTestBase {

    @Override
    protected RedisClient createClient(RedisProtocol protocol) {
      return RedisClient.builder()
          .hostAndPort(new HostAndPort("localhost", serverStub.getPort()))
          .clientConfig(DefaultJedisClientConfig.builder()
              .protocol(protocol)
              .build())
          .build();
    }

    @Override
    protected RedisProtocol getProtocol() {
      return RedisProtocol.RESP3;
    }
  }
}

