package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JedisProviderTest {

  private TestJedisProvider provider = spy(new TestJedisProvider());

  @Test
  public void testWithResourceClosesConnection() {
    provider.withResource(Jedis::ping);

    verify(provider, times(1)).getResource();
    assertEquals(1, provider.getBorrowed());
    assertEquals(1, provider.getClosed());
  }

  @Test
  public void testWithResourceGetClosesConnection() {
    String result = provider.withResourceGet(jedis -> jedis.ping());

    verify(provider, times(1)).getResource();
    assertEquals(1, provider.getBorrowed());
    assertEquals(1, provider.getClosed());
    assertNotNull(result);
  }

  @Test
  public void testWithSomeResources() {
    provider.withResource(Jedis::ping);
    String result = provider.withResourceGet(Jedis::ping);
    provider.withResource(Jedis::ping);
    Jedis jedis = provider.getResource();
    assertEquals(4, provider.getBorrowed());
    assertEquals(3, provider.getClosed());
    assertNotNull(result);
    jedis.close();
    assertEquals(4, provider.getClosed());
  }

  @Test
  public void tesReturntWithSomeResources() {
    provider.withResource(Jedis::ping);
    String result = provider.withResourceGet(Jedis::ping);
    provider.withResource(Jedis::ping);
    Jedis jedis = provider.getResource();
    assertEquals(4, provider.getBorrowed());
    assertEquals(3, provider.getClosed());
    assertNotNull(result);
    provider.returnResource(jedis);
    assertEquals(4, provider.getClosed());
  }

  static class TestJedisProvider implements JedisProvider {

    private Jedis jedis;
    private AtomicInteger borrowed = new AtomicInteger(0);
    private AtomicInteger closed = new AtomicInteger(0);

    TestJedisProvider() {
      jedis = mock(Jedis.class);
      Mockito.doAnswer(ioc -> {
        closed.incrementAndGet();
        return null;
      }).when(jedis).close();
      when(jedis.ping()).thenAnswer(ioc -> "PONG_" + System.nanoTime());
    }

    @Override
    public Jedis getResource() {
      borrowed.incrementAndGet();
      return jedis;
    }

    @Override
    public void returnResource(Jedis resource) {
      jedis.close();
    }

    public int getBorrowed() {
      return borrowed.get();
    }

    public int getClosed() {
      return closed.get();
    }

  }
}
