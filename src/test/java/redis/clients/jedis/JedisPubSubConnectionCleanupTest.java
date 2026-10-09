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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.jedis.Protocol.ResponseKeyword;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.SafeEncoder;

public class JedisPubSubConnectionCleanupTest {

  private Connection connection;
  private ConnectionPool pool;
  private UnifiedJedis jedis;

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
    ConnectionProvider provider = mock(ConnectionProvider.class);
    when(provider.getConnection()).thenReturn(connection);
    jedis = new UnifiedJedis(provider, RedisProtocol.RESP2);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenMessageCallbackThrows(boolean patterns) {
    RuntimeException failure = new IllegalStateException("callback failed");
    JedisPubSub pubSub = messageCallback(() -> {
      throw failure;
    });
    doReturn(subscriptionReply(patterns, 1), messageReply(patterns)).when(connection)
        .getUnflushedObject();

    assertSame(failure,
      assertThrows(IllegalStateException.class, () -> subscribe(pubSub, patterns)));

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenMessageCallbackThrowsError(boolean patterns) {
    Error failure = new AssertionError("callback failed");
    JedisPubSub pubSub = messageCallback(() -> {
      throw failure;
    });
    doReturn(subscriptionReply(patterns, 1), messageReply(patterns)).when(connection)
        .getUnflushedObject();

    assertSame(failure, assertThrows(AssertionError.class, () -> subscribe(pubSub, patterns)));

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenReadFailsBeforeSubscriptionReply(boolean patterns) {
    JedisDataException failure = new JedisDataException("subscription failed");
    doThrow(failure).when(connection).getUnflushedObject();
    JedisPubSub pubSub = new JedisPubSub() {
    };

    assertSame(failure, assertThrows(JedisDataException.class, () -> subscribe(pubSub, patterns)));
    assertFalse(pubSub.isSubscribed());

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenMessageTypeIsUnknown(boolean patterns) {
    doReturn(subscriptionReply(patterns, 1),
      Collections.singletonList(SafeEncoder.encode("unknown"))).when(connection)
          .getUnflushedObject();

    assertThrows(JedisException.class, () -> subscribe(new JedisPubSub() {
    }, patterns));

    assertDiscarded();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void discardsConnectionWhenInterruptedWhileSubscribed(boolean patterns) {
    JedisPubSub pubSub = new JedisPubSub() {
      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        Thread.currentThread().interrupt();
      }

      @Override
      public void onPSubscribe(String pattern, int subscribedChannels) {
        Thread.currentThread().interrupt();
      }
    };
    doReturn(subscriptionReply(patterns, 1)).when(connection).getUnflushedObject();

    try {
      subscribe(pubSub, patterns);
      assertTrue(Thread.currentThread().isInterrupted());
      assertTrue(pubSub.isSubscribed());
      assertDiscarded();
    } finally {
      Thread.interrupted();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void returnsConnectionAfterUnsubscribeConfirmation(boolean patterns) {
    JedisPubSub pubSub = new JedisPubSub() {
    };
    doReturn(subscriptionReply(patterns, 1), subscriptionReply(patterns, 0)).when(connection)
        .getUnflushedObject();

    subscribe(pubSub, patterns);

    assertFalse(pubSub.isSubscribed());
    assertFalse(connection.isBroken());
    assertFalse(connection.isActiveSubscription());
    verify(connection).rollbackTimeout();
    verify(pool).returnResource(connection);
    verify(pool, never()).returnBrokenResource(connection);
  }

  private void subscribe(JedisPubSub pubSub, boolean patterns) {
    if (patterns) {
      jedis.psubscribe(pubSub, "channel*");
    } else {
      jedis.subscribe(pubSub, "channel");
    }
  }

  private void assertDiscarded() {
    assertTrue(connection.isBroken());
    assertFalse(connection.isActiveSubscription());
    verify(connection).rollbackTimeout();
    verify(pool).returnBrokenResource(connection);
    verify(pool, never()).returnResource(connection);
  }

  private static JedisPubSub messageCallback(Runnable callback) {
    return new JedisPubSub() {
      @Override
      public void onMessage(String channel, String message) {
        callback.run();
      }

      @Override
      public void onPMessage(String pattern, String channel, String message) {
        callback.run();
      }
    };
  }

  private static List<Object> subscriptionReply(boolean patterns, int count) {
    ResponseKeyword keyword = patterns
        ? (count == 0 ? ResponseKeyword.PUNSUBSCRIBE : ResponseKeyword.PSUBSCRIBE)
        : (count == 0 ? ResponseKeyword.UNSUBSCRIBE : ResponseKeyword.SUBSCRIBE);
    return Arrays.asList(keyword.getRaw(), SafeEncoder.encode(patterns ? "channel*" : "channel"),
      (long) count);
  }

  private static List<Object> messageReply(boolean patterns) {
    if (patterns) {
      return Arrays.asList(ResponseKeyword.PMESSAGE.getRaw(), SafeEncoder.encode("channel*"),
        SafeEncoder.encode("channel"), SafeEncoder.encode("message"));
    }
    return Arrays.asList(ResponseKeyword.MESSAGE.getRaw(), SafeEncoder.encode("channel"),
      SafeEncoder.encode("message"));
  }
}
