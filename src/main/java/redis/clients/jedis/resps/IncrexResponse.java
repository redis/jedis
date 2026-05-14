package redis.clients.jedis.resps;

import java.io.Serializable;
import redis.clients.jedis.annots.Experimental;

/**
 * Result of the {@code INCREX} command.
 * @param <T> Number type — {@link Long} for integer mode, {@link Double} for float mode
 */
@Experimental
public class IncrexResponse<T extends Number> implements Serializable {

  private static final long serialVersionUID = 1L;

  private final T value;
  private final T increment;

  public IncrexResponse(T value, T increment) {
    this.value = value;
    this.increment = increment;
  }

  /**
   * The key's value after the increment operation.
   */
  public T getValue() {
    return value;
  }

  /**
   * The actual increment applied. May differ from the requested increment when overflow SAT is
   * active.
   */
  public T getIncrement() {
    return increment;
  }

  @Override
  public String toString() {
    return "IncrexResponse{value=" + value + ", increment=" + increment + "}";
  }
}
