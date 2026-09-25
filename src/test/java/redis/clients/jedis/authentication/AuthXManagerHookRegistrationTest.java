package redis.clients.jedis.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.authentication.core.TokenManager;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * A pool registers a post-authentication hook on its {@link AuthXManager} and must unregister it
 * when torn down, without touching the lifecycle of the manager itself.
 */
public class AuthXManagerHookRegistrationTest {

  // no connection is ever borrowed, so no Redis is needed
  private static final HostAndPort HNP = new HostAndPort("localhost", 6379);

  private static JedisClientConfig configWith(AuthXManager authXManager) {
    return DefaultJedisClientConfig.builder().authXManager(authXManager).build();
  }

  private static List<?> hooksOf(AuthXManager authXManager) {
    return ReflectionTestUtil.getField(authXManager, "postAuthenticateHooks");
  }

  @Test
  public void destroyUnregistersHookAndLeavesManagerRunning() {
    TokenManager tokenManager = mock(TokenManager.class);
    AuthXManager authXManager = new AuthXManager(tokenManager);

    ConnectionPool pool = new ConnectionPool(HNP, configWith(authXManager));
    assertEquals(1, hooksOf(authXManager).size());

    pool.destroy();
    assertEquals(0, hooksOf(authXManager).size(), "hook of a destroyed pool must be removed");

    // repeated teardown is harmless
    pool.destroy();
    pool.close();
    assertEquals(0, hooksOf(authXManager).size());
    verify(tokenManager, never()).stop();
  }

  @Test
  public void closeUnregistersOnlyTheClosedPool() {
    TokenManager tokenManager = mock(TokenManager.class);
    AuthXManager authXManager = new AuthXManager(tokenManager);
    JedisClientConfig config = configWith(authXManager);

    ConnectionPool first = new ConnectionPool(HNP, config);
    ConnectionPool second = new ConnectionPool(new HostAndPort("localhost", 6380), config);
    assertEquals(2, hooksOf(authXManager).size());

    first.close();
    assertEquals(1, hooksOf(authXManager).size());

    second.close();
    assertEquals(0, hooksOf(authXManager).size());
    verify(tokenManager, never()).stop();
  }

  @Test
  public void poolCreatedAfterAnotherWasClosedRegistersOnce() {
    AuthXManager authXManager = new AuthXManager(mock(TokenManager.class));
    JedisClientConfig config = configWith(authXManager);

    new ConnectionPool(HNP, config).close();
    ConnectionPool recreated = new ConnectionPool(HNP, config);

    assertEquals(1, hooksOf(authXManager).size());
    recreated.close();
    assertEquals(0, hooksOf(authXManager).size());
  }
}
