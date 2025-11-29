package redis.clients.jedis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.Pool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JedisPooledUnitTest {

  private JedisPooled pooled;
  private Pool<Connection> mockPool;
  private PooledConnectionProvider mockProvider;
  private Connection mockConnection;

  private AtomicInteger pings = new AtomicInteger(0);

  @BeforeEach
  public void setUp() throws Exception {
    mockConnection = mock(Connection.class);
    Mockito.doAnswer( ioc -> {
      pings.incrementAndGet();
      return null;
    }).when(mockConnection).sendCommand(eq(Protocol.Command.PING));
    when(mockConnection.getStatusCodeReply()).thenAnswer( ioc -> "PONG" + System.currentTimeMillis());
    mockPool = mock(Pool.class);
    mockProvider = mock(PooledConnectionProvider.class);
    when(mockProvider.getPool()).thenReturn(mockPool);
    when(mockPool.getResource()).thenReturn(mockConnection);
    pooled = spy(new JedisPooled(mockProvider));
  }

  @AfterEach
  public void tearDown() {
    if (pooled != null) {
      pooled.close();
    }
  }

  @Test
  public void testWithResourceClosesConnection() {
    pooled.withResource(jedis -> assertEquals(mockConnection, jedis.getConnection()));

    verify(pooled, times(1)).getPool();
    verify(mockPool, times(1)).returnResource(eq(mockConnection));
  }

  @Test
  public void testWithResourceGetClosesConnection() {
    String result = pooled.withResourceGet(Jedis::ping);
    assertNotNull(result);
    assertEquals(1, pings.get());
    verify(pooled, times(1)).getPool();
    verify(mockPool, times(1)).returnResource(eq(mockConnection));
  }

  @Test
  public void testWithResourceGetReturnsConnection() {
    Jedis jedis = pooled.getResource();
    String result = jedis.ping();
    pooled.returnResource(jedis);
    assertNotNull(result);
    assertEquals(1, pings.get());
    verify(pooled, times(2)).getPool();
    verify(mockPool, times(1)).returnResource(eq(mockConnection));
  }

  @Test
  public void testWithResourceGetSetData() {
    AtomicReference<String> settedValue = new AtomicReference<>("");
    try(MockedConstruction<Jedis> mockConstruction = Mockito.mockConstruction(Jedis.class,(mock, context)->
            when(mock.set(eq("test-key"), anyString())).thenAnswer( ioc -> {
              settedValue.set(ioc.getArgument(1));
              return "OK";
            })
    )) {
      pooled.withResource( jedis -> jedis.set("test-key","test-result"));
      assertEquals("test-result", settedValue.get());
      verify(pooled, times(1)).getPool();
      Jedis mockJedis = mockConstruction.constructed().get(0);
      verify(mockJedis, times(1)).close();
    }
  }

  @Test
  public void testWithResourceGetReturnsResult() {
    try(MockedConstruction<Jedis> mockConstruction = Mockito.mockConstruction(Jedis.class,(mock, context)->
      when(mock.get(eq("test-key"))).thenReturn("test-result")
    )) {
      String result = pooled.withResourceGet( jedis -> jedis.get("test-key"));
      assertEquals("test-result", result);
      verify(pooled, times(1)).getPool();
      Jedis mockJedis = mockConstruction.constructed().get(0);
      verify(mockJedis, times(1)).close();
    }
  }

}
