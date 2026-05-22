package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.List;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;

/**
 * Result of the {@code INCREX} command.
 * @param <T> Number type — {@link Long} for integer mode, {@link Double} for float mode
 * @since 8.0
 */
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
   * The actual increment applied. May differ from the requested increment when {@code SATURATE} is
   * active and the result is clamped to a bound. Returns {@code 0} when the operation was silently
   * rejected due to an out-of-bounds result and {@code SATURATE} was not set.
   */
  public T getIncrement() {
    return increment;
  }

  @Override
  public String toString() {
    return "IncrexResponse{value=" + value + ", increment=" + increment + "}";
  }

  public static final Builder<IncrexResponse<Long>> INCREX_RESPONSE_LONG = new Builder<IncrexResponse<Long>>() {
    @Override
    public IncrexResponse<Long> build(Object data) {
      if (data == null) return null;
      List<Long> list = BuilderFactory.LONG_LIST.build(data);
      return new IncrexResponse<>(list.get(0), list.get(1));
    }

    @Override
    public String toString() {
      return "IncrexResponse<Long>";
    }
  };

  public static final Builder<IncrexResponse<Double>> INCREX_RESPONSE_DOUBLE = new Builder<IncrexResponse<Double>>() {
    @Override
    public IncrexResponse<Double> build(Object data) {
      if (data == null) return null;
      List<Double> list = BuilderFactory.DOUBLE_LIST.build(data);
      return new IncrexResponse<>(list.get(0), list.get(1));
    }

    @Override
    public String toString() {
      return "IncrexResponse<Double>";
    }
  };
}
