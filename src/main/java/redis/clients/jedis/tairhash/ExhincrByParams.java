package redis.clients.jedis.tairhash;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

public class ExhincrByParams<T> extends ExhBaseParams {
  private static final String MIN = "min";
  private static final String MAX = "max";

  public ExhincrByParams() {
  }

  public static ExhincrByParams ExhincrByParams() {
    return new ExhincrByParams();
  }

  /**
   * Min boundary
   * @param min
   * @return ExhincrByParams
   */
  public ExhincrByParams min(T min) {
    addParam(MIN, min);
    return this;
  }

  /**
   * Max boundary
   * @param max
   * @return ExhincrByParams
   */
  public ExhincrByParams max(T max) {
    addParam(MAX, max);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    super.addParams(args);

    if (contains(MIN)) {
      args.add(MIN);
      args.add(Protocol.toByteArray((long) getParam(MIN)));
    }
    if (contains(MAX)) {
      args.add(MAX);
      args.add(Protocol.toByteArray((long) getParam(MAX)));
    }
  }
}
