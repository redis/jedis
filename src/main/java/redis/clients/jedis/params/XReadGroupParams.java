package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.BLOCK;
import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.NOACK;
import static redis.clients.jedis.Protocol.toByteArray;

import redis.clients.jedis.CommandArguments;

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
      args.add(COUNT);
      args.add(toByteArray(count));
    }
    if (block != null) {
      args.add(BLOCK);
      args.add(toByteArray(block));
      args.blocking();
    }
    if (noack) {
      args.add(NOACK);
    }
  }
}
