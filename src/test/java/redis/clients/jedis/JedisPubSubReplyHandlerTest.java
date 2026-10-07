package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.Token;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.ResponseKeyword;
import redis.clients.jedis.authentication.AuthXEventListener;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

@Timeout(15)
public class JedisPubSubReplyHandlerTest {

  private final Connection connection = mock(Connection.class);
  private final AuthXManager authXManager = mock(AuthXManager.class);
  private final AuthXEventListener listener = mock(AuthXEventListener.class);
  private final BlockingQueue<Object> replies = new LinkedBlockingQueue<>();
  private final CountDownLatch subscribed = new CountDownLatch(1);
  private final CountDownLatch replyProcessed = new CountDownLatch(1);
  private final AtomicReference<Consumer<Token>> authHook = new AtomicReference<>();
  private final AtomicBoolean pongReceived = new AtomicBoolean();
  private final AtomicReference<String> pong = new AtomicReference<>();
  private final AtomicBoolean failNextPing = new AtomicBoolean();
  private final JedisException pingFailure = new JedisException("send failed");
  private boolean renewDuringPing;
  private String pongReply = "PONG";

  @BeforeEach
  public void setUp() {
    when(connection.getRedisProtocol()).thenReturn(RedisProtocol.RESP3);
    when(connection.getAuthXManager()).thenReturn(authXManager);
    when(authXManager.getListener()).thenReturn(listener);
    when(connection.encodeToBytes(any(char[].class))).thenAnswer(
      invocation -> SafeEncoder.encode(new String((char[]) invocation.getArgument(0))));
    doAnswer(invocation -> {
      authHook.set(invocation.getArgument(0));
      return null;
    }).when(authXManager).addPostAuthenticationHook(any());

    AtomicReference<Command> command = new AtomicReference<>();
    doAnswer(invocation -> {
      command.set((Command) invocation.getArgument(0, CommandArguments.class).getCommand());
      if (command.get() == Command.PING && failNextPing.getAndSet(false)) {
        throw pingFailure;
      }
      return null;
    }).when(connection).sendCommand(any(CommandArguments.class));
    doAnswer(invocation -> {
      Command sent = command.get();
      switch (sent) {
        case SUBSCRIBE:
        case PSUBSCRIBE:
        case SSUBSCRIBE:
          replies.add(subscriptionReply(sent, 1));
          break;
        case UNSUBSCRIBE:
        case PUNSUBSCRIBE:
        case SUNSUBSCRIBE:
          replies.add(subscriptionReply(sent, 0));
          break;
        case AUTH:
        case PING:
          if (sent == Command.PING && renewDuringPing) {
            authHook.get().accept(new SimpleToken("default", "password", 0, 0, null));
          }
          replies.add(SafeEncoder.encode(sent == Command.AUTH ? "OK" : pongReply));
          // Let the reader dispatch the reply before flush returns to the sender.
          assertTrue(replyProcessed.await(5, TimeUnit.SECONDS));
          break;
        default:
          break;
      }
      return null;
    }).when(connection).flush();

    AtomicBoolean replyRead = new AtomicBoolean();
    when(connection.getUnflushedObject()).thenAnswer(invocation -> {
      if (replyRead.getAndSet(false)) {
        replyProcessed.countDown();
      }
      Object reply = replies.poll(5, TimeUnit.SECONDS);
      assertNotNull(reply, "No subscription reply was queued");
      replyRead.set(reply instanceof byte[]);
      return reply;
    });
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void handlesAuthReplyBeforeSendReturns(boolean patterns) throws Exception {
    JedisPubSub pubSub = stringPubSub();

    runSubscription(pubSub, patterns ? "channel*" : "channel", patterns,
      () -> authHook.get().accept(new SimpleToken("default", "password", 0, 0, null)));

    verify(listener, never()).onConnectionAuthenticationError(any());
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void handlesShardedAuthReplyBeforeSendReturns(boolean binary) throws Exception {
    Runnable renew = () -> authHook.get()
        .accept(new SimpleToken("default", "password", 0, 0, null));
    if (binary) {
      BinaryJedisShardedPubSub pubSub = new BinaryJedisShardedPubSub() {
        @Override
        public void onSSubscribe(byte[] channel, int count) {
          subscribed.countDown();
        }
      };
      runSubscription(() -> pubSub.proceed(connection, SafeEncoder.encode("channel")), renew,
        pubSub::sunsubscribe, pubSub::getSubscribedChannels);
    } else {
      JedisShardedPubSub pubSub = new JedisShardedPubSub() {
        @Override
        public void onSSubscribe(String channel, int count) {
          subscribed.countDown();
        }
      };
      runSubscription(() -> pubSub.proceed(connection, "channel"), renew, pubSub::sunsubscribe,
        pubSub::getSubscribedChannels);
    }

    verify(listener, never()).onConnectionAuthenticationError(any());
  }

  @ParameterizedTest
  @CsvSource({ "false, false, false", "false, true, false", "true, false, false",
      "true, true, false", "false, false, true", "false, true, true", "true, false, true",
      "true, true, true" })
  public void deliversPongBeforeSendReturns(boolean binary, boolean argument, boolean patterns)
      throws Exception {
    pongReply = argument ? "payload" : "PONG";
    if (binary) {
      BinaryJedisPubSub pubSub = binaryPubSub();
      runSubscription(pubSub, SafeEncoder.encode(patterns ? "channel*" : "channel"), patterns,
        () -> {
          if (argument) {
            pubSub.ping(SafeEncoder.encode("payload"));
          } else {
            pubSub.ping();
          }
        });
    } else {
      JedisPubSub pubSub = stringPubSub();
      runSubscription(pubSub, patterns ? "channel*" : "channel", patterns, () -> {
        if (argument) {
          pubSub.ping("payload");
        } else {
          pubSub.ping();
        }
      });
    }

    assertTrue(pongReceived.get());
    assertEquals(argument ? "payload" : null, pong.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void failedPingDoesNotConsumeNextAuthReply(boolean argument) throws Exception {
    JedisPubSub pubSub = stringPubSub();
    runSubscription(pubSub, "channel", false, () -> {
      failNextPing.set(true);
      assertSame(pingFailure, assertThrows(JedisException.class, () -> {
        if (argument) {
          pubSub.ping("payload");
        } else {
          pubSub.ping();
        }
      }));
      authHook.get().accept(new SimpleToken("default", "password", 0, 0, null));
    });

    assertFalse(pongReceived.get());
    verify(listener, never()).onConnectionAuthenticationError(any());
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void handlesRenewalQueuedDuringPing(boolean argument) throws Exception {
    renewDuringPing = true;
    pongReply = argument ? "payload" : "PONG";
    JedisPubSub pubSub = stringPubSub();
    runSubscription(pubSub, "channel", false, () -> {
      if (argument) {
        pubSub.ping("payload");
      } else {
        pubSub.ping();
      }
    });

    assertTrue(pongReceived.get());
    assertEquals(argument ? "payload" : null, pong.get());
    verify(listener, never()).onConnectionAuthenticationError(any());
  }

  private <T> void runSubscription(JedisPubSubBase<T> pubSub, T channel, boolean patterns,
      Runnable send) throws Exception {
    runSubscription(() -> {
      if (patterns) {
        pubSub.proceedWithPatterns(connection, channel);
      } else {
        pubSub.proceed(connection, channel);
      }
    }, send, () -> {
      if (patterns) {
        pubSub.punsubscribe();
      } else {
        pubSub.unsubscribe();
      }
    }, pubSub::getSubscribedChannels);
  }

  private void runSubscription(Runnable subscribe, Runnable send, Runnable unsubscribe,
      IntSupplier subscribedChannels) throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> subscription = executor.submit(() -> {
      try {
        subscribe.run();
      } finally {
        replyProcessed.countDown();
      }
    });
    try {
      assertTrue(subscribed.await(5, TimeUnit.SECONDS));
      send.run();
      assertEquals(0, replyProcessed.getCount(), "The command reply was not dispatched");
      unsubscribe.run();
      subscription.get(5, TimeUnit.SECONDS);
      assertEquals(0, subscribedChannels.getAsInt());
    } finally {
      subscription.cancel(true);
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private JedisPubSub stringPubSub() {
    return new JedisPubSub() {
      @Override
      public void onSubscribe(String channel, int count) {
        subscribed.countDown();
      }

      @Override
      public void onPSubscribe(String pattern, int count) {
        subscribed.countDown();
      }

      @Override
      public void onPong(String value) {
        pong.set(value);
        pongReceived.set(true);
      }
    };
  }

  private BinaryJedisPubSub binaryPubSub() {
    return new BinaryJedisPubSub() {
      @Override
      public void onSubscribe(byte[] channel, int count) {
        subscribed.countDown();
      }

      @Override
      public void onPSubscribe(byte[] pattern, int count) {
        subscribed.countDown();
      }

      @Override
      public void onPong(byte[] value) {
        pong.set(value == null ? null : SafeEncoder.encode(value));
        pongReceived.set(true);
      }
    };
  }

  private static Object subscriptionReply(Command command, int count) {
    if (command == Command.SSUBSCRIBE || command == Command.SUNSUBSCRIBE) {
      ResponseKeyword keyword = count == 0 ? ResponseKeyword.SUNSUBSCRIBE
          : ResponseKeyword.SSUBSCRIBE;
      return Arrays.asList(keyword.getRaw(), SafeEncoder.encode("channel"), (long) count);
    }
    boolean patterns = command == Command.PSUBSCRIBE || command == Command.PUNSUBSCRIBE;
    ResponseKeyword keyword = patterns
        ? (count == 0 ? ResponseKeyword.PUNSUBSCRIBE : ResponseKeyword.PSUBSCRIBE)
        : (count == 0 ? ResponseKeyword.UNSUBSCRIBE : ResponseKeyword.SUBSCRIBE);
    return Arrays.asList(keyword.getRaw(), SafeEncoder.encode(patterns ? "channel*" : "channel"),
      (long) count);
  }
}
