package redis.clients.jedis.tests.commands.async.util;

import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.exceptions.JedisException;

public class AsyncJUnitTestCallback<T> implements AsyncResponseCallback<T> {
  private T response = null;
  private JedisException exception = null;
  private boolean complete = false;

  @Override
  public void execute(T response, JedisException exc) {
    try {
      this.response = response;
      this.exception = exc;
    } finally {
      complete = true;
    }
  }

  public T getResponse() {
    return response;
  }

  public T getResponseWithWaiting(int timeoutMs) {
    waitForComplete(timeoutMs);
    if (!isComplete()) {
      throw new JedisException("Response not received yet");
    }

    return getResponse();
  }

  public JedisException getException() {
    return exception;
  }

  public boolean isComplete() {
    return complete;
  }

  public void waitForComplete(int timeoutMs) {
    int currentMs = 0;
    while (!complete && currentMs < timeoutMs) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
      }
      currentMs += 10;
    }

    if (getException() != null) {
      throw getException();
    }
  }

  public void reset() {
    response = null;
    exception = null;
    complete = false;
  }

  public AsyncJUnitTestCallback<T> withReset() {
    reset();
    return this;
  }
}
