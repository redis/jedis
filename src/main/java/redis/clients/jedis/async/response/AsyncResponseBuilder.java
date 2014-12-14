package redis.clients.jedis.async.response;

import redis.clients.jedis.exceptions.JedisException;

import java.nio.ByteBuffer;

public interface AsyncResponseBuilder<T> {
  public void appendPartialResponse(final ByteBuffer buffer);

  public boolean isComplete();

  public T getResponse();

  JedisException getException();
}
