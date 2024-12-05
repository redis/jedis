package redis.clients.jedis.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Durations;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;
import redis.clients.authentication.core.TokenManagerConfig;
import redis.clients.authentication.core.TokenRequestException;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;

public class TokenBasedAuthenticationUnitTests {

  private HostAndPort hnp = new HostAndPort("localhost", 6379);
  private EndpointConfig endpoint = new EndpointConfig(hnp, null, null, false);

  @Test
  public void testJedisAuthXManagerInstance() {
    TokenManagerConfig tokenManagerConfig = mock(TokenManagerConfig.class);
    IdentityProviderConfig identityProviderConfig = mock(IdentityProviderConfig.class);
    IdentityProvider identityProvider = mock(IdentityProvider.class);

    when(identityProviderConfig.getProvider()).thenReturn(identityProvider);

    try (MockedConstruction<TokenManager> mockedConstructor = mockConstruction(TokenManager.class,
      (mock, context) -> {
        assertEquals(identityProvider, context.arguments().get(0));
        assertEquals(tokenManagerConfig, context.arguments().get(1));
      })) {

      new AuthXManager(new TokenAuthConfig(tokenManagerConfig, identityProviderConfig));
    }
  }

  @Test
  public void withExpirationRefreshRatio_testJedisAuthXManagerTriggersEvict() throws Exception {

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken())
        .thenReturn(new SimpleToken("password", System.currentTimeMillis() + 1000,
            System.currentTimeMillis(), Collections.singletonMap("oid", "default")));

    TokenManager tokenManager = new TokenManager(idProvider,
        new TokenManagerConfig(0.4F, 100, 1000, null));
    AuthXManager jedisAuthXManager = new AuthXManager(tokenManager);

    AtomicInteger numberOfEvictions = new AtomicInteger(0);
    ConnectionPool pool = new ConnectionPool(hnp,
        endpoint.getClientConfigBuilder().authXManager(jedisAuthXManager).build()) {
      @Override
      public void evict() throws Exception {
        numberOfEvictions.incrementAndGet();
        super.evict();
      }
    };

