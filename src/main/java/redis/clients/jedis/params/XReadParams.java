package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.BLOCK;
import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.toByteArray;

import redis.clients.jedis.CommandArguments;

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
      args.add(COUNT);
      args.add(toByteArray(count));
    }
    if (block != null) {
      args.add(BLOCK);
      args.add(toByteArray(block));
      args.blocking();
    }
  }
}
