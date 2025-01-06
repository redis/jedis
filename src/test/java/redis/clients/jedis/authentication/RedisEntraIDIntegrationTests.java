package redis.clients.jedis.authentication;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.MockedConstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDIdentityProvider;
import redis.clients.authentication.entraid.EntraIDIdentityProviderConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;
import redis.clients.authentication.entraid.ServicePrincipalInfo;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.scenario.FaultInjectionClient;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedisEntraIDIntegrationTests {
  private static final Logger log = LoggerFactory.getLogger(RedisEntraIDIntegrationTests.class);

  private static EntraIDTestContext testCtx;
  private static EndpointConfig endpointConfig;
  private static HostAndPort hnp;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @BeforeClass
  public static void before() {
    try {
      testCtx = EntraIDTestContext.DEFAULT;
      endpointConfig = HostAndPorts.getRedisEndpoint("standalone-entraid-acl");
      hnp = endpointConfig.getHostAndPort();
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      org.junit.Assume.assumeTrue(false);
    }
  }

  @Test
  public void testJedisConfig() {
    AtomicInteger counter = new AtomicInteger(0);
    try (MockedConstruction<EntraIDIdentityProvider> mockedConstructor = mockConstruction(
      EntraIDIdentityProvider.class, (mock, context) -> {
        ServicePrincipalInfo info = (ServicePrincipalInfo) context.arguments().get(0);

        assertEquals(testCtx.getClientId(), info.getClientId());
        assertEquals(testCtx.getAuthority(), info.getAuthority());
        assertEquals(testCtx.getClientSecret(), info.getSecret());
        assertEquals(testCtx.getRedisScopes(), context.arguments().get(1));
        assertNotNull(mock);
        doAnswer(invocation -> {
          counter.incrementAndGet();
          return new SimpleToken("default", "token1", System.currentTimeMillis() + 5 * 60 * 1000,
              System.currentTimeMillis(), null);
        }).when(mock).requestToken();
      })) {

      TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
          .authority(testCtx.getAuthority()).clientId(testCtx.getClientId())
          .secret(testCtx.getClientSecret()).scopes(testCtx.getRedisScopes()).build();

      DefaultJedisClientConfig jedisConfig = DefaultJedisClientConfig.builder()
          .authXManager(new AuthXManager(tokenAuthConfig)).build();

      JedisPooled jedis = new JedisPooled(new HostAndPort("localhost", 6379), jedisConfig);
      assertNotNull(jedis);
      assertEquals(1, counter.get());

    }
  }

  // T.1.1
  // Verify authentication using Azure AD with service principals
  @Test
  public void withSecret_azureServicePrincipalIntegrationTest() {
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .clientId(testCtx.getClientId()).secret(testCtx.getClientSecret())
        .authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes()).build();

    DefaultJedisClientConfig jedisConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (JedisPooled jedis = new JedisPooled(hnp, jedisConfig)) {
      String key = UUID.randomUUID().toString();
      jedis.set(key, "value");
      assertEquals("value", jedis.get(key));
      jedis.del(key);
    }
  }

  // T.1.1        
  // Verify authentication using Azure AD with service principals
  @Test
  public void withCertificate_azureServicePrincipalIntegrationTest() {
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .clientId(testCtx.getClientId()).secret(testCtx.getClientSecret())
        .authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes()).build();

    DefaultJedisClientConfig jedisConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (JedisPooled jedis = new JedisPooled(hnp, jedisConfig)) {
      String key = UUID.randomUUID().toString();
      jedis.set(key, "value");
      assertEquals("value", jedis.get(key));
      jedis.del(key);
    }
  }

  // T.2.2
  // Test that the Redis client is not blocked/interrupted during token renewal.
  @Test
  public void renewalDuringOperationsTest() throws InterruptedException, ExecutionException {
    // set the stage with consecutive get/set operations with unique keys which keeps running with a jedispooled instace, 
    // configure token manager to renew token approximately approximately every 10ms
    // wait till token was renewed at least 10 times after initial token acquisition 
    // Additional note: Assumptions made on the time taken for token renewal and operations are based on the current implementation and may vary in future
    // Assumptions:
    //    - TTL of token is 2 hour
    //    - expirationRefreshRatio is 0.000001F
    //    - renewal delay is 7 ms each time a token is acquired
    //    - each auth command takes 40 ms in total to complete(considering the cloud test environments)
    //    - each auth command would need to wait for an ongoing customer operation(GET/SET/DEL) to complete, which would take another 40 ms
    //    - each renewal happens in 40+40+7 = 87 ms
    //    - total number of renewals would take 87 * 10 = 870 ms
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .clientId(testCtx.getClientId()).secret(testCtx.getClientSecret())
        .authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes())
        .expirationRefreshRatio(0.000001F).build();

    AuthXManager authXManager = new AuthXManager(tokenAuthConfig);
    Consumer<Token> hook = mock(Consumer.class);
    authXManager.addPostAuthenticationHook(hook);

    DefaultJedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
        .authXManager(authXManager).build();

    ExecutorService jedisExecutors = Executors.newFixedThreadPool(5);
    AtomicBoolean completed = new AtomicBoolean(false);

    ExecutorService runner = Executors.newSingleThreadExecutor();
    runner.submit(() -> {

      try (JedisPooled jedis = new JedisPooled(hnp, jedisClientConfig)) {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
          Future<?> future = jedisExecutors.submit(() -> {
            while (!completed.get()) {
              String key = UUID.randomUUID().toString();
              jedis.set(key, "value");
              assertEquals("value", jedis.get(key));
              jedis.del(key);
            }
          });
          futures.add(future);
        }
        for (Future<?> task : futures) {
          try {
            task.get();
          } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
          }
        }
      }
    });

    await().pollInterval(ONE_HUNDRED_MILLISECONDS).atMost(FIVE_SECONDS).untilAsserted(() -> {
      verify(hook, atLeast(10)).accept(any());
    });

    completed.set(true);
    runner.shutdown();
    jedisExecutors.shutdown();
  }

  // T.3.2
  // Verify that all existing connections can be re-authenticated when a new token is received.
  @Test
  public void allConnectionsReauthTest() throws InterruptedException, ExecutionException {
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .clientId(testCtx.getClientId()).secret(testCtx.getClientSecret())
        .authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes())
        .expirationRefreshRatio(0.000001F).build();

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

    DefaultJedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
        .authXManager(authXManager).build();

    long startTime = System.currentTimeMillis();
    List<Future<?>> futures = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(5);

    try (JedisPooled jedis = new JedisPooled(hnp, jedisClientConfig)) {
      for (int i = 0; i < 5; i++) {
        Future<?> future = executor.submit(() -> {
          for (; System.currentTimeMillis() - startTime < 2000;) {
            String key = UUID.randomUUID().toString();
            jedis.set(key, "value");
            assertEquals("value", jedis.get(key));
            jedis.del(key);
          }
        });
        futures.add(future);
      }
      for (Future<?> task : futures) {
        task.get();
      }

      connections.forEach(conn -> {
        verify(conn, atLeast(1)).reAuthenticate();
      });
      executor.shutdown();
    }
  }

  // T.3.3
  // Verify behavior when attempting to authenticate a single connection with an expired token.
  @Test
  public void connectionAuthWithExpiredTokenTest() {
    IdentityProvider idp = new EntraIDIdentityProviderConfig(
        new ServicePrincipalInfo(testCtx.getClientId(), testCtx.getClientSecret(),
            testCtx.getAuthority()),
        testCtx.getRedisScopes(), 1000).getProvider();

    IdentityProvider mockIdentityProvider = mock(IdentityProvider.class);
    AtomicReference<Token> token = new AtomicReference<>();
    doAnswer(invocation -> {
      if (token.get() == null) {
        token.set(idp.requestToken());
      }
      return token.get();
    }).when(mockIdentityProvider).requestToken();
    IdentityProviderConfig idpConfig = mock(IdentityProviderConfig.class);
    when(idpConfig.getProvider()).thenReturn(mockIdentityProvider);

    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .identityProviderConfig(idpConfig).expirationRefreshRatio(0.000001F).build();
    AuthXManager authXManager = new AuthXManager(tokenAuthConfig);
    DefaultJedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
        .authXManager(authXManager).build();

    try (JedisPooled jedis = new JedisPooled(hnp, jedisClientConfig)) {
      for (int i = 0; i < 50; i++) {
        String key = UUID.randomUUID().toString();
        jedis.set(key, "value");
        assertEquals("value", jedis.get(key));
        jedis.del(key);
      }

      token.set(new SimpleToken(idp.requestToken().getUser(), "token1",
          System.currentTimeMillis() - 1, System.currentTimeMillis(), null));

      JedisAccessControlException aclException = assertThrows(JedisAccessControlException.class,
        () -> {
          for (int i = 0; i < 50; i++) {
            String key = UUID.randomUUID().toString();
            jedis.set(key, "value");
            assertEquals("value", jedis.get(key));
            jedis.del(key);
          }
        });

      assertEquals("WRONGPASS invalid username-password pair", aclException.getMessage());
    }
  }

  // T.3.4
  // Verify handling of reconnection and re-authentication after a network partition. (use cached token)
  @Test
  public void networkPartitionEvictionTest() {
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .clientId(testCtx.getClientId()).secret(testCtx.getClientSecret())
        .authority(testCtx.getAuthority()).scopes(testCtx.getRedisScopes())
        .expirationRefreshRatio(0.5F).build();
    AuthXManager authXManager = new AuthXManager(tokenAuthConfig);
    DefaultJedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
        .authXManager(authXManager).build();

    try (JedisPooled jedis = new JedisPooled(hnp, jedisClientConfig)) {
      for (int i = 0; i < 5; i++) {
        String key = UUID.randomUUID().toString();
        jedis.set(key, "value");
        assertEquals("value", jedis.get(key));
        jedis.del(key);
      }

      triggerNetworkFailure();

      JedisConnectionException aclException = assertThrows(JedisConnectionException.class, () -> {
        for (int i = 0; i < 50; i++) {
          String key = UUID.randomUUID().toString();
          jedis.set(key, "value");
          assertEquals("value", jedis.get(key));
          jedis.del(key);
        }
      });

      assertEquals("Unexpected end of stream.", aclException.getMessage());
      Awaitility.await().pollDelay(Durations.ONE_HUNDRED_MILLISECONDS).atMost(Durations.TWO_SECONDS)
          .until(() -> {
            String key = UUID.randomUUID().toString();
            jedis.set(key, "value");
            assertEquals("value", jedis.get(key));
            jedis.del(key);
            return true;
          });
    }
  }

  private void triggerNetworkFailure() {
    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpointConfig.getBdbId());

    FaultInjectionClient.TriggerActionResponse actionResponse = null;
    String action = "network_failure";
    try {
      log.info("Triggering {}", action);
      actionResponse = faultClient.triggerAction(action, params);
    } catch (IOException e) {
      fail("Fault Injection Server error:" + e.getMessage());
    }
    log.info("Action id: {}", actionResponse.getActionId());
  }
}
