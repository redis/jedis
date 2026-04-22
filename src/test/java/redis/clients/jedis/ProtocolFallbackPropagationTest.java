package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.Socket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Mocked tests verifying that when Connection#helloAndAuth connects to a server that does not
 * support the HELLO command, the negotiated protocol is correctly propagated to
 * {@link CommandObjects} in {@link RedisClient} and to {@link ClusterCommandObjects} in
 * {@link RedisClusterClient}.
 */
@ExtendWith(MockitoExtension.class)
class ProtocolFallbackPropagationTest {

  @Mock
  CommandExecutor exec;
  @Mock
  ConnectionProvider provider;

  // ---------------------------------------------------------------------------
  // Connection-level tests: verify helloAndAuth fallback sets protocol to RESP2
  // ---------------------------------------------------------------------------

  /**
   * When RESP3_PREFERRED is configured and the server does not support HELLO (no user credentials
   * path), Connection should fall back to RESP2.
   */
  @Test
  void connectionFallsBackToResp2WhenHelloNotSupported() throws Exception {
    Connection connection = createSpyConnection();

    // hello() throws JedisDataException – server < 6.0 does not support HELLO
    doThrow(new JedisDataException("ERR unknown command 'HELLO'")).when(connection)
        .hello(any(byte[][].class));

    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3_PREFERRED).clientSetInfoConfig(ClientSetInfoConfig.DISABLED)
        .build();

    connection.initializeFromClientConfig(config);

