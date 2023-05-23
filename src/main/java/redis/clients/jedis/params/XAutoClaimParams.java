package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class XAutoClaimParams implements IParams {

  private Integer count;

  public XAutoClaimParams() {
  }

  public static XAutoClaimParams xAutoClaimParams() {
    return new XAutoClaimParams();
  }

  /**
   * Set the count of stream entries/ids to return as part of the command output.
   * @param count COUNT
   * @return XAutoClaimParams
   */
  public XAutoClaimParams count(int count) {
    this.count = count;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (count != null) {
      args.add(Keyword.COUNT.getRaw()).add(count);
    }
  }

}
