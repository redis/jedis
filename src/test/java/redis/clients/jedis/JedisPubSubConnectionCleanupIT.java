package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.JedisClusterCRC16;

public class JedisPubSubConnectionCleanupIT {

  private enum SubscriptionType {
    SUBSCRIBE, PSUBSCRIBE, SSUBSCRIBE, LEGACY_SSUBSCRIBE;

    boolean isSharded() {
      return this == SSUBSCRIBE || this == LEGACY_SSUBSCRIBE;
    }
  }

  private enum ExitMode {
    CALLBACK_FAILURE, INTERRUPT, UNSUBSCRIBE
  }

  private static Stream<Arguments> subscriptions() {
    return Arrays.stream(SubscriptionType.values())
        .flatMap(type -> Stream.of(RedisProtocol.RESP2, RedisProtocol.RESP3)
            .map(protocol -> Arguments.of(type, protocol)));
  }

  @ParameterizedTest
  @MethodSource("subscriptions")
  public void discardsConnectionAfterCallbackFailure(SubscriptionType type, RedisProtocol protocol)
      throws Exception {
    assertCleanup(type, protocol, ExitMode.CALLBACK_FAILURE);
  }

  @ParameterizedTest
  @MethodSource("subscriptions")
  public void discardsConnectionAfterInterruptedCallback(SubscriptionType type,
      RedisProtocol protocol) throws Exception {
    assertCleanup(type, protocol, ExitMode.INTERRUPT);
  }

  @ParameterizedTest
  @MethodSource("subscriptions")
  public void reusesConnectionAfterUnsubscribeConfirmation(SubscriptionType type,
      RedisProtocol protocol) throws Exception {
    assertCleanup(type, protocol, ExitMode.UNSUBSCRIBE);
  }

  private void assertCleanup(SubscriptionType type, RedisProtocol protocol, ExitMode exit)
      throws Exception {
    EndpointConfig endpoint = Endpoints
        .getRedisEndpoint(type.isSharded() ? "cluster-stable" : "standalone0");
    JedisClientConfig config = endpoint.getClientConfigBuilder().protocol(protocol)
        .connectionTimeoutMillis(2000).socketTimeoutMillis(2000).build();
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);
    poolConfig.setMaxIdle(1);
    poolConfig.setMinIdle(0);
    // Pool validation must not hide a connection returned in subscriber mode.
    poolConfig.setTestOnBorrow(false);
    poolConfig.setTestOnReturn(false);
    poolConfig.setTestWhileIdle(false);

    ConnectionProvider provider = type.isSharded()
        ? new ClusterConnectionProvider(new HashSet<>(endpoint.getHostsAndPorts()), config,
            poolConfig)
        : new PooledConnectionProvider(endpoint.getHostAndPort(), config, poolConfig);

