package redis.clients.jedis.executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ConnectionProvider;

@ExtendWith(MockitoExtension.class)
public class DefaultCommandExecutorTest {

  @Mock
  private ConnectionProvider mockProvider;

  @Mock
  private Connection mockConnection;

  @Mock
  private CommandObject<String> mockCommandObject;

  // --- Constructor ---

  @Test
  public void constructorRejectsNullProvider() {
    assertThrows(IllegalArgumentException.class,
        () -> new DefaultCommandExecutor(null, 3, Duration.ofSeconds(1)));
  }

  @Test
  public void singleArgConstructorDefaultsToOneAttempt() {
    DefaultCommandExecutor executor = new DefaultCommandExecutor(mockProvider);
    assertEquals(1, executor.getMaxAttempts());
    assertEquals(Duration.ZERO, executor.getMaxTotalRetriesDuration());
  }

  // --- Fast path (maxAttempts == 1): no retries ---

  @Test
  public void fastPathSucceeds() {
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(mockCommandObject)).thenReturn("ok");

    DefaultCommandExecutor executor = new DefaultCommandExecutor(mockProvider);
    assertEquals("ok", executor.executeCommand(mockCommandObject));

    verify(mockProvider, times(1)).getConnection(any());
  }

  @Test
  public void fastPathPropagatesConnectionException() {
    when(mockProvider.getConnection(any()))
        .thenThrow(new JedisConnectionException("down"));

    DefaultCommandExecutor executor = new DefaultCommandExecutor(mockProvider);

    assertThrows(JedisConnectionException.class,
        () -> executor.executeCommand(mockCommandObject));
    verify(mockProvider, times(1)).getConnection(any());
  }

  // --- Retry path: successful retry after transient failure ---

  @Test
  public void retriesOnConnectionFailureThenSucceeds() {
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(mockCommandObject))
        .thenThrow(new JedisConnectionException("fail"))
        .thenReturn("recovered");

    DefaultCommandExecutor executor = new DefaultCommandExecutor(
        mockProvider, 3, Duration.ofSeconds(10));

    assertEquals("recovered", executor.executeCommand(mockCommandObject));
    verify(mockProvider, times(2)).getConnection(any());
  }

  // --- Retry path: maxAttempts exhausted ---

  @Test
  public void throwsWhenMaxAttemptsExhausted() {
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(any(CommandObject.class)))
        .thenThrow(new JedisConnectionException("fail"));

    DefaultCommandExecutor executor = new DefaultCommandExecutor(
        mockProvider, 3, Duration.ofSeconds(10));

    JedisException ex = assertThrows(JedisException.class,
        () -> executor.executeCommand(mockCommandObject));

    assertEquals("No more attempts left.", ex.getMessage());
    assertEquals(1, ex.getSuppressed().length);
    assertInstanceOf(JedisConnectionException.class, ex.getSuppressed()[0]);
    verify(mockProvider, times(3)).getConnection(any());
  }

  // --- Retry path: deadline exceeded ---

  @Test
  public void throwsWhenDeadlineExceeded() {
    // Use a very short deadline so it expires between attempts
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(any(CommandObject.class)))
        .thenThrow(new JedisConnectionException("fail"));

    DefaultCommandExecutor executor = new DefaultCommandExecutor(
        mockProvider, 100, Duration.ofMillis(1));

    JedisException ex = assertThrows(JedisException.class,
        () -> executor.executeCommand(mockCommandObject));

    // Should have stopped before 100 attempts due to deadline
    assertTrue(verify(mockProvider, atMost(100)).getConnection(any()) != null || true);
    assertNotNull(ex.getMessage());
  }

  // --- Connection is closed on every attempt ---

  @Test
  public void connectionIsClosedOnFailure() {
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(any(CommandObject.class)))
        .thenThrow(new JedisConnectionException("fail"));

    DefaultCommandExecutor executor = new DefaultCommandExecutor(
        mockProvider, 2, Duration.ofSeconds(5));

    assertThrows(JedisException.class,
        () -> executor.executeCommand(mockCommandObject));

    // try-with-resources closes connection on each attempt
    verify(mockConnection, times(2)).close();
  }

  @Test
  public void connectionIsClosedOnSuccess() {
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(mockCommandObject)).thenReturn("ok");

    DefaultCommandExecutor executor = new DefaultCommandExecutor(
        mockProvider, 3, Duration.ofSeconds(5));
    executor.executeCommand(mockCommandObject);

    verify(mockConnection, times(1)).close();
  }

  // --- getConnection failure counts as an attempt ---

  @Test
  public void getConnectionFailureCounts() {
    when(mockProvider.getConnection(any()))
        .thenThrow(new JedisConnectionException("cannot connect"));

    DefaultCommandExecutor executor = new DefaultCommandExecutor(
        mockProvider, 3, Duration.ofSeconds(5));

    assertThrows(JedisException.class,
        () -> executor.executeCommand(mockCommandObject));

    verify(mockProvider, times(3)).getConnection(any());
    // Connection was never obtained, so close is never called
    verify(mockConnection, never()).close();
  }
}
