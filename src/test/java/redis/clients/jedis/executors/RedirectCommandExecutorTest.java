package redis.clients.jedis.executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.providers.RedirectConnectionProvider;

@ExtendWith(MockitoExtension.class)
public class RedirectCommandExecutorTest {

  @Mock
  private RedirectConnectionProvider mockProvider;

  @Mock
  private Connection firstConnection;

  @Mock
  private Connection secondConnection;

  @Mock
  private CommandObject<String> mockCommandObject;

  @Test
  public void constructorRejectsNullProvider() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> new RedirectCommandExecutor(null, 3, Duration.ofSeconds(1)));

    assertTrue(exception.getMessage().contains("provider"));
  }

  @Test
  public void constructorRejectsInvalidMaxAttempts() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> new RedirectCommandExecutor(mockProvider, 0, Duration.ofSeconds(1)));

    assertTrue(exception.getMessage().contains("maxAttempts"));
  }

  @Test
  public void constructorRejectsNullMaxTotalRetriesDuration() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> new RedirectCommandExecutor(mockProvider, 3, null));

    assertTrue(exception.getMessage().contains("maxTotalRetriesDuration"));
  }

  @Test
  public void constructorAcceptsValidValues() {
    assertDoesNotThrow(() -> new RedirectCommandExecutor(mockProvider, 1, Duration.ZERO));
    assertDoesNotThrow(() -> new RedirectCommandExecutor(mockProvider, 3, Duration.ofSeconds(1)));
  }

  @Test
  public void executeCommandReturnsSuccessfulResult() {
    when(mockProvider.getConnection()).thenReturn(firstConnection);
    when(firstConnection.executeCommand(mockCommandObject)).thenReturn("success");

    RedirectCommandExecutor executor = new RedirectCommandExecutor(mockProvider, 1,
        Duration.ofSeconds(1));

    assertEquals("success", executor.executeCommand(mockCommandObject));
    verify(mockProvider, times(1)).getConnection();
    verify(firstConnection, times(1)).close();
  }

  @Test
  public void executeCommandRenewsPoolAndRetriesOnRedirect() {
    HostAndPort targetNode = new HostAndPort("127.0.0.1", 6380);
    JedisRedirectionException redirect = new JedisRedirectionException("REDIRECT 127.0.0.1:6380",
        targetNode, -1);
    when(mockProvider.getConnection()).thenReturn(firstConnection, secondConnection);
    when(firstConnection.executeCommand(mockCommandObject)).thenThrow(redirect);
    when(secondConnection.executeCommand(mockCommandObject)).thenReturn("success");

    RedirectCommandExecutor executor = new RedirectCommandExecutor(mockProvider, 2,
        Duration.ofSeconds(1));

    assertEquals("success", executor.executeCommand(mockCommandObject));
    verify(mockProvider, times(2)).getConnection();
    verify(mockProvider, times(1)).renewPool(firstConnection, targetNode);
    verify(firstConnection, times(1)).close();
    verify(secondConnection, times(1)).close();
  }

  @Test
  public void executeCommandStopsAfterMaxRedirectAttempts() {
    HostAndPort targetNode = new HostAndPort("127.0.0.1", 6380);
    JedisRedirectionException redirect = new JedisRedirectionException("REDIRECT 127.0.0.1:6380",
        targetNode, -1);
    when(mockProvider.getConnection()).thenReturn(firstConnection);
    when(firstConnection.executeCommand(mockCommandObject)).thenThrow(redirect);

    RedirectCommandExecutor executor = new RedirectCommandExecutor(mockProvider, 2,
        Duration.ofSeconds(1));

    JedisException exception = assertThrows(JedisException.class,
      () -> executor.executeCommand(mockCommandObject));

    assertEquals("No more redirect attempts left.", exception.getMessage());
    assertEquals(1, exception.getSuppressed().length);
    verify(mockProvider, times(2)).getConnection();
    verify(mockProvider, times(2)).renewPool(firstConnection, targetNode);
    verify(firstConnection, times(2)).close();
  }

  @Test
  public void executeCommandRenewsPoolOnFinalConnectionFailureWhenRetriesAreLow() {
    when(mockProvider.getConnection()).thenThrow(new JedisConnectionException("connection failed"));
    RedirectCommandExecutor executor = new RedirectCommandExecutor(mockProvider, 1,
        Duration.ofSeconds(1));

    JedisException exception = assertThrows(JedisException.class,
      () -> executor.executeCommand(mockCommandObject));

    assertEquals("No more redirect attempts left.", exception.getMessage());
    verify(mockProvider, times(1)).renewPool(eq(null), eq(null));
  }
}
