package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

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
      args.add(Keyword.RANK).add(rank);
    }

    if (maxlen != null) {
      args.add(Keyword.MAXLEN).add(maxlen);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LPosParams that = (LPosParams) o;
    return Objects.equals(rank, that.rank) && Objects.equals(maxlen, that.maxlen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rank, maxlen);
  }
}
