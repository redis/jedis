package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenManagerConfig;
import redis.clients.authentication.core.TokenManagerConfig.RetryPolicy;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.providers.SentineledConnectionProvider.SentinelConnectionFactory;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Master failover rotates the master pool; the old pool must be unregistered from the shared
 * {@link AuthXManager} and the new one registered exactly once.
 */
public class SentineledConnectionProviderAuthXManagerTest {

  private static final String MASTER_NAME = "mymaster";
  private static final HostAndPort SENTINEL = new HostAndPort("localhost", 26379);
  private static final HostAndPort OLD_MASTER = new HostAndPort("localhost", 6379);
  private static final HostAndPort NEW_MASTER = new HostAndPort("localhost", 6380);

  // keeps the mocked subscribe() blocked like a real one until the test is over
  private final CountDownLatch subscriptionEnd = new CountDownLatch(1);

  @AfterEach
  void tearDown() {
    subscriptionEnd.countDown();
  }

  private static AuthXManager authXManagerWithStaticToken() {
    IdentityProvider idProvider = mock(IdentityProvider.class);
    long now = System.currentTimeMillis();
    when(idProvider.requestToken()).thenReturn(new SimpleToken("default", "password",
      now + TimeUnit.HOURS.toMillis(1), now, Collections.singletonMap("oid", "default")));
    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);
    return new AuthXManager(new TokenAuthConfig(
        new TokenManagerConfig(0.9F, 1000, 1000, new RetryPolicy(1, 1)), idProviderConfig));
  }

  private static List<?> hooksOf(AuthXManager authXManager) {
    return ReflectionTestUtil.getField(authXManager, "postAuthenticateHooks");
  }

  @Test
  public void failoverUnregistersOldMasterPool() throws Exception {
    AuthXManager authXManager = authXManagerWithStaticToken();
    JedisClientConfig masterConfig = DefaultJedisClientConfig.builder().authXManager(authXManager)
        .build();

    CompletableFuture<JedisPubSub> subscriber = new CompletableFuture<>();
    Jedis sentinelJedis = mock(Jedis.class);
    when(sentinelJedis.sentinelGetMasterAddrByName(MASTER_NAME))
        .thenReturn(Arrays.asList(OLD_MASTER.getHost(), String.valueOf(OLD_MASTER.getPort())));
    doAnswer(invocation -> {
      subscriber.complete(invocation.getArgument(0));
      subscriptionEnd.await();
      return null;
    }).when(sentinelJedis).subscribe(any(JedisPubSub.class), anyString());
    SentinelConnectionFactory factory = mock(SentinelConnectionFactory.class);
    when(factory.createConnection(any(), any())).thenReturn(sentinelJedis);

    Set<HostAndPort> sentinels = new HashSet<>(Collections.singleton(SENTINEL));
    SentineledConnectionProvider provider = new SentineledConnectionProvider(MASTER_NAME,
        masterConfig, null, null, sentinels, DefaultJedisClientConfig.builder().build(), null,
        factory, null);
    try {
      Pool<?> oldPool = provider.getConnectionMap().get(OLD_MASTER);
      assertEquals(1, hooksOf(authXManager).size());

      subscriber.get(5, TimeUnit.SECONDS).onMessage("+switch-master",
        MASTER_NAME + " " + OLD_MASTER.getHost() + " " + OLD_MASTER.getPort() + " "
            + NEW_MASTER.getHost() + " " + NEW_MASTER.getPort());

      assertEquals(NEW_MASTER, provider.getCurrentMaster());
      assertNotSame(oldPool, provider.getConnectionMap().get(NEW_MASTER));
      assertTrue(oldPool.isClosed());
      assertEquals(1, hooksOf(authXManager).size(), "only the new master pool stays registered");

      provider.close();
      assertEquals(0, hooksOf(authXManager).size());
    } finally {
      provider.close();
      authXManager.stop();
    }
  }
}
