package redis.clients.jedis;

import java.util.function.Supplier;
import redis.clients.jedis.exceptions.JedisDataException;

public class Response<T> implements Supplier<T> {
  protected T response = null;
  protected JedisDataException exception = null;

  private boolean isResponseBuilding = false;
  private boolean isResponseBuilt = false;
  private boolean isDataSet = false;

  private Builder<T> builder;
  private Object data;
  private Response<?> dependency = null;

  public Response(Builder<T> b) {
    this.builder = b;
  }

  public void set(Object data) {
    this.data = data;
    isDataSet = true;
  }

  @Override
  public T get() {
    // if response has dependency response and dependency is not built, build it first and no more!!
    boolean dependencyNeedsBuilding = dependency != null && dependency.isDataSet && !dependency.isResponseBuilt;
    if (dependencyNeedsBuilding) {
      dependency.build();
    }
    if (!isDataSet) {
      throw new IllegalStateException(
          "Please close pipeline or multi block before calling this method.");
    }
    if (!isResponseBuilt) {
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
    if (isResponseBuilding) {
      return;
    }

    isResponseBuilding = true;
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
      isResponseBuilding = false;
      isResponseBuilt = true;
    }
  }

  @Override
  public String toString() {
    return "Response " + builder.toString();
  }

}
