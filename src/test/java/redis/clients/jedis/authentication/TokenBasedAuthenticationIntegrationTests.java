package redis.clients.jedis.authentication;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_SECOND;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
/*  */
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisException;

public class TokenBasedAuthenticationIntegrationTests {
  private static final Logger log = LoggerFactory
      .getLogger(TokenBasedAuthenticationIntegrationTests.class);

  private static EndpointConfig endpointConfig;

  @BeforeClass
  public static void before() {
    try {
      endpointConfig = HostAndPorts.getRedisEndpoint("standalone0");
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      org.junit.Assume.assumeTrue(false);
    }
  }

  @Test
  public void testJedisPooledForInitialAuth() {
    String user = "default";
    String password = endpointConfig.getPassword();

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken()).thenReturn(new SimpleToken(user, password,
        System.currentTimeMillis() + 100000, System.currentTimeMillis(), null));

    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);

    TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
        .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
        .lowerRefreshBoundMillis(10000).tokenRequestExecTimeoutInMs(1000).build();

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (JedisPooled jedis = new JedisPooled(endpointConfig.getHostAndPort(), clientConfig)) {
      jedis.get("key1");
    }
  }

  @Test
  public void testJedisPooledReauth() {
    String user = "default";
    String password = endpointConfig.getPassword();

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken()).thenAnswer(invocation -> new SimpleToken(user, password,
        System.currentTimeMillis() + 5000, System.currentTimeMillis(), null));

    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);

    TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
        .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
        .lowerRefreshBoundMillis(4800).tokenRequestExecTimeoutInMs(1000).build();

    AuthXManager authXManager = new AuthXManager(tokenAuthConfig);
    authXManager = spy(authXManager);
    List<Connection> connections = new ArrayList<>();
    doAnswer(invocation -> {
      Connection connection = spy((Connection) invocation.getArgument(0));
      invocation.getArguments()[0] = connection;
      connections.add(connection);
      Object result = invocation.callRealMethod();
      return result;
    }).when(authXManager).addConnection(any(Connection.class));

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().authXManager(authXManager)
        .build();

    try (JedisPooled jedis = new JedisPooled(endpointConfig.getHostAndPort(), clientConfig)) {
      AtomicBoolean stop = new AtomicBoolean(false);
      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.submit(() -> {
        while (!stop.get()) {
          jedis.get("key1");
        }
      });

      for (Connection connection : connections) {
        await().pollDelay(ONE_HUNDRED_MILLISECONDS).atMost(ONE_SECOND).untilAsserted(() -> {
          verify(connection, atLeast(3)).reAuthenticate();
        });
      }
      stop.set(true);
      executor.shutdown();
    }
  }

  @Test
  public void testPubSubForInitialAuth() throws InterruptedException {
    String user = "default";
    String password = endpointConfig.getPassword();

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken()).thenReturn(new SimpleToken(user, password,
        System.currentTimeMillis() + 100000, System.currentTimeMillis(), null));

    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);

    TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
        .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
        .lowerRefreshBoundMillis(10000).tokenRequestExecTimeoutInMs(1000).build();

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).protocol(RedisProtocol.RESP3).build();

    JedisPubSub pubSub = new JedisPubSub() {
      public void onSubscribe(String channel, int subscribedChannels) {
        this.unsubscribe();
      }
    };

    try (JedisPooled jedis = new JedisPooled(endpointConfig.getHostAndPort(), clientConfig)) {
      jedis.subscribe(pubSub, "channel1");
    }
  }

  @Test
  public void testJedisPubSubReauth() {
    String user = "default";
    String password = endpointConfig.getPassword();

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken()).thenAnswer(invocation -> new SimpleToken(user, password,
        System.currentTimeMillis() + 5000, System.currentTimeMillis(), null));

    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);

    TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
        .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
        .lowerRefreshBoundMillis(4800).tokenRequestExecTimeoutInMs(1000).build();

    AuthXManager authXManager = new AuthXManager(tokenAuthConfig);
    authXManager = spy(authXManager);
    List<Connection> connections = new ArrayList<>();
    doAnswer(invocation -> {
      Connection connection = spy((Connection) invocation.getArgument(0));
      invocation.getArguments()[0] = connection;
      connections.add(connection);
      Object result = invocation.callRealMethod();
      return result;
    }).when(authXManager).addConnection(any(Connection.class));

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().authXManager(authXManager)
        .protocol(RedisProtocol.RESP3).build();

    JedisPubSub pubSub = new JedisPubSub() {
    };
    try (JedisPooled jedis = new JedisPooled(endpointConfig.getHostAndPort(), clientConfig)) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.submit(() -> {
        jedis.subscribe(pubSub, "channel1");
      });

      await().pollDelay(ONE_HUNDRED_MILLISECONDS).atMost(ONE_SECOND)
          .until(pubSub::getSubscribedChannels, greaterThan(0));

      assertEquals(1, connections.size());
      for (Connection connection : connections) {
        await().pollDelay(ONE_HUNDRED_MILLISECONDS).atMost(ONE_SECOND).untilAsserted(() -> {
          ArgumentCaptor<CommandArguments> captor = ArgumentCaptor.forClass(CommandArguments.class);

          verify(connection, atLeast(3)).sendCommand(captor.capture());
          assertThat(captor.getAllValues().stream()
              .filter((item) -> item.getCommand() == Command.AUTH).count(),
            greaterThan(3L));

        });
      }
      pubSub.unsubscribe();
      executor.shutdown();
    }
  }

  @Test
  public void testJedisPubSubWithResp2() {
    String user = "default";
    String password = endpointConfig.getPassword();

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken()).thenReturn(new SimpleToken(user, password,
        System.currentTimeMillis() + 100000, System.currentTimeMillis(), null));

    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);

    TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
        .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
        .lowerRefreshBoundMillis(10000).tokenRequestExecTimeoutInMs(1000).build();

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (JedisPooled jedis = new JedisPooled(endpointConfig.getHostAndPort(), clientConfig)) {
      JedisPubSub pubSub = new JedisPubSub() {
      };
      JedisException e = assertThrows(JedisException.class,
        () -> jedis.subscribe(pubSub, "channel1"));
      assertEquals(
        "Blocking pub/sub operations are not supported on token-based authentication enabled connections with RESP2 protocol!",
        e.getMessage());
    }
  }
}
