package redis.clients.jedis.authentication;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenManager;
import redis.clients.authentication.core.TokenManagerConfig;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;

public class TokenBasedAuthenticationUnitTests {
  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

  @Test
  public void testJedisAuthXManager() throws Exception {

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken())
        .thenReturn(new SimpleToken("password", System.currentTimeMillis() + 100000,
            System.currentTimeMillis(), Collections.singletonMap("oid", "default")));

    TokenManager tokenManager = new TokenManager(idProvider,
        new TokenManagerConfig(0.5F, 1000, 1000, null));
    JedisAuthXManager jedisAuthXManager = new JedisAuthXManager(tokenManager);

    AtomicInteger numberOfEvictions = new AtomicInteger(0);
    ConnectionPool pool = spy(new ConnectionPool(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), jedisAuthXManager) {
      @Override
      public void evict() throws Exception {
        numberOfEvictions.incrementAndGet();
        super.evict();
      }
    });

    jedisAuthXManager.start(true);

    assertEquals(1, numberOfEvictions.get());
  }
}
