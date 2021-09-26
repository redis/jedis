package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

public class LPosParams extends Params implements IParams {

  private static final String RANK = "RANK";
  private static final String MAXLEN = "MAXLEN";

  public static LPosParams lPosParams() {
    return new LPosParams();
  }

  public LPosParams rank(int rank) {
    addParam(RANK, rank);
    return this;
  }

  public LPosParams maxlen(int maxLen) {
    addParam(MAXLEN, maxLen);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (contains(RANK)) {
      args.addObject(RANK);
      args.addObject(Protocol.toByteArray((int) getParam(RANK)));
    }

    if (contains(MAXLEN)) {
      args.addObject(MAXLEN);
      args.addObject(Protocol.toByteArray((int) getParam(MAXLEN)));
    }
  }

}
