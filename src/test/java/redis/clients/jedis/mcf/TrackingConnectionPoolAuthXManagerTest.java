package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenManagerConfig;
import redis.clients.authentication.core.TokenManagerConfig.RetryPolicy;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Fast failover closes a {@link TrackingConnectionPool} and later recreates it from the same
 * config; the closed pool must not stay registered on the {@link AuthXManager}.
 */
public class TrackingConnectionPoolAuthXManagerTest {

  private static final HostAndPort HNP = new HostAndPort("localhost", 6379);

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
  public void closeUnregistersHook() {
    AuthXManager authXManager = authXManagerWithStaticToken();
    try {
      JedisClientConfig config = DefaultJedisClientConfig.builder().authXManager(authXManager)
          .build();

      TrackingConnectionPool pool = TrackingConnectionPool.builder().hostAndPort(HNP)
          .clientConfig(config).build();
      assertEquals(1, hooksOf(authXManager).size());

      pool.close();
      assertEquals(0, hooksOf(authXManager).size());
    } finally {
      authXManager.stop();
    }
  }

  @Test
  public void forceDisconnectAndRecreateRegistersExactlyOnce() {
    AuthXManager authXManager = authXManagerWithStaticToken();
    try {
      JedisClientConfig config = DefaultJedisClientConfig.builder().authXManager(authXManager)
          .build();

      TrackingConnectionPool pool = TrackingConnectionPool.builder().hostAndPort(HNP)
          .clientConfig(config).build();
      pool.forceDisconnect();
      assertTrue(pool.isClosed());
      assertEquals(0, hooksOf(authXManager).size(), "closed pool must not stay registered");

      TrackingConnectionPool recreated = TrackingConnectionPool.from(pool);
      assertEquals(1, hooksOf(authXManager).size(), "recreated pool registers exactly once");

      recreated.close();
      assertEquals(0, hooksOf(authXManager).size());
    } finally {
      authXManager.stop();
    }
  }
}
