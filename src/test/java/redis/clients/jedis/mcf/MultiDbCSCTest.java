package redis.clients.jedis.mcf;

import org.junit.jupiter.api.*;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.TcpMockServer;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class MultiDbCSCTest {

  TcpMockServer server;
  TcpMockServer server2;
  Cache cache;
  HostAndPort active;
  HostAndPort standby;

  @BeforeEach
  public void setUp() throws IOException {
    cache = spy(CacheFactory.getCache(CacheConfig.builder().build()));
    server = new TcpMockServer();
    server2 = new TcpMockServer();
    server.start();
    server2.start();
    active = new HostAndPort("localhost", server.getPort());
    standby = new HostAndPort("localhost", server2.getPort());
  }

  @AfterEach
  public void tearDown() throws IOException {
    server.stop();
    server2.stop();
  }

  @Test
  public void databaseSwitch_flushesClientSideCache() throws IOException {
    // SET is not cacheable, so it goes straight to the server; answer it so the pre-MULTI command
    // completes and the cached connection is returned to the pool (idle) before the switch.
    server.setCommandHandler(
      (args, clientId) -> "SET".equals(SafeEncoder.encode(args.getCommand().getRaw())) ? "+OK\r\n"
          : null);

    MultiDbConnectionProvider p = twoDbProvider(active, standby, true, cache);
    MultiDbClient client = MultiDbClient.builder().connectionProvider(p).build();
    try {
      Database standbyDb = p.getDatabase(standby);

      client.set("foo", "bar");

      // Ignore any cache interactions during warm-up; only care about the flush on switch.
      reset(cache);

      // Switching away from the active database force-disconnects its pool and flushes the cache.
      p.setActiveDatabase(standby);

      assertEquals(standbyDb, p.getDatabase(), "active database should have switched");
      verify(cache, atLeastOnce()).flush();
    } finally {
      p.close();

    }
  }

  @Test
  public void connectionClose_flushesClientSideCache() throws IOException {
    server.setCommandHandler((args, clientId) -> {
      return null; // ( anything unhandled) -> server closes the connection
    });

    HostAndPort standby = new HostAndPort("fake-standby", 6379);

    MultiDbConnectionProvider p = twoDbProvider(active, standby, false, cache);
    MultiDbClient client = MultiDbClient.builder().connectionProvider(p).build();

    try {
      reset(cache);
      assertThrows(JedisConnectionException.class, () -> client.set("foo", "bar"));

      // Destroying the broken CacheConnection disconnects it and flushes the client-side cache.
      verify(cache, atLeastOnce()).flush();
    } finally {
      p.close();
    }
  }

  @Test
  public void databaseSwitch_notFlushesClientSideCache() throws IOException {
    // SET is not cacheable, so it goes straight to the server; answer it so the pre-MULTI command
    // completes and the cached connection is returned to the pool (idle) before the switch.
    server.setCommandHandler(
      (args, clientId) -> "SET".equals(SafeEncoder.encode(args.getCommand().getRaw())) ? "+OK\r\n"
          : null);

    MultiDbConnectionProvider p = twoDbProvider(active, standby, false, cache);
    MultiDbClient client = MultiDbClient.builder().connectionProvider(p).build();
    try {
      Database standbyDb = p.getDatabase(standby);
      client.set("foo", "bar");

      // Ignore any cache interactions during warm-up; only care about the flush on switch.
      reset(cache);

      // Switching away from the active database force-disconnects its pool and flushes the cache.
      p.setActiveDatabase(standby);

      assertEquals(standbyDb, p.getDatabase(), "active database should have switched");
      verify(cache, never()).flush();
    } finally {
      p.close();
    }
  }

  private static MultiDbConnectionProvider twoDbProvider(HostAndPort active, HostAndPort standby,
      boolean fastFailover, Cache cache) {
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3).build();

    DatabaseConfig activeDb = DatabaseConfig.builder(active, clientConfig).healthCheckEnabled(false)
        .weight(100.0f).build();
    DatabaseConfig standbyDb = DatabaseConfig.builder(standby, clientConfig)
        .healthCheckEnabled(false).weight(1.0f).build();
    MultiDbConfig cfg = MultiDbConfig.builder(new DatabaseConfig[] { activeDb, standbyDb })
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
        .fastFailover(fastFailover).build();
    return cache == null ? new MultiDbConnectionProvider(cfg)
        : new MultiDbConnectionProvider(cfg, cache);
  }
}