    await().pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
        .atMost(Durations.FIVE_HUNDRED_MILLISECONDS)
        .until(numberOfEvictions::get, Matchers.greaterThanOrEqualTo(1));
  }

  public void withLowerRefreshBounds_testJedisAuthXManagerTriggersEvict() throws Exception {

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken())
        .thenReturn(new SimpleToken("password", System.currentTimeMillis() + 1000,
            System.currentTimeMillis(), Collections.singletonMap("oid", "default")));

    TokenManager tokenManager = new TokenManager(idProvider,
        new TokenManagerConfig(0.9F, 600, 1000, null));
    AuthXManager jedisAuthXManager = new AuthXManager(tokenManager);

    AtomicInteger numberOfEvictions = new AtomicInteger(0);
    ConnectionPool pool = new ConnectionPool(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().authXManager(jedisAuthXManager).build()) {
      @Override
      public void evict() throws Exception {
        numberOfEvictions.incrementAndGet();
        super.evict();
      }
    };

    await().pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
        .atMost(Durations.FIVE_HUNDRED_MILLISECONDS)
        .until(numberOfEvictions::get, Matchers.greaterThanOrEqualTo(1));
  }

  public static class TokenManagerConfigWrapper extends TokenManagerConfig {
    int lower;
    float ratio;

    public TokenManagerConfigWrapper() {
      super(0, 0, 0, null);
    }

    @Override
    public int getLowerRefreshBoundMillis() {
      return lower;
    }

    @Override
    public float getExpirationRefreshRatio() {
      return ratio;
    }
  }

  @Test
  public void testCalculateRenewalDelay() {
    long delay = 0;
    long duration = 0;
    long issueDate;
    long expireDate;

    TokenManagerConfigWrapper config = new TokenManagerConfigWrapper();
    TokenManager manager = new TokenManager(() -> null, config);

    duration = 5000;
    config.lower = 2000;
    config.ratio = 0.5F;
    issueDate = System.currentTimeMillis();
    expireDate = issueDate + duration;

    delay = manager.calculateRenewalDelay(expireDate, issueDate);

    assertThat(delay, Matchers
        .greaterThanOrEqualTo(Math.min(duration - config.lower, (long) (duration * config.ratio))));

    duration = 10000;
    config.lower = 8000;
    config.ratio = 0.2F;
    issueDate = System.currentTimeMillis();
    expireDate = issueDate + duration;

    delay = manager.calculateRenewalDelay(expireDate, issueDate);

    assertThat(delay, Matchers
        .greaterThanOrEqualTo(Math.min(duration - config.lower, (long) (duration * config.ratio))));

    duration = 10000;
    config.lower = 10000;
    config.ratio = 0.2F;
    issueDate = System.currentTimeMillis();
    expireDate = issueDate + duration;

    delay = manager.calculateRenewalDelay(expireDate, issueDate);

    assertEquals(0, delay);

    duration = 0;
    config.lower = 5000;
    config.ratio = 0.2F;
    issueDate = System.currentTimeMillis();
    expireDate = issueDate + duration;

    delay = manager.calculateRenewalDelay(expireDate, issueDate);

    assertEquals(0, delay);

    duration = 10000;
    config.lower = 1000;
    config.ratio = 0.00001F;
    issueDate = System.currentTimeMillis();
    expireDate = issueDate + duration;

    delay = manager.calculateRenewalDelay(expireDate, issueDate);

    assertEquals(0, delay);

    duration = 10000;
    config.lower = 1000;
    config.ratio = 0.0001F;
    issueDate = System.currentTimeMillis();
    expireDate = issueDate + duration;

    delay = manager.calculateRenewalDelay(expireDate, issueDate);

    assertThat(delay, either(is(0L)).or(is(1L)));
  }

  @Test
  public void testAuthXManagerReceivesNewToken()
      throws InterruptedException, ExecutionException, TimeoutException {

    IdentityProvider identityProvider = () -> new SimpleToken("tokenVal",
        System.currentTimeMillis() + 5 * 1000, System.currentTimeMillis(),
        Collections.singletonMap("oid", "user1"));

    TokenManager tokenManager = new TokenManager(identityProvider,
        new TokenManagerConfig(0.7F, 200, 2000, null));

    AuthXManager manager = spy(new AuthXManager(tokenManager));

    final Token[] tokenHolder = new Token[1];
    doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      tokenHolder[0] = (Token) args[0];
      return null;
    }).when(manager).authenticateConnections(any());

    manager.start(true);
    assertEquals(tokenHolder[0].getValue(), "tokenVal");
  }

  @Test
  public void testBlockForInitialToken() {
    String exceptionMessage = "Test exception from identity provider!";
    IdentityProvider identityProvider = () -> {
      throw new RuntimeException(exceptionMessage);
    };

    TokenManager tokenManager = new TokenManager(identityProvider,
        new TokenManagerConfig(0.7F, 200, 2000, new TokenManagerConfig.RetryPolicy(5, 100)));

    AuthXManager manager = new AuthXManager(tokenManager);
    TokenRequestException e = assertThrows(TokenRequestException.class, () -> manager.start(true));

    assertEquals(exceptionMessage, e.getCause().getCause().getCause().getMessage());
  }

  @Test
  public void testNoBlockForInitialToken()
      throws InterruptedException, ExecutionException, TimeoutException {
    int numberOfRetries = 5;
    CountDownLatch requesLatch = new CountDownLatch(numberOfRetries);
    IdentityProvider identityProvider = () -> {
      requesLatch.countDown();
      throw new RuntimeException("Test exception from identity provider!");
    };

    TokenManager tokenManager = new TokenManager(identityProvider, new TokenManagerConfig(0.7F, 200,
        2000, new TokenManagerConfig.RetryPolicy(numberOfRetries - 1, 0)));

    AuthXManager manager = spy(new AuthXManager(tokenManager));
    manager.start(false);

    requesLatch.await();
    verify(manager, Mockito.atLeastOnce()).onError(Mockito.any());
    verify(manager, Mockito.never()).authenticateConnections(Mockito.any());
  }

  @Test
  public void testTokenManagerWithFailingTokenRequest()
      throws InterruptedException, ExecutionException, TimeoutException {
    int numberOfRetries = 5;
    CountDownLatch requesLatch = new CountDownLatch(numberOfRetries);

    IdentityProvider identityProvider = mock(IdentityProvider.class);
    when(identityProvider.requestToken()).thenAnswer(invocation -> {
      requesLatch.countDown();
      if (requesLatch.getCount() > 0) {
        throw new RuntimeException("Test exception from identity provider!");
      }
      return new SimpleToken("tokenValX", System.currentTimeMillis() + 50 * 1000,
          System.currentTimeMillis(), Collections.singletonMap("oid", "user1"));
    });

    ArgumentCaptor<Token> argument = ArgumentCaptor.forClass(Token.class);

    TokenManager tokenManager = new TokenManager(identityProvider, new TokenManagerConfig(0.7F, 200,
        2000, new TokenManagerConfig.RetryPolicy(numberOfRetries - 1, 100)));

    TokenListener listener = mock(TokenListener.class);
    tokenManager.start(listener, false);
    requesLatch.await();
    verify(identityProvider, times(numberOfRetries)).requestToken();
    verify(listener, never()).onError(any());
    verify(listener).onTokenRenewed(argument.capture());
    assertEquals("tokenValX", argument.getValue().getValue());
  }

  @Test
  public void testTokenManagerWithHangingTokenRequest()
      throws InterruptedException, ExecutionException, TimeoutException {
    int sleepDuration = 200;
    int executionTimeout = 100;
    int tokenLifetime = 50 * 1000;
    int numberOfRetries = 5;
    CountDownLatch requesLatch = new CountDownLatch(numberOfRetries);

    IdentityProvider identityProvider = () -> {
      requesLatch.countDown();
      if (requesLatch.getCount() > 0) {
        try {
          Thread.sleep(sleepDuration);
        } catch (InterruptedException e) {
        }
        return null;
      }
      return new SimpleToken("tokenValX", System.currentTimeMillis() + tokenLifetime,
          System.currentTimeMillis(), Collections.singletonMap("oid", "user1"));
    };

    TokenManager tokenManager = new TokenManager(identityProvider, new TokenManagerConfig(0.7F, 200,
        executionTimeout, new TokenManagerConfig.RetryPolicy(numberOfRetries, 100)));

    AuthXManager manager = spy(new AuthXManager(tokenManager));
    manager.start(false);
    requesLatch.await();
    verify(manager, never()).onError(any());
    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(manager, times(1)).authenticateConnections(any());
    });
  }
}
