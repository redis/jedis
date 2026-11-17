package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.authentication.core.SimpleToken;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.authentication.AuthXEventListener;
import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.exceptions.JedisException;

public class JedisSafeAuthenticatorTest {

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void removesAuthReplyHandlerWhenTransportFails(boolean duringFlush) {
    JedisException failure = new JedisException("transport failed");
    JedisSafeAuthenticator authenticator = new JedisSafeAuthenticator();
    AuthXEventListener listener = configureConnection(authenticator);
    if (duringFlush) {
      doThrow(failure).when(authenticator.client).flush();
    } else {
      doThrow(failure).when(authenticator.client).sendCommand(any(CommandArguments.class));
    }
    Consumer<Object> previous = reply -> {
    };
    authenticator.resultHandler.add(previous);

    renew(authenticator);

    assertSame(previous, authenticator.resultHandler.poll());
    assertTrue(authenticator.resultHandler.isEmpty());
    assertNull(authenticator.pendingTokenRef.get());
    assertFalse(authenticator.commandSync.isLocked());
    verify(listener).onConnectionAuthenticationError(failure);
  }

  @Test
  public void removesAuthReplyHandlerWhenSendFails() {
    JedisException failure = new JedisException("send failed");
    JedisSafeAuthenticator authenticator = new JedisSafeAuthenticator() {
      @Override
      protected void sendAndFlushCommand(Command command, Object... args) {
        throw failure;
      }
    };
    AuthXEventListener listener = configureConnection(authenticator);
    Consumer<Object> previous = reply -> {
    };
    authenticator.resultHandler.add(previous);

    renew(authenticator);

    assertSame(previous, authenticator.resultHandler.poll());
    assertTrue(authenticator.resultHandler.isEmpty());
    assertNull(authenticator.pendingTokenRef.get());
    assertFalse(authenticator.commandSync.isLocked());
    verify(listener).onConnectionAuthenticationError(failure);
  }

  @Test
  public void preservesEarlierAuthReplyWhenLaterSendFails() {
    JedisException failure = new JedisException("send failed");
    AtomicInteger sends = new AtomicInteger();
    List<String> handled = new ArrayList<>();
    JedisSafeAuthenticator authenticator = new JedisSafeAuthenticator() {
      @Override
      protected void sendAndFlushCommand(Command command, Object... args) {
        if (sends.incrementAndGet() == 2) {
          throw failure;
        }
      }

      @Override
      protected void processAuthReply(Object reply) {
        handled.add("auth:" + reply);
      }
    };
    AuthXEventListener listener = configureConnection(authenticator);
    renew(authenticator);
    authenticator.resultHandler.add(reply -> handled.add("ping:" + reply));

    renew(authenticator);

    authenticator.resultHandler.poll().accept("OK");
    authenticator.resultHandler.poll().accept("PONG");
    assertEquals(Arrays.asList("auth:OK", "ping:PONG"), handled);
    assertTrue(authenticator.resultHandler.isEmpty());
    verify(listener).onConnectionAuthenticationError(failure);
  }

  private static AuthXEventListener configureConnection(JedisSafeAuthenticator authenticator) {
    Connection connection = mock(Connection.class);
    when(connection.encodeToBytes(any(char[].class))).thenReturn(new byte[] { 1 });
    AuthXManager manager = mock(AuthXManager.class);
    AuthXEventListener listener = mock(AuthXEventListener.class);
    when(connection.getAuthXManager()).thenReturn(manager);
    when(manager.getListener()).thenReturn(listener);
    authenticator.client = connection;
    return listener;
  }

  private static void renew(JedisSafeAuthenticator authenticator) {
    authenticator.authenticationHandler.accept(new SimpleToken("default", "password", 0, 0, null));
  }
}
