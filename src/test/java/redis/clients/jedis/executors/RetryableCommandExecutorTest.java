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
public class RetryableCommandExecutorTest {

  @Mock
  private ConnectionProvider mockProvider;
  
  @Mock
  private Connection mockConnection;
  
  @Mock
  private CommandObject<String> mockCommandObject;

  @Test
  public void testConstructorWithNullProvider() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new RetryableCommandExecutor(null, 3, Duration.ofSeconds(1)),
        "Should throw IllegalArgumentException when provider is null"
    );
    
    assertTrue(exception.getMessage().contains("provider"),
        "Exception message should mention 'provider'");
  }
  
  @Test
  public void testConstructorWithInvalidMaxAttempts() {
    // Test with zero
    IllegalArgumentException exceptionZero = assertThrows(
        IllegalArgumentException.class,
        () -> new RetryableCommandExecutor(mockProvider, 0, Duration.ofSeconds(1)),
        "Should throw IllegalArgumentException when maxAttempts is zero"
    );
    
    assertTrue(exceptionZero.getMessage().contains("maxAttempts"),
        "Exception message should mention 'maxAttempts'");
    
    // Test with negative value
    IllegalArgumentException exceptionNegative = assertThrows(
        IllegalArgumentException.class,
        () -> new RetryableCommandExecutor(mockProvider, -1, Duration.ofSeconds(1)),
        "Should throw IllegalArgumentException when maxAttempts is negative"
    );
    
    assertTrue(exceptionNegative.getMessage().contains("maxAttempts"),
        "Exception message should mention 'maxAttempts'");
  }

  @Test
  public void testValidConstruction() {
    // Should not throw any exceptions
    assertDoesNotThrow(() -> new RetryableCommandExecutor(mockProvider, 1, Duration.ofSeconds(1)));
    assertDoesNotThrow(() -> new RetryableCommandExecutor(mockProvider, 3, Duration.ZERO));
    assertDoesNotThrow(() -> new RetryableCommandExecutor(mockProvider, 10, Duration.ofMinutes(5)));
  }
  
  @Test
  public void testMaxAttemptsIsRespected() throws Exception {
    // Set up the mock to return a connection but throw an exception when executing
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(any(CommandObject.class))).thenThrow(new JedisConnectionException("Connection failed"));
    
    // Create the executor with exactly 3 attempts
    final int maxAttempts = 3;
    RetryableCommandExecutor executor = spy(new RetryableCommandExecutor(mockProvider, maxAttempts, Duration.ofSeconds(10)));
    
    // Mock the sleep method to avoid actual sleeping
    doNothing().when(executor).sleep(anyLong());
    
    // Execute the command and expect an exception
    assertThrows(JedisException.class, () -> executor.executeCommand(mockCommandObject));
    
    // Verify that we tried exactly maxAttempts times
    verify(mockProvider, times(maxAttempts)).getConnection(any());
    verify(mockConnection, times(maxAttempts)).close();
  }
  
  @Test
  public void testExecuteCommandWithNoRetries() throws Exception {
    // Set up the mock to return a connection and have it execute the command successfully
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(mockCommandObject)).thenReturn("success");
    
    // Create the executor with just 1 attempt (no retries)
    RetryableCommandExecutor executor = new RetryableCommandExecutor(mockProvider, 1, Duration.ofSeconds(1));
    
    // Execute the command
    String result = executor.executeCommand(mockCommandObject);
    
    // Verify the result and that the connection was closed
    assertEquals("success", result);
    verify(mockConnection, times(1)).close();
    verify(mockProvider, times(1)).getConnection(any());
  }
  
  @Test
  public void testMaxAttemptsExceeded() throws Exception {
    // Set up the mock to return a connection but throw an exception when executing
    when(mockProvider.getConnection(any())).thenReturn(mockConnection);
    when(mockConnection.executeCommand(any(CommandObject.class))).thenThrow(new JedisConnectionException("Connection failed"));
    
    // Create the executor with 3 attempts
    RetryableCommandExecutor executor = spy(new RetryableCommandExecutor(mockProvider, 3, Duration.ofSeconds(1)));
    
    // Mock the sleep method to avoid actual sleeping
    doNothing().when(executor).sleep(anyLong());
    
    // Execute the command and expect an exception
    JedisException exception = assertThrows(
        JedisException.class,
        () -> executor.executeCommand(mockCommandObject),
        "Should throw JedisException when max attempts are exceeded"
    );
    
    // Verify the exception and that we tried the correct number of times
    assertEquals("No more attempts left.", exception.getMessage());
    assertEquals(1, exception.getSuppressed().length);
    verify(mockProvider, times(3)).getConnection(any());
    verify(mockConnection, times(3)).close();
  }
}