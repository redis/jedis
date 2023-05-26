package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class XReadGroupParams implements IParams {

  private Integer count = null;
  private Integer block = null;
  private boolean noack = false;

  public static XReadGroupParams xReadGroupParams() {
    return new XReadGroupParams();
  }

  public XReadGroupParams count(int count) {
    this.count = count;
    return this;
  }

  public XReadGroupParams block(int block) {
    this.block = block;
    return this;
  }

  public XReadGroupParams noAck() {
    this.noack = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (count != null) {
      args.add(Keyword.COUNT).add(count);
    }
    if (block != null) {
      args.add(Keyword.BLOCK).add(block).blocking();
    }
    if (noack) {
      args.add(Keyword.NOACK);
    }
  }
}
