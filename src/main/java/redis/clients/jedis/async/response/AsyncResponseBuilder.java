package redis.clients.jedis.async.response;

import redis.clients.jedis.exceptions.JedisException;

public interface AsyncResponseBuilder<T> {
  public void appendPartialResponse(byte b);

  public boolean isComplete();

  public T getResponse();

  JedisException getException();
}
