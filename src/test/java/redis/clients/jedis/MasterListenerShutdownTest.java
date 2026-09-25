package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class MasterListenerShutdownTest {

  private JedisSentinelPool.MasterListener listener;

  @BeforeEach
  void setUp() {
    JedisSentinelPool pool = mock(JedisSentinelPool.class);
    listener = pool.new MasterListener("mymaster", "localhost", 26379);
  }

  private void stopOnSubscribe(Jedis jedis) {
    // End the loop even on the broken implementation so the test cannot hang.
    doAnswer(invocation -> {
      listener.shutdown();
      return null;
    }).when(jedis).subscribe(any(JedisPubSub.class), anyString());
  }

  @Test
  void shutdownBeforeRunMustNotOpenConnection() {
    try (MockedConstruction<Jedis> connections = mockConstruction(Jedis.class,
      (jedis, context) -> stopOnSubscribe(jedis))) {
      listener.shutdown();

      listener.run();

      assertEquals(0, connections.constructed().size());
    }
  }

  @Test
  void shutdownDuringConnectionCreationClosesConnectionWithoutSubscribing() {
    try (MockedConstruction<Jedis> connections = mockConstruction(Jedis.class, (jedis, context) -> {
      listener.shutdown();
      stopOnSubscribe(jedis);
    })) {
      listener.run();

      assertEquals(1, connections.constructed().size());
      Jedis jedis = connections.constructed().get(0);
      verify(jedis).close();
      verifyNoMoreInteractions(jedis);
    }
  }

  @Test
  void runningListenerStillSubscribes() {
    try (MockedConstruction<Jedis> connections = mockConstruction(Jedis.class,
      (jedis, context) -> stopOnSubscribe(jedis))) {
      listener.run();

      assertEquals(1, connections.constructed().size());
      verify(connections.constructed().get(0)).subscribe(any(JedisPubSub.class), anyString());
    }
  }
}
