package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisDataException;

public class Response<T> {
  protected T response = null;
  protected JedisDataException exception = null;

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

  public T get() {
    // if response has dependency response and dependency is not built,
    // build it first and no more!!
    if (dependency != null && dependency.set && !dependency.built) {
      dependency.build();
    }
    if (!set) {
      throw new JedisDataException(
          "Please close pipeline or multi block before calling this method.");
    }
    if (!built) {
      build();
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

  public String toString() {
    return "Response " + builder.toString();
  }

}
