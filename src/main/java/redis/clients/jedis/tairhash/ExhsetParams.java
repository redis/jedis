package redis.clients.jedis.tairhash;

import redis.clients.jedis.CommandArguments;

public class ExhsetParams extends ExhBaseParams {
  private static final String XX = "xx";
  private static final String NX = "nx";

  public ExhsetParams() {
  }

  public static ExhsetParams ExhsetParams() {
    return new ExhsetParams();
  }

  /**
   * Only set the key if it already exist.
   * @return ExhsetParams
   */
  public ExhsetParams xx() {
    addParam(XX);
    return this;
  }

  /**
   * Only set the key if it does not already exist.
   * @return ExhsetParams
   */
  public ExhsetParams nx() {
    addParam(NX);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    super.addParams(args);

    if (contains(NX)) {
      args.add(NX);
    }
    if (contains(XX)) {
      args.add(XX);
    }
  }
}
