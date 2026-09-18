package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Unit tests for {@link MultiDbTransaction}. Uses a real {@link MultiDbConnectionProvider} with
 * mocked {@link TrackingConnectionPool} and {@link Connection} to avoid network I/O.
 */
public class MultiDbTransactionUnitTest {

  private final HostAndPort fakeEndpoint = new HostAndPort("fake", 6379);
  private MultiDbConnectionProvider provider;
  private Database database;
  private TrackingConnectionPool poolMock;

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
  }

  private MultiDbTransaction newTx(boolean doMulti) {
    return new MultiDbTransaction(provider, doMulti, new CommandObjects(RedisProtocol.RESP3));
  }

  @Test
  public void exec_happyPath_returnsFormattedResponsesAndReleasesConnection() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    // MULTI is dispatched via executeCommand(CommandObject) and expects a raw byte[] reply
    when(conn.executeCommand(any(CommandObject.class))).thenReturn("OK".getBytes());
    // getMany() consumes QUEUED replies (raw byte[]) for each buffered command
    when(conn.getMany(anyInt()))
        .thenReturn(Arrays.<Object> asList("QUEUED".getBytes(), "QUEUED".getBytes()));
    // then EXEC returns the values for the user commands (raw byte[] payloads from protocol)
    when(conn.getObjectMultiBulkReply())
        .thenReturn(Arrays.<Object> asList("OK".getBytes(), "bar".getBytes()));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    MultiDbTransaction tx = newTx(true);
    Response<String> setResp = tx.set("foo", "bar");
    Response<String> getResp = tx.get("foo");

    List<Object> formatted = tx.exec();

    assertEquals(2, formatted.size());
    assertEquals("OK", formatted.get(0));
    assertEquals("bar", formatted.get(1));
    assertEquals("OK", setResp.get());
    assertEquals("bar", getResp.get());

    // set + get are dispatched via sendCommand(CommandArguments); MULTI uses executeCommand and
    // EXEC uses the ProtocolCommand overload
    verify(conn, times(2)).sendCommand(any(CommandArguments.class));
    verify(conn, times(1)).executeCommand(any(CommandObject.class));
    verify(conn, times(1)).sendCommand(Protocol.Command.EXEC);
    verify(conn, atLeastOnce()).close();
    verify(poolMock, times(1)).getResource();
  }

  @Test
  public void exec_nullReply_returnsNullAndClearsCommands() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    when(conn.executeCommand(any(CommandObject.class))).thenReturn("OK".getBytes());
    when(conn.getMany(anyInt())).thenReturn(Arrays.<Object> asList("QUEUED".getBytes()));
    when(conn.getObjectMultiBulkReply()).thenReturn(null);
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    MultiDbTransaction tx = newTx(true);
    tx.set("k", "v");
    assertNull(tx.exec());
    verify(conn, atLeastOnce()).close();
  }

  @Test
  public void watch_setsInWatch_andClose_callsUnwatch() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    when(conn.executeCommand(any(CommandObject.class))).thenReturn("OK");
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    // doMulti=false defers connection acquisition; watch() borrows it and executes WATCH inline.
    MultiDbTransaction tx = newTx(false);
    assertEquals("OK", tx.watch("k"));
    tx.close();

    // the same connection borrowed for WATCH is reused for the UNWATCH triggered by close()
    verify(poolMock, times(1)).getResource();
    // WATCH and UNWATCH are both routed through appendCommand -> executeCommand(CommandObject)
    verify(conn, times(2)).executeCommand(any(CommandObject.class));
    verify(conn, atLeastOnce()).close();
  }

  @Test
  public void multi_then_exec_clearsState() {
    Connection conn = mock(Connection.class);
    when(conn.ping()).thenReturn(true);
    when(conn.executeCommand(any(CommandObject.class))).thenReturn("OK".getBytes());
    when(conn.getMany(anyInt())).thenReturn(Arrays.<Object> asList());
    when(conn.getObjectMultiBulkReply()).thenReturn(Arrays.<Object> asList());
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    MultiDbTransaction tx = newTx(true);
    tx.exec();
    // exec without intervening multi should now throw
    assertThrows(IllegalStateException.class, tx::exec);
  }

  @Test
  public void exec_pingFails_releasesConnectionFromPool() {
    Connection conn = mock(Connection.class);
    when(conn.ping())
        .thenThrow(new redis.clients.jedis.exceptions.JedisConnectionException("stale"));
    doNothing().when(conn).close();
    when(poolMock.getResource()).thenReturn(conn);

    MultiDbTransaction tx = newTx(true);
    tx.set("k", "v");
    Exception ex = assertThrows(Exception.class, tx::exec);
    assertTrue(ex instanceof JedisConnectionException);
    assertEquals("stale", ex.getMessage());
    verify(conn, atLeastOnce()).close();
  }

  @Test
  public void exec_withoutMulti_throws() {
    MultiDbTransaction tx = newTx(false);
    assertThrows(IllegalStateException.class, tx::exec);
    // exec rejects the call before reaching the connection supplier
    verify(poolMock, never()).getResource();
  }

  @Test
  public void discard_withoutMulti_throws() {
    MultiDbTransaction tx = newTx(false);
    assertThrows(IllegalStateException.class, tx::discard);
    verify(poolMock, never()).getResource();
  }

  @Test
  public void discard_happyPath_releasesConnection() {
    MultiDbTransaction tx = newTx(true);
    tx.set("k", "v");
    // MULTI was never sent (connection acquisition deferred); discard just clears local state
    assertEquals("OK", tx.discard());

    // no command ever needed a connection, so the pool was never touched
    verify(poolMock, never()).getResource();
  }

  @Test
  public void close_inMulti_invokesDiscard() {
    MultiDbTransaction tx = newTx(true);
    tx.set("k", "v");
    tx.close();

    // discard during close clears local state without contacting the server
    verify(poolMock, never()).getResource();
  }

  @Test
  public void close_notInMultiNorWatch_doesNothing() {
    MultiDbTransaction tx = newTx(false);
    tx.close();
    // no command was issued, so the supplier was never touched
    verify(poolMock, never()).getResource();
  }

  @Test
  public void exec_whenActiveDatabaseChangesMidTransaction_throws() {
    HostAndPort active = fakeEndpoint;
    HostAndPort standby = new HostAndPort("fake-standby", 6379);

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3).build();

    DatabaseConfig activeDb = DatabaseConfig.builder(active, clientConfig).healthCheckEnabled(false)
        .weight(100.0f).build();
    DatabaseConfig standbyDb = DatabaseConfig.builder(standby, clientConfig)
        .healthCheckEnabled(false).weight(1.0f).build();
    MultiDbConfig cfg = MultiDbConfig.builder(new DatabaseConfig[] { activeDb, standbyDb })
        .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build()).build();
    MultiDbConnectionProvider p = new MultiDbConnectionProvider(cfg);

    try {
      Connection conn = mock(Connection.class);
      when(conn.executeCommand(any(CommandObject.class))).thenReturn("OK".getBytes());

      Database initial = p.getDatabase();
      Database other = p.getDatabase(standby);
      initial = spy(initial);
      doReturn(conn).when(initial).getConnection();
      other = spy(other);
      doReturn(conn).when(other).getConnection();

      p = spy(p);
      // 1 - initial: for acquiring the conn
      // 2 - initial: for the pre-MULTI command
      // 3 - other: for the database switch check during exec()
      when(p.getDatabase()).thenReturn(initial).thenReturn(initial).thenReturn(other);

      // doMulti=false: the pre-MULTI command borrows a connection and pins the initial database.
      MultiDbTransaction tx = new MultiDbTransaction(p, false,
          new CommandObjects(RedisProtocol.RESP3));
      tx.set("k", "v");
      tx.multi();
      tx.set("k", "v2");

      // Simulate a database switch happening between MULTI buffering and exec().
      ReflectionTestUtil.setField(p, "activeDatabase", other);

      JedisException ex = assertThrows(JedisException.class, tx::exec);
      assertTrue(ex.getMessage().contains("Active database has changed"),
        "expected active-database-changed error, got: " + ex.getMessage());

      verify(conn, times(1)).executeCommand(any(CommandArguments.class));
      verify(conn, atLeastOnce()).close();
    } finally {
      p.close();
    }
  }

}
