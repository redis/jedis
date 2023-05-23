package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.MAXLEN;
import static redis.clients.jedis.Protocol.Keyword.RANK;

import redis.clients.jedis.CommandArguments;

public class LPosParams implements IParams {

  private Integer rank;
  private Integer maxlen;
  
  public static LPosParams lPosParams() {
    return new LPosParams();
  }

  public LPosParams rank(int rank) {
    this.rank = rank;
    return this;
  }

  public LPosParams maxlen(int maxLen) {
    this.maxlen = maxLen;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (rank != null) {
      args.add(RANK).add(rank);
    }

    if (maxlen != null) {
      args.add(MAXLEN).add(maxlen);
    }
  }

}
