package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Unit tests for {@link MultiDbPipeline}. Uses a real {@link MultiDbConnectionProvider} with mocked
 * {@link TrackingConnectionPool} and {@link Connection} to avoid network I/O.
 */
public class MultiDbPipelineUnitTest {

  private final HostAndPort fakeEndpoint = new HostAndPort("fake", 6379);
  private MultiDbConnectionProvider provider;
  private Database database;
  private TrackingConnectionPool poolMock;
  private MultiDbPipeline pipeline;

  @BeforeEach
  public void setup() throws Exception {
    DatabaseConfig db = DatabaseConfig
        .builder(fakeEndpoint, DefaultJedisClientConfig.builder().build()).healthCheckEnabled(false)
        .weight(1.0f).build();

    MultiDbConfig cfg = MultiDbConfig.builder(new DatabaseConfig[] { db })
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build()).build();

    provider = new MultiDbConnectionProvider(cfg);
    database = provider.getDatabase();

    poolMock = mock(TrackingConnectionPool.class);
    ReflectionTestUtil.setField(database, "connectionPool", poolMock);

    pipeline = new MultiDbPipeline(provider, new CommandObjects(RedisProtocol.RESP3));
  }

  @Test
  public void syncOnEmptyPipeline_doesNotBorrowConnection() {
    pipeline.sync();
    verify(poolMock, never()).getResource();
  }

  @Test
  public void sync_executesQueuedCommandsAndReleasesConnection() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    when(conn.getMany(anyInt())).thenReturn(Arrays.asList("OK".getBytes(), "bar".getBytes()));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    Response<String> setResp = pipeline.set("foo", "bar");
    Response<String> getResp = pipeline.get("foo");

    pipeline.sync();

    verify(poolMock, times(1)).getResource();
    verify(conn, times(2)).sendCommand(any(CommandArguments.class));
    verify(conn, times(1)).getMany(2);
    verify(conn, atLeastOnce()).close();
    assertEquals("OK", setResp.get());
    assertEquals("bar", getResp.get());
  }

  @Test
  public void close_callsSync_releasingConnection() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    when(conn.getMany(anyInt())).thenReturn(Arrays.asList("PONG"));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    pipeline.set("k", "v");
    pipeline.close();

    verify(conn, atLeastOnce()).close();
  }

  @Test
  public void close_onEmptyPipeline_doesNotBorrowConnection() {
    pipeline.close();
    verify(poolMock, never()).getResource();
  }

  @Test
  public void sync_pingThrows_propagatesException() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenThrow(new JedisConnectionException("stale"));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    pipeline.set("k", "v");

    assertThrows(Exception.class, () -> pipeline.sync());
  }

  /**
   * Demonstrates that a Resilience4j retry replays the validation ping. With a higher maxAttempts,
   * an intermittent ping failure is recovered and the pipeline completes successfully.
   */
  @Test
  public void sync_transientConnectionValidationFailure_recoversViaRetry() throws Exception {
    MultiDbConfig cfg = MultiDbConfig
        .builder(new DatabaseConfig[] {
            DatabaseConfig.builder(fakeEndpoint, DefaultJedisClientConfig.builder().build())
                .healthCheckEnabled(false).weight(1.0f).build() })
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(3).waitDuration(1).build())
        .build();

    try (MultiDbConnectionProvider localProvider = new MultiDbConnectionProvider(cfg)) {
      TrackingConnectionPool localPool = mock(TrackingConnectionPool.class);
      ReflectionTestUtil.setField(localProvider.getDatabase(), "connectionPool", localPool);

      AtomicInteger pingCalls = new AtomicInteger();
      Connection conn = mock(Connection.class);
      when(conn.ping()).thenAnswer(inv -> {
        if (pingCalls.incrementAndGet() == 1) throw new JedisConnectionException("stale");
        return true;
      });
      when(conn.getMany(anyInt())).thenReturn(Arrays.asList("OK".getBytes()));
      doNothing().when(conn).close();
      when(localPool.getResource()).thenReturn(conn);

      try (MultiDbPipeline p = new MultiDbPipeline(localProvider,
          new CommandObjects(RedisProtocol.RESP3))) {
        Response<String> r = p.set("k", "v");
        p.sync();
        assertEquals("OK", r.get());
        assertEquals(2, pingCalls.get());
      }
    }
  }

  /**
   * When the validation ping fails repeatedly and the retry policy borrows a new connection on each
   * attempt, every borrowed connection must be returned to the pool (i.e. closed).
   */
  @Test
  public void sync_connectionValidationAlwaysFails_allAcquiredConnectionsReturnedToPool()
      throws Exception {
    MultiDbConfig cfg = MultiDbConfig
        .builder(new DatabaseConfig[] {
            DatabaseConfig.builder(fakeEndpoint, DefaultJedisClientConfig.builder().build())
                .healthCheckEnabled(false).weight(1.0f).build() })
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(3).waitDuration(1).build())
        .build();

    try (MultiDbConnectionProvider localProvider = new MultiDbConnectionProvider(cfg)) {
      TrackingConnectionPool localPool = mock(TrackingConnectionPool.class);
      ReflectionTestUtil.setField(localProvider.getDatabase(), "connectionPool", localPool);

      // three distinct connections: first two pings fail, third succeeds
      Connection conn1 = mock(Connection.class);
      Connection conn2 = mock(Connection.class);
      Connection conn3 = mock(Connection.class);
      when(conn1.ping()).thenThrow(new JedisConnectionException("stale-1"));
      when(conn2.ping()).thenThrow(new JedisConnectionException("stale-2"));
      when(conn3.ping()).thenReturn(true);
      when(conn3.getMany(anyInt())).thenReturn(Arrays.asList("OK".getBytes()));
      doNothing().when(conn1).close();
      doNothing().when(conn2).close();
      doNothing().when(conn3).close();
      when(localPool.getResource()).thenReturn(conn1, conn2, conn3);

      try (MultiDbPipeline p = new MultiDbPipeline(localProvider,
          new CommandObjects(RedisProtocol.RESP3))) {
        Response<String> r = p.set("k", "v");
        p.sync();
        assertEquals("OK", r.get());
      }

      verify(localPool, times(3)).getResource();
      verify(conn1, atLeastOnce()).close();
      verify(conn2, atLeastOnce()).close();
      verify(conn3, atLeastOnce()).close();
    }
  }

  /**
   * When sendCommand throws after the connection has been acquired and validated, the borrowed
   * connection must still be returned to the pool by the try-with-resources block in sync().
   */
  @Test
  public void sync_sendCommandThrows_connectionReturnedToPool() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    doThrow(new JedisConnectionException("network down")).when(conn)
        .sendCommand(any(CommandArguments.class));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    pipeline.set("k", "v");
    assertThrows(Exception.class, () -> pipeline.sync());

    verify(poolMock, times(1)).getResource();
    verify(conn, atLeastOnce()).close();
  }

  /**
   * When getMany throws after commands have been written, the borrowed connection must still be
   * returned to the pool by the try-with-resources block in sync().
   */
  @Test
  public void sync_getManyThrows_connectionReturnedToPool() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    when(conn.getMany(anyInt())).thenThrow(new JedisConnectionException("read failed"));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    pipeline.set("k", "v");
    assertThrows(Exception.class, () -> pipeline.sync());

    verify(poolMock, times(1)).getResource();
    verify(conn, atLeastOnce()).close();
  }
}