    try (UnifiedJedis client = createClient(type, endpoint, config, provider)) {
      String channel = "jedis-4722-" + UUID.randomUUID();
      Connection original;
      try (Connection connection = borrow(provider, type, channel)) {
        assertEquals(protocol, connection.getRedisProtocol());
        original = connection;
      }

      CountDownLatch subscribed = new CountDownLatch(1);
      RuntimeException failure = new IllegalStateException("callback failed");
      AtomicReference<Throwable> thrown = new AtomicReference<>();
      AtomicBoolean interrupted = new AtomicBoolean();
      Runnable subscription = subscription(type, client, channel, subscribed, exit, failure);
      Thread listener = new Thread(() -> {
        try {
          subscription.run();
        } catch (Throwable t) {
          thrown.set(t);
        } finally {
          interrupted.set(Thread.currentThread().isInterrupted());
        }
      }, "jedis-pubsub-cleanup");
      listener.setDaemon(true);

      try (Jedis publisher = new Jedis(original.getHostAndPort(), config)) {
        listener.start();
        try {
          assertTrue(subscribed.await(5, TimeUnit.SECONDS), "listener did not subscribe");
          Command publish = type.isSharded() ? Command.SPUBLISH : Command.PUBLISH;
          long receivers = (Long) publisher.sendCommand(publish, channel, "message");
          assertEquals(1L, receivers);
          listener.join(5000);
          assertFalse(listener.isAlive(), "listener did not exit");
          if (exit == ExitMode.CALLBACK_FAILURE) {
            assertSame(failure, thrown.get());
          } else {
            assertNull(thrown.get());
          }
          assertEquals(exit == ExitMode.INTERRUPT, interrupted.get());

          // A unique key on the channel's slot also exercises the next borrower's read path.
          assertNull(client.get(channel));
          try (Connection next = borrow(provider, type, channel)) {
            if (exit == ExitMode.UNSUBSCRIBE) {
              assertSame(original, next);
            } else {
              assertNotSame(original, next);
              assertTrue(original.isBroken());
            }
            assertFalse(next.isBroken());
            assertFalse(next.isActiveSubscription());
          }
          assertNull(client.get(channel));
        } finally {
          if (listener.isAlive()) {
            // Interrupt alone cannot unblock a socket read after a failed assertion.
            original.forceDisconnect();
            listener.join(5000);
          }
        }
      }
    }
  }

  private UnifiedJedis createClient(SubscriptionType type, EndpointConfig endpoint,
      JedisClientConfig config, ConnectionProvider provider) {
    if (type == SubscriptionType.LEGACY_SSUBSCRIBE) {
      return new JedisCluster((ClusterConnectionProvider) provider, 1, Duration.ofSeconds(2));
    }
    if (type == SubscriptionType.SSUBSCRIBE) {
      return RedisClusterClient.builder().nodes(new HashSet<>(endpoint.getHostsAndPorts()))
          .clientConfig(config).connectionProvider(provider).maxAttempts(1).build();
    }
    return new UnifiedJedis(provider, config.getRedisProtocol());
  }

  private Connection borrow(ConnectionProvider provider, SubscriptionType type, String channel) {
    return type.isSharded()
        ? ((ClusterConnectionProvider) provider)
            .getConnectionFromSlot(JedisClusterCRC16.getSlot(channel))
        : provider.getConnection();
  }

  private Runnable subscription(SubscriptionType type, UnifiedJedis client, String channel,
      CountDownLatch subscribed, ExitMode exit, RuntimeException failure) {
    if (type.isSharded()) {
      JedisShardedPubSub pubSub = new JedisShardedPubSub() {
        @Override
        public void onSSubscribe(String channel, int subscribedChannels) {
          subscribed.countDown();
        }

        @Override
        public void onSMessage(String channel, String message) {
          exit(exit, failure, this::sunsubscribe);
        }
      };
      return type == SubscriptionType.LEGACY_SSUBSCRIBE
          ? () -> ((JedisCluster) client).ssubscribe(pubSub, channel)
          : () -> ((RedisClusterClient) client).ssubscribe(pubSub, channel);
    }

    JedisPubSub pubSub = new JedisPubSub() {
      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        subscribed.countDown();
      }

      @Override
      public void onPSubscribe(String pattern, int subscribedChannels) {
        subscribed.countDown();
      }

      @Override
      public void onMessage(String channel, String message) {
        exit(exit, failure, this::unsubscribe);
      }

      @Override
      public void onPMessage(String pattern, String channel, String message) {
        exit(exit, failure, this::punsubscribe);
      }
    };
    return type == SubscriptionType.PSUBSCRIBE ? () -> client.psubscribe(pubSub, channel + "*")
        : () -> client.subscribe(pubSub, channel);
  }

  private void exit(ExitMode exit, RuntimeException failure, Runnable unsubscribe) {
    switch (exit) {
      case CALLBACK_FAILURE:
        throw failure;
      case INTERRUPT:
        Thread.currentThread().interrupt();
        break;
      case UNSUBSCRIBE:
        unsubscribe.run();
        break;
      default:
        throw new AssertionError(exit);
    }
  }
}
