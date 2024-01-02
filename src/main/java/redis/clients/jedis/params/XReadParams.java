package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class XReadParams implements IParams {

  private Integer count = null;
  private Integer block = null;

  public static XReadParams xReadParams() {
    return new XReadParams();
  }

  public XReadParams count(int count) {
    this.count = count;
    return this;
  }

  public XReadParams block(int block) {
    this.block = block;
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
  }
}