    assertEquals(RedisProtocol.RESP2, connection.getRedisProtocol(),
      "Connection should fall back to RESP2 when HELLO is not supported");
  }

  /**
   * When RESP3_PREFERRED is configured with password-only credentials (no username), HELLO fails
   * with a JedisDataException (e.g., RESP3 not supported), Connection should fall back to RESP2.
   * The authenticate step happens before HELLO in this path.
   */
  @Test
  void connectionFallsBackToResp2WhenHelloFailsWithDataException() throws Exception {
    Socket mockSocket = mock(Socket.class);
    lenient().when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    lenient().when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    lenient().when(mockSocket.getSoTimeout()).thenReturn(2000);

    JedisSocketFactory socketFactory = mock(JedisSocketFactory.class);
    when(socketFactory.createSocket()).thenReturn(mockSocket);

    // Subclass Connection to override hello() and getStatusCodeReply()
    Connection connection = new Connection(socketFactory) {
      @Override
      protected Map<String, Object> hello(byte[]... args) {
        throw new JedisDataException("NOPROTO unsupported protocol version");
      }

      @Override
      public String getStatusCodeReply() {
        // Called by authenticate(credentials) for password-only auth
        return "OK";
      }
    };

    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3_PREFERRED).password("testpass")
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED).build();

    connection.initializeFromClientConfig(config);

    assertEquals(RedisProtocol.RESP2, connection.getRedisProtocol(),
      "Connection should fall back to RESP2 when HELLO fails with JedisDataException");
  }

  // ---------------------------------------------------------------------------
  // Scenarios with username+password: AUTH is always done first, then HELLO
  // ---------------------------------------------------------------------------

  /**
   * When RESP3_PREFERRED is configured with username+password and the server supports HELLO,
   * Connection should AUTH first, then HELLO succeeds, and protocol should be RESP3.
   */
  @Test
  void connectionNegotiatesResp3WhenAuthAndHelloSucceed() throws Exception {
    Socket mockSocket = mock(Socket.class);
    lenient().when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    lenient().when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    lenient().when(mockSocket.getSoTimeout()).thenReturn(2000);

    JedisSocketFactory socketFactory = mock(JedisSocketFactory.class);
    when(socketFactory.createSocket()).thenReturn(mockSocket);

    Connection connection = new Connection(socketFactory) {
      @Override
      protected Map<String, Object> hello(byte[]... args) {
        // HELLO 3 — succeeds (AUTH was already done before HELLO)
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("server", "redis");
        result.put("version", "6.0.20");
        result.put("proto", Long.valueOf(3));
        return result;
      }

      @Override
      public String getStatusCodeReply() {
        // Called by authenticate() for AUTH user pass
        return "OK";
      }
    };

    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3_PREFERRED).user("testuser").password("testpass")
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED).build();

    connection.initializeFromClientConfig(config);

    assertEquals(RedisProtocol.RESP3, connection.getRedisProtocol(),
      "Connection should negotiate RESP3 when AUTH and HELLO both succeed");
  }

  /**
   * When RESP3_PREFERRED is configured with username+password and HELLO fails (e.g., RESP3 not
   * supported), Connection should fall back to RESP2. AUTH is done before HELLO.
   */
  @Test
  void connectionFallsBackToResp2WhenHelloFailsAfterAuth() throws Exception {
    Socket mockSocket = mock(Socket.class);
    lenient().when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    lenient().when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    lenient().when(mockSocket.getSoTimeout()).thenReturn(2000);

    JedisSocketFactory socketFactory = mock(JedisSocketFactory.class);
    when(socketFactory.createSocket()).thenReturn(mockSocket);

    Connection connection = new Connection(socketFactory) {
      @Override
      protected Map<String, Object> hello(byte[]... args) {
        // HELLO 3 — fails (RESP3 not supported by this server)
        throw new JedisDataException("NOPROTO unsupported protocol version");
      }

      @Override
      public String getStatusCodeReply() {
        return "OK";
      }
    };

    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3_PREFERRED).user("testuser").password("testpass")
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED).build();

    connection.initializeFromClientConfig(config);

    assertEquals(RedisProtocol.RESP2, connection.getRedisProtocol(),
      "Connection should fall back to RESP2 when HELLO fails after AUTH");
  }

  /**
   * When non-RESP3_PREFERRED protocol (e.g., RESP3) is configured with username+password, and HELLO
   * fails with an access control error, the exception should propagate to the caller since there's
   * no fallback for explicit protocol requests.
   */
  @Test
  void connectionThrowsWhenHelloFailsAndProtocolIsNotPreferred() throws Exception {
    Socket mockSocket = mock(Socket.class);
    lenient().when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    lenient().when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    lenient().when(mockSocket.getSoTimeout()).thenReturn(2000);

    JedisSocketFactory socketFactory = mock(JedisSocketFactory.class);
    when(socketFactory.createSocket()).thenReturn(mockSocket);

    Connection connection = new Connection(socketFactory) {
      @Override
      protected Map<String, Object> hello(byte[]... args) {
        throw new JedisAccessControlException(
            "NOPERM this user has no permissions to run the 'hello' command or its subcommand");
      }

      @Override
      public String getStatusCodeReply() {
        return "OK";
      }
    };

    JedisClientConfig config = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .user("testuser").password("testpass").clientSetInfoConfig(ClientSetInfoConfig.DISABLED)
        .build();

    org.junit.jupiter.api.Assertions.assertThrows(JedisAccessControlException.class,
      () -> connection.initializeFromClientConfig(config),
      "Should throw JedisAccessControlException for explicit RESP3 when HELLO is denied");
  }

  // ---------------------------------------------------------------------------
  // RedisClient: verify RESP3_PREFERRED protocol resolution via Connection probe
  // ---------------------------------------------------------------------------

  /**
   * When RedisClient is built with RESP3_PREFERRED, the constructor probes a Connection from the
   * provider to resolve the actual protocol. If the Connection fell back to RESP2 (server does not
   * support HELLO), CommandObjects should get RESP2.
   */
  @Test
  void redisClientResolvesResp3PreferredToResp2WhenConnectionFellBack() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(
          DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3_PREFERRED).build())
        .build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

      assertEquals(RedisProtocol.RESP2, protocol,
        "CommandObjects should receive RESP2 resolved from Connection that fell back");
    }
  }

  /**
   * When RedisClient is built with RESP3_PREFERRED and the Connection negotiated RESP3,
   * CommandObjects should get RESP3.
   */
  @Test
  void redisClientResolvesResp3PreferredToResp3WhenConnectionSucceeded() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(
          DefaultJedisClientConfig.builder().build())
        .build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");

      assertEquals(RedisProtocol.RESP3, protocol,
        "CommandObjects should receive RESP3 resolved from Connection that negotiated RESP3");
    }
  }

  // ---------------------------------------------------------------------------
  // RedisClusterClient: verify RESP3_PREFERRED protocol resolution
  // ---------------------------------------------------------------------------

  /**
   * When RedisClusterClient is built with RESP3_PREFERRED and the Connection fell back to RESP2,
   * ClusterCommandObjects should get RESP2.
   */
  @Test
  void redisClusterClientResolvesResp3PreferredToResp2WhenConnectionFellBack() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP2);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379))).commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(
          DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3_PREFERRED).build())
        .build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertInstanceOf(ClusterCommandObjects.class, commandObjects,
        "RedisClusterClient should use ClusterCommandObjects");

      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");
      assertEquals(RedisProtocol.RESP2, protocol,
        "ClusterCommandObjects should receive RESP2 resolved from Connection that fell back");
    }
  }

  /**
   * When RedisClusterClient is built with RESP3_PREFERRED and the Connection negotiated RESP3,
   * ClusterCommandObjects should get RESP3.
   */
  @Test
  void redisClusterClientResolvesResp3PreferredToResp3WhenConnectionSucceeded() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379))).commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(
          DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3_PREFERRED).build())
        .build()) {

      CommandObjects commandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertInstanceOf(ClusterCommandObjects.class, commandObjects);

      RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");
      assertEquals(RedisProtocol.RESP3, protocol,
        "ClusterCommandObjects should receive RESP3 resolved from Connection that negotiated RESP3");
    }
  }

  // ---------------------------------------------------------------------------
  // End-to-end: Connection fallback protocol propagated via UnifiedJedis(Connection)
  // ---------------------------------------------------------------------------

  /**
   * When a Connection has fallen back to RESP2 (HELLO not supported) and is used to construct a
   * UnifiedJedis directly, the CommandObjects should receive RESP2.
   */
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

  /**
   * When a Connection has negotiated RESP3 (HELLO succeeded) and is used to construct a
   * UnifiedJedis directly, the CommandObjects should receive RESP3.
   */
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
  // Pipeline: verify RESP3 protocol propagation when server supports RESP3
  // ---------------------------------------------------------------------------

  /**
   * When a Pipeline is created from a Connection that negotiated RESP3, the Pipeline's
   * CommandObjects should have RESP3 protocol.
   */
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

  /**
   * When RedisClient.pipelined() is called and the client resolved RESP3 from RESP3_PREFERRED, the
   * Pipeline should inherit the client's CommandObjects with RESP3 protocol.
   */
  @Test
  void redisClientPipelinedInheritsResp3Protocol() {
    // Connection that negotiated RESP3
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(provider.getConnection()).thenReturn(mockConnection);

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider)
        .clientConfig(
          DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3_PREFERRED).build())
        .build()) {

      // Verify client resolved to RESP3
      CommandObjects clientCommandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      RedisProtocol clientProtocol = ReflectionTestUtil.getField(clientCommandObjects, "protocol");
      assertEquals(RedisProtocol.RESP3, clientProtocol);

      // pipelined() passes the client's commandObjects to Pipeline
      Pipeline pipeline = client.pipelined();
      CommandObjects pipelineCommandObjects = ReflectionTestUtil.getField(pipeline,
        "commandObjects");

      assertSame(clientCommandObjects, pipelineCommandObjects,
        "Pipeline should share the same CommandObjects instance as the client");

      RedisProtocol pipelineProtocol = ReflectionTestUtil.getField(pipelineCommandObjects,
        "protocol");
      assertEquals(RedisProtocol.RESP3, pipelineProtocol,
        "Pipeline's CommandObjects should have RESP3 inherited from the client");
    }
  }

  // ---------------------------------------------------------------------------
  // ClusterPipeline: verify RESP3 protocol propagation when server supports RESP3
  // ---------------------------------------------------------------------------

  /**
   * When ClusterPipeline is created with RESP3_PREFERRED and the provider returns a Connection that
   * negotiated RESP3, the ClusterCommandObjects should resolve to RESP3.
   */
  @Test
  void clusterPipelineResolvesResp3PreferredToResp3() {
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    ClusterConnectionProvider clusterProvider = mock(ClusterConnectionProvider.class);
    when(clusterProvider.getConnection()).thenReturn(mockConnection);

    ClusterPipeline pipeline = new ClusterPipeline(clusterProvider, RedisProtocol.RESP3_PREFERRED);

    CommandObjects commandObjects = ReflectionTestUtil.getField(pipeline, "commandObjects");
    assertInstanceOf(ClusterCommandObjects.class, commandObjects,
      "ClusterPipeline should use ClusterCommandObjects");

    RedisProtocol protocol = ReflectionTestUtil.getField(commandObjects, "protocol");
    assertEquals(RedisProtocol.RESP3, protocol,
      "ClusterPipeline's ClusterCommandObjects should have RESP3 when Connection negotiated RESP3");
  }

  /**
   * When RedisClusterClient.pipelined() is called and the client resolved RESP3 from
   * RESP3_PREFERRED, the ClusterPipeline should inherit the client's ClusterCommandObjects with
   * RESP3 protocol.
   */
  @Test
  void redisClusterClientPipelinedInheritsResp3Protocol() {
    // Connection that negotiated RESP3
    Connection mockConnection = mock(Connection.class);
    when(mockConnection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);

    // RedisClusterClient.pipelined() casts provider to ClusterConnectionProvider
    ClusterConnectionProvider clusterProvider = mock(ClusterConnectionProvider.class);
    when(clusterProvider.getConnection()).thenReturn(mockConnection);

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379))).commandExecutor(exec)
        .connectionProvider(clusterProvider)
        .clientConfig(
          DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3_PREFERRED).build())
        .build()) {

      // Verify client resolved to RESP3
      CommandObjects clientCommandObjects = ReflectionTestUtil.getField(client, "commandObjects");
      assertInstanceOf(ClusterCommandObjects.class, clientCommandObjects);
      RedisProtocol clientProtocol = ReflectionTestUtil.getField(clientCommandObjects, "protocol");
      assertEquals(RedisProtocol.RESP3, clientProtocol);

      // pipelined() passes the client's ClusterCommandObjects to ClusterPipeline
      ClusterPipeline pipeline = client.pipelined();
      CommandObjects pipelineCommandObjects = ReflectionTestUtil.getField(pipeline,
        "commandObjects");

      assertSame(clientCommandObjects, pipelineCommandObjects,
        "ClusterPipeline should share the same ClusterCommandObjects instance as the client");

      RedisProtocol pipelineProtocol = ReflectionTestUtil.getField(pipelineCommandObjects,
        "protocol");
      assertEquals(RedisProtocol.RESP3, pipelineProtocol,
        "ClusterPipeline's ClusterCommandObjects should have RESP3 inherited from the client");
    }
  }

  // ---------------------------------------------------------------------------
  // JedisPubSubBase: verify RESP3 protocol propagation to pub/sub
  // ---------------------------------------------------------------------------

  /**
   * When a Connection has negotiated RESP3 and has token-based auth enabled,
   * {@link JedisPubSubBase#subscribe} should NOT throw, because
   * {@code checkConnectionSuitableForPubSub()} allows RESP3 with token-based auth.
   */
  @Test
  void pubSubAllowsSubscribeWhenConnectionHasResp3WithTokenAuth() {
    Connection mockConnection = mock(Connection.class);
    // Connection.protocol is accessed directly (not via getter) by checkConnectionSuitableForPubSub
    // so we need to set the field via reflection
    ReflectionTestUtil.setField(mockConnection, "protocol", RedisProtocol.RESP3);
    // lenient: with RESP3 the condition short-circuits before calling this method
    lenient().when(mockConnection.isTokenBasedAuthenticationEnabled()).thenReturn(true);

    JedisPubSubBase<String> pubSub = new JedisPubSub() {
      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
      }
    };

    // registerForAuthentication sets authenticator.client = mockConnection
    JedisSafeAuthenticator authenticator = ReflectionTestUtil.getField(pubSub, "authenticator");
    ReflectionTestUtil.setField(authenticator, "client", mockConnection);

    // subscribe() calls checkConnectionSuitableForPubSub() which reads
    // authenticator.client.protocol directly — should NOT throw with RESP3
    // It will then call sendAndFlushCommand which we don't need to test here,
    // so we expect it to proceed past the check and fail at sendCommand
    try {
      pubSub.subscribe("testchannel");
    } catch (JedisException e) {
      // We expect a different exception (e.g., from sendCommand/flush), NOT the
      // "Blocking pub/sub operations are not supported" message
      assertFalse(e.getMessage().contains("Blocking pub/sub operations are not supported"),
        "RESP3 connection should be allowed for pub/sub with token-based auth");
    }
  }

  /**
   * When a Connection has RESP2 protocol and has token-based auth enabled,
   * {@link JedisPubSubBase#subscribe} should throw JedisException because
   * {@code checkConnectionSuitableForPubSub()} rejects RESP2 with token-based auth.
   */
  @Test
  void pubSubRejectsSubscribeWhenConnectionHasResp2WithTokenAuth() {
    Connection mockConnection = mock(Connection.class);
    ReflectionTestUtil.setField(mockConnection, "protocol", RedisProtocol.RESP2);
    when(mockConnection.isTokenBasedAuthenticationEnabled()).thenReturn(true);

    JedisPubSubBase<String> pubSub = new JedisPubSub() {
    };

    JedisSafeAuthenticator authenticator = ReflectionTestUtil.getField(pubSub, "authenticator");
    ReflectionTestUtil.setField(authenticator, "client", mockConnection);

    JedisException ex = org.junit.jupiter.api.Assertions.assertThrows(JedisException.class,
      () -> pubSub.subscribe("testchannel"));
    assertTrue(ex.getMessage().contains("Blocking pub/sub operations are not supported"),
      "RESP2 connection with token-based auth should be rejected for pub/sub");
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  /**
   * Creates a spy Connection backed by mocked socket infrastructure. The spy allows overriding
   * {@code hello()} to simulate servers that do not support the HELLO command.
   */
  private Connection createSpyConnection() throws Exception {
    Socket mockSocket = mock(Socket.class);
    lenient().when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    lenient().when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    lenient().when(mockSocket.getSoTimeout()).thenReturn(2000);
    lenient().when(mockSocket.isBound()).thenReturn(true);
    lenient().when(mockSocket.isConnected()).thenReturn(true);
    lenient().when(mockSocket.isClosed()).thenReturn(false);
    lenient().when(mockSocket.isInputShutdown()).thenReturn(false);
    lenient().when(mockSocket.isOutputShutdown()).thenReturn(false);

    JedisSocketFactory socketFactory = mock(JedisSocketFactory.class);
    when(socketFactory.createSocket()).thenReturn(mockSocket);

    return spy(new Connection(socketFactory));
  }
}
