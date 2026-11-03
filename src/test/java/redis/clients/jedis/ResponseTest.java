package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

class ResponseTest {

  @Test
  void completeExceptionallyRethrowsJedisExceptionsAsTheyAre() {
    Response<String> response = new Response<>(BuilderFactory.STRING);
    JedisConnectionException failure = new JedisConnectionException("connection reset");

    response.completeExceptionally(failure);

    assertSame(failure, assertThrows(JedisConnectionException.class, response::get));
  }

  @Test
  void completeExceptionallyWrapsOtherFailures() {
    Response<String> response = new Response<>(BuilderFactory.STRING);
    Error error = new Error("out of memory");

    response.completeExceptionally(error);

    JedisException thrown = assertThrows(JedisException.class, response::get);
    assertFalse(thrown instanceof JedisConnectionException);
    assertSame(error, thrown.getCause());
  }

  @Test
  void completeExceptionallyHasNoEffectOnACompletedResponse() {
    Response<String> response = new Response<>(BuilderFactory.STRING);
    response.set(SafeEncoder.encode("OK"));

    response.completeExceptionally(new JedisConnectionException("too late"));

    assertEquals("OK", response.get());
  }

  @Test
  void firstFailureWins() {
    Response<String> response = new Response<>(BuilderFactory.STRING);
    JedisConnectionException first = new JedisConnectionException("first");

    response.completeExceptionally(first);
    response.completeExceptionally(new JedisConnectionException("second"));

    assertSame(first, assertThrows(JedisConnectionException.class, response::get));
  }
}
