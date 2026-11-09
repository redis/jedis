package redis.clients.jedis;

import java.util.function.Supplier;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

public class Response<T> implements Supplier<T> {
  protected T response = null;
  protected JedisDataException exception = null;
  // The failure that stopped the reply from being read; kept apart from the error reply above,
  // whose protected type subclasses may rely on.
  private JedisException failure = null;

  private boolean building = false;
  private boolean built = false;
  private boolean set = false;

  private Builder<T> builder;
  private Object data;
  private Response<?> dependency = null;

  public Response(Builder<T> b) {
    this.builder = b;
  }

  public void set(Object data) {
    this.data = data;
    set = true;
  }

  /**
   * Completes this response with the failure that stopped its reply from being read, so that
   * {@link #get()} rethrows it instead of reporting the response as not synced yet. A
   * {@link JedisException} is kept as is; any other throwable, including an {@link Error}, is
   * wrapped in a {@code JedisException}. Has no effect on a response already completed.
   */
  void completeExceptionally(Throwable failure) {
    if (set) {
      return;
    }
    this.failure = failure instanceof JedisException ? (JedisException) failure
        : new JedisException("Failed to read reply", failure);
    this.data = null;
    this.set = true;
    this.built = true;
  }

  @Override
  public T get() {
    // if response has dependency response and dependency is not built, build it first and no more!!
    if (dependency != null && dependency.set && !dependency.built) {
      dependency.build();
    }
    if (!set) {
      throw new IllegalStateException(
          "Please close pipeline or multi block before calling this method.");
    }
    if (!built) {
      build();
    }
    if (failure != null) {
      throw failure;
    }
    if (exception != null) {
      throw exception;
    }
    return response;
  }

  public void setDependency(Response<?> dependency) {
    this.dependency = dependency;
  }

  private void build() {
    // check build state to prevent recursion
    if (building) {
      return;
    }

    building = true;
    try {
      if (data != null) {
        if (data instanceof JedisDataException) {
          exception = (JedisDataException) data;
        } else {
          response = builder.build(data);
        }
      }

      data = null;
    } finally {
      building = false;
      built = true;
    }
  }

  @Override
  public String toString() {
    return "Response " + builder.toString();
  }

  static class DecodedResponse<T> extends Response<T> {

    private DecodedResponse(T response, JedisDataException exception) {
      super(null);
      this.exception = exception;
      this.response = response;
    }

    @Override
    public T get() {
      if (exception != null) {
        throw exception;
      }

      return response;
    }

    @Override
    public String toString() {
      return "Response<T>";
    }
  }

  public static <T> Response<T> of(T response) {
    return new DecodedResponse<>(response, null);
  }

  public static <T> Response<T> error(JedisDataException exception) {
    return new DecodedResponse<>(null, exception);
  }
}
