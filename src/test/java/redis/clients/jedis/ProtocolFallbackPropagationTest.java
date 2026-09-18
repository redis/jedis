package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Verifies that the RESP protocol established by a {@link Connection} (returned by
 * {@link Connection#getRedisProtocol()}) is correctly propagated to the {@link CommandObjects} used
 * by {@link RedisClient}, {@link RedisClusterClient}, {@link UnifiedJedis}, {@link Pipeline},
 * {@link ClusterPipeline}, and to {@link JedisPubSubBase}.
 * <p>
 * Tests in this class do <em>not</em> exercise the handshake itself — that is covered by
 * {@link ConnectionHelloAuthTest} (wire-level) and {@link ProtocolHandshakeTest} (state-machine).
 * Here {@link Connection} is fully mocked and only the propagation chain is under test.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ProtocolFallbackPropagationTest {

  @Mock
  CommandExecutor exec;
  @Mock
  ConnectionProvider provider;

  // ---------------------------------------------------------------------------
  // RedisClient
  // ---------------------------------------------------------------------------

  /**
   * When RedisClient is built with auto-negotiated protocol, the constructor probes a Connection
   * from the provider to resolve the actual protocol. If the Connection fell back to RESP2 (server
   * does not support HELLO), CommandObjects should get RESP2.
   */
  @Test
  void redisClientResolvesAutoNegotiatedToResp2WhenConnectionFellBack() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(null).build()).build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

      assertEquals(RedisProtocol.RESP2, protocol,
        "CommandObjects should receive RESP2 resolved from Connection that fell back");
    }
  }

  /**
   * When RedisClient is built with auto-negotiated protocol and the Connection negotiated RESP3,
   * CommandObjects should get RESP3.
   */
  @Test
  void redisClientResolvesAutoNegotiatedToResp3WhenConnectionSucceeded() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider).clientConfig(DefaultJedisClientConfig.builder().build())
        .build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

      assertEquals(RedisProtocol.RESP3, protocol,
        "CommandObjects should receive RESP3 resolved from Connection that negotiated RESP3");
    }
  }

  // ---------------------------------------------------------------------------
  // RedisClusterClient
  // ---------------------------------------------------------------------------

  /**
   * When RedisClusterClient is built with auto-negotiated protocol and the Connection fell back to
   * RESP2, ClusterCommandObjects should get RESP2.
   */
  @Test
  void redisClusterClientResolvesAutoNegotiatedToResp2WhenConnectionFellBack() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379))).commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(null).build()).build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertInstanceOf(ClusterCommandObjects.class, commandObjects,
        "RedisClusterClient should use ClusterCommandObjects");

      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");
      assertEquals(RedisProtocol.RESP2, protocol,
        "ClusterCommandObjects should receive RESP2 resolved from Connection that fell back");
    }
  }

  @Test
  void redisClusterClientResolvesAutoNegotiatedToResp3WhenConnectionSucceeded() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379))).commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(null).build()).build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertInstanceOf(ClusterCommandObjects.class, commandObjects);

      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");
      assertEquals(RedisProtocol.RESP3, protocol,
        "ClusterCommandObjects should receive RESP3 resolved from Connection that negotiated RESP3");
    }
  }

  // ---------------------------------------------------------------------------
  // UnifiedJedis(Connection)
  // ---------------------------------------------------------------------------

  @Test
  @SuppressWarnings("deprecation")
  void unifiedJedisFromConnectionPropagatesFallenBackProtocol() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);

    try (UnifiedJedis jedis = new UnifiedJedis(mockConnection)) {
      CommandObjects commandObjects = ReflectionTestUtil.getField(jedis, "commandObjects");
      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

      assertEquals(RedisProtocol.RESP2, protocol,
        "CommandObjects should receive RESP2 from the Connection that fell back");
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  void unifiedJedisFromConnectionPropagatesResp3Protocol() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    try (UnifiedJedis jedis = new UnifiedJedis(mockConnection)) {
      CommandObjects commandObjects = ReflectionTestUtil.getField(jedis, "commandObjects");
      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

      assertEquals(RedisProtocol.RESP3, protocol,
        "CommandObjects should receive RESP3 from the Connection that negotiated RESP3");
    }
  }

  // ---------------------------------------------------------------------------
  // Pipeline
  // ---------------------------------------------------------------------------

  @Test
  void pipelineFromResp3ConnectionHasResp3Protocol() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    Pipeline pipeline = new Pipeline(mockConnection);
    CommandObjects commandObjects = ReflectionTestUtil.getField(pipeline, "commandObjects");
    RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

    assertEquals(RedisProtocol.RESP3, protocol,
      "Pipeline's CommandObjects should have RESP3 when Connection negotiated RESP3");
  }

  @Test
  void redisClientPipelinedInheritsResp3Protocol() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(null).build()).build()) {

      CommandObjects clientCommandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertEquals(RedisProtocol.RESP3,
        ReflectionTestUtil.getField(clientCommandObjects, "protocol"));

      Pipeline pipeline = client.pipelined();
      CommandObjects pipelineCommandObjects = ReflectionTestUtil.getField(pipeline,
        "commandObjects");

      assertSame(clientCommandObjects, pipelineCommandObjects,
        "Pipeline should share the same CommandObjects instance as the client");
      assertEquals(RedisProtocol.RESP3,
        ReflectionTestUtil.getField(pipelineCommandObjects, "protocol"),
        "Pipeline's CommandObjects should have RESP3 inherited from the client");
    }
  }

  // ---------------------------------------------------------------------------
  // ClusterPipeline
  // ---------------------------------------------------------------------------

  @Test
  void clusterPipelineResolvesAutoNegotiatedToResp3() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    ClusterConnectionProvider clusterProvider = mock(ClusterConnectionProvider.class);
    when(clusterProvider.getConnection()).thenReturn(mockConnection);

    ClusterPipeline pipeline = new ClusterPipeline(clusterProvider, (RedisProtocol) null);

    CommandObjects commandObjects = ReflectionTestUtil.getField(pipeline, "commandObjects");
    assertInstanceOf(ClusterCommandObjects.class, commandObjects,
      "ClusterPipeline should use ClusterCommandObjects");

    RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");
    assertEquals(RedisProtocol.RESP3, protocol,
      "ClusterPipeline's ClusterCommandObjects should have RESP3 when Connection negotiated RESP3");
  }

  @Test
  void redisClusterClientPipelinedInheritsResp3Protocol() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    ClusterConnectionProvider clusterProvider = mock(ClusterConnectionProvider.class);
    when(clusterProvider.getConnection()).thenReturn(mockConnection);

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379))).commandExecutor(exec)
        .connectionProvider(clusterProvider)
        .clientConfig(DefaultJedisClientConfig.builder().protocol(null).build()).build()) {

      CommandObjects clientCommandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertInstanceOf(ClusterCommandObjects.class, clientCommandObjects);
      assertEquals(RedisProtocol.RESP3,
        ReflectionTestUtil.getField(clientCommandObjects, "protocol"));

      ClusterPipeline pipeline = client.pipelined();
      CommandObjects pipelineCommandObjects = ReflectionTestUtil.getField(pipeline,
        "commandObjects");

      assertSame(clientCommandObjects, pipelineCommandObjects,
        "ClusterPipeline should share the same ClusterCommandObjects instance as the client");
      assertEquals(RedisProtocol.RESP3,
        ReflectionTestUtil.getField(pipelineCommandObjects, "protocol"),
        "ClusterPipeline's ClusterCommandObjects should have RESP3 inherited from the client");
    }
  }

  // ---------------------------------------------------------------------------
  // JedisPubSubBase
  // ---------------------------------------------------------------------------

  @Test
  void pubSubAllowsSubscribeWhenConnectionHasResp3WithTokenAuth() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    JedisPubSubBase<String> pubSub = new JedisPubSub() {
    };

    JedisSafeAuthenticator authenticator = ReflectionTestUtil.getField(pubSub, "authenticator");
    ReflectionTestUtil.setField(authenticator, "client", mockConnection);

    assertDoesNotThrow(() -> pubSub.subscribe("testchannel"));
  }

  @Test
  void pubSubRejectsSubscribeWhenConnectionHasResp2WithTokenAuth() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);
    when(mockConnection.isTokenBasedAuthenticationEnabled()).thenReturn(true);

    JedisPubSubBase<String> pubSub = new JedisPubSub() {
    };

    JedisSafeAuthenticator authenticator = ReflectionTestUtil.getField(pubSub, "authenticator");
    ReflectionTestUtil.setField(authenticator, "client", mockConnection);

    JedisException ex = assertThrows(JedisException.class, () -> pubSub.subscribe("testchannel"));
    assertTrue(ex.getMessage().contains("Blocking pub/sub operations are not supported"),
      "RESP2 connection with token-based auth should be rejected for pub/sub");
  }
}
