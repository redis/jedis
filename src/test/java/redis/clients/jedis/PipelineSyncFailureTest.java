package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * {@link Pipeline#sync()} propagates a lost connection to the caller. Its buffered responses must
 * also carry that error, so that {@link Response#get()} reports the lost reply instead of the
 * generic "not synced yet" state, matching {@link MultiNodePipelineBase#sync()}.
 */
class PipelineSyncFailureTest {

  private Connection connection;
  private JedisConnectionException failure;

  @BeforeEach
  void setUp() {
    connection = mock(Connection.class);
    failure = new JedisConnectionException("connection reset");
    when(connection.getMany(anyInt())).thenThrow(failure);
  }

  @Test
  void syncCompletesBufferedResponsesWithTheConnectionError() {
    Pipeline pipeline = new Pipeline(connection);
    Response<String> reply = pipeline.set("foo", "bar");

    assertSame(failure, assertThrows(JedisConnectionException.class, pipeline::sync));
    assertSame(failure, assertThrows(JedisConnectionException.class, reply::get));

    // The replies are gone for good, so a later sync() must not try to read them again.
    assertFalse(pipeline.hasPipelinedResponse());
    pipeline.sync();
    verify(connection, times(1)).getMany(anyInt());
  }

  @Test
  void syncAndReturnAllCompletesBufferedResponsesWithTheConnectionError() {
    Pipeline pipeline = new Pipeline(connection);
    Response<String> reply = pipeline.set("foo", "bar");

    assertSame(failure, assertThrows(JedisConnectionException.class, pipeline::syncAndReturnAll));
    assertSame(failure, assertThrows(JedisConnectionException.class, reply::get));
    assertFalse(pipeline.hasPipelinedResponse());
  }

  @Test
  void nonConnectionFailureIsRethrownAndCompletesTheResponses() {
    IllegalStateException bug = new IllegalStateException("push consumer failed");
    doThrow(bug).when(connection).getMany(anyInt());
    Pipeline pipeline = new Pipeline(connection);
    Response<String> reply = pipeline.set("foo", "bar");

    assertSame(bug, assertThrows(IllegalStateException.class, pipeline::sync));
    JedisException lost = assertThrows(JedisException.class, reply::get);
    assertFalse(lost instanceof JedisConnectionException);
    assertSame(bug, lost.getCause());
    assertFalse(pipeline.hasPipelinedResponse());
  }

  @Test
  void errorIsRethrownAndReportedByTheResponsesAsTheCause() {
    Error error = new Error("out of memory");
    doThrow(error).when(connection).getMany(anyInt());
    Pipeline pipeline = new Pipeline(connection);
    Response<String> reply = pipeline.set("foo", "bar");

    assertSame(error, assertThrows(Error.class, pipeline::sync));
    assertSame(error, assertThrows(JedisException.class, reply::get).getCause());
    assertFalse(pipeline.hasPipelinedResponse());
  }
}
