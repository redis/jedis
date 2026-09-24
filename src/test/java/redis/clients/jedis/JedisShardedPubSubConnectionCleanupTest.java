package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.jedis.Protocol.ResponseKeyword;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

public class JedisShardedPubSubConnectionCleanupTest {

  private Connection connection;
  private ConnectionPool pool;
  private ClusterConnectionProvider provider;

  @BeforeEach
  public void setUp() {
    // Keep the real broken flag and close() routing; stub only socket operations.
    connection = spy(new Connection());
    doNothing().when(connection).setTimeoutInfinite();
    doNothing().when(connection).rollbackTimeout();
    doNothing().when(connection).sendCommand(any(CommandArguments.class));
    doNothing().when(connection).flush();
    pool = mock(ConnectionPool.class);
    connection.setHandlingPool(pool);
    provider = mock(ClusterConnectionProvider.class);
    when(provider.getConnectionFromSlot(JedisClusterCRC16.getSlot("channel")))
        .thenReturn(connection);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenMessageCallbackThrows(boolean legacy) {
    RuntimeException failure = new IllegalStateException("callback failed");
    JedisShardedPubSub pubSub = messageCallback(() -> {
      throw failure;
    });
    doReturn(subscriptionReply(1), messageReply()).when(connection).getUnflushedObject();

    assertSame(failure, assertThrows(IllegalStateException.class, () -> subscribe(pubSub, legacy)));

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenMessageCallbackThrowsError(boolean legacy) {
    Error failure = new AssertionError("callback failed");
    JedisShardedPubSub pubSub = messageCallback(() -> {
      throw failure;
    });
    doReturn(subscriptionReply(1), messageReply()).when(connection).getUnflushedObject();

    assertSame(failure, assertThrows(AssertionError.class, () -> subscribe(pubSub, legacy)));

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenReadFailsBeforeSubscriptionReply(boolean legacy) {
    JedisDataException failure = new JedisDataException("subscription failed");
    doThrow(failure).when(connection).getUnflushedObject();
    JedisShardedPubSub pubSub = new JedisShardedPubSub() {
    };

    assertSame(failure, assertThrows(JedisDataException.class, () -> subscribe(pubSub, legacy)));
    assertFalse(pubSub.isSubscribed());

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenMessageTypeIsUnknown(boolean legacy) {
    doReturn(subscriptionReply(1), Collections.singletonList(SafeEncoder.encode("unknown")))
        .when(connection).getUnflushedObject();

    assertThrows(JedisException.class, () -> subscribe(new JedisShardedPubSub() {
    }, legacy));

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenInterruptedWhileSubscribed(boolean legacy) {
    JedisShardedPubSub pubSub = new JedisShardedPubSub() {
      @Override
      public void onSSubscribe(String channel, int subscribedChannels) {
        Thread.currentThread().interrupt();
      }
    };
    doReturn(subscriptionReply(1)).when(connection).getUnflushedObject();

    try {
      subscribe(pubSub, legacy);
      assertTrue(Thread.currentThread().isInterrupted());
      assertTrue(pubSub.isSubscribed());
      assertDiscarded();
    } finally {
      Thread.interrupted();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void returnsConnectionAfterUnsubscribeConfirmation(boolean legacy) {
    doReturn(subscriptionReply(1), subscriptionReply(0)).when(connection).getUnflushedObject();
    JedisShardedPubSub pubSub = new JedisShardedPubSub() {
    };

    subscribe(pubSub, legacy);

    assertFalse(pubSub.isSubscribed());
    assertFalse(connection.isBroken());
    assertFalse(connection.isActiveSubscription());
    verify(connection).rollbackTimeout();
    verify(pool).returnResource(connection);
    verify(pool, never()).returnBrokenResource(connection);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenBinaryMessageCallbackThrows(boolean legacy) {
    RuntimeException failure = new IllegalStateException("binary callback failed");
    BinaryJedisShardedPubSub pubSub = new BinaryJedisShardedPubSub() {
      @Override
      public void onSMessage(byte[] channel, byte[] message) {
        throw failure;
      }
    };
    doReturn(subscriptionReply(1), messageReply()).when(connection).getUnflushedObject();

    assertSame(failure, assertThrows(IllegalStateException.class, () -> {
      if (legacy) {
        try (JedisCluster client = new JedisCluster(provider, 1, Duration.ofSeconds(1))) {
          client.ssubscribe(pubSub, SafeEncoder.encode("channel"));
        }
      } else {
        try (RedisClusterClient client = clusterClient()) {
          client.ssubscribe(pubSub, SafeEncoder.encode("channel"));
        }
      }
    }));

    assertDiscarded();
  }

  private void subscribe(JedisShardedPubSub pubSub, boolean legacy) {
    if (legacy) {
      try (JedisCluster client = new JedisCluster(provider, 1, Duration.ofSeconds(1))) {
        client.ssubscribe(pubSub, "channel");
      }
    } else {
      try (RedisClusterClient client = clusterClient()) {
        client.ssubscribe(pubSub, "channel");
      }
    }
  }

  private RedisClusterClient clusterClient() {
    return RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", 6379)))
        .connectionProvider(provider).build();
  }

  private void assertDiscarded() {
    assertTrue(connection.isBroken());
    assertFalse(connection.isActiveSubscription());
    verify(connection).rollbackTimeout();
    verify(pool).returnBrokenResource(connection);
    verify(pool, never()).returnResource(connection);
  }

  private static JedisShardedPubSub messageCallback(Runnable callback) {
    return new JedisShardedPubSub() {
      @Override
      public void onSMessage(String channel, String message) {
        callback.run();
      }
    };
  }

  private static List<Object> subscriptionReply(int count) {
    ResponseKeyword keyword = count == 0 ? ResponseKeyword.SUNSUBSCRIBE
        : ResponseKeyword.SSUBSCRIBE;
    return Arrays.asList(keyword.getRaw(), SafeEncoder.encode("channel"), (long) count);
  }

  private static List<Object> messageReply() {
    return Arrays.asList(ResponseKeyword.SMESSAGE.getRaw(), SafeEncoder.encode("channel"),
      SafeEncoder.encode("message"));
  }
}
