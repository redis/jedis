package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

class SentinelListenerShutdownTest {

  private SentineledConnectionProvider.SentinelConnectionFactory factory;
  private SentineledConnectionProvider.SentinelListener listener;
  private Jedis jedis;

  @BeforeEach
  void setUp() throws Exception {
    SentineledConnectionProvider provider = mock(SentineledConnectionProvider.class);
    factory = mock(SentineledConnectionProvider.SentinelConnectionFactory.class);
    Field field = SentineledConnectionProvider.class.getDeclaredField("sentinelConnectionFactory");
    field.setAccessible(true);
    field.set(provider, factory);
    listener = provider.new SentinelListener(new HostAndPort("localhost", 26379));
    jedis = mock(Jedis.class);
    // End the loop even on the broken implementation so the test cannot hang.
    doAnswer(invocation -> {
      listener.shutdown();
      return null;
    }).when(jedis).subscribe(any(JedisPubSub.class), anyString());
  }

  @Test
  void shutdownBeforeRunMustNotOpenConnection() {
    when(factory.createConnection(any(), any())).thenReturn(jedis);
    listener.shutdown();

    // Reproduce start() being scheduled only after the caller has closed the provider.
    assertDoesNotThrow(listener::run);

    verifyNoInteractions(factory, jedis);
  }

  @Test
  void shutdownDuringConnectionCreationClosesConnectionWithoutSubscribing() {
    when(factory.createConnection(any(), any())).thenAnswer(invocation -> {
      listener.shutdown();
      return jedis;
    });

    listener.run();

    verify(jedis).close();
    verifyNoMoreInteractions(jedis);
  }

  @Test
  void runningListenerStillSubscribes() {
    when(factory.createConnection(any(), any())).thenReturn(jedis);

    listener.run();

    verify(jedis).subscribe(any(JedisPubSub.class), anyString());
  }
}
