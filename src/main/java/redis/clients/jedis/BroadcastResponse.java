package redis.clients.jedis;

import java.util.function.Supplier;

public class BroadcastResponse<T> implements Supplier<T> {

  private T response = null;
  private RuntimeException exception = null;

  public BroadcastResponse(T response) {
    this.response = response;
  }

  public BroadcastResponse(RuntimeException exception) {
    this.exception = exception;
  }

  @Override
  public T get() {
    if (exception != null) {
      throw exception;
    }
    return response;
  }
}
