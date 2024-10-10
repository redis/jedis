package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XReadGroupParams that = (XReadGroupParams) o;
    return noack == that.noack && Objects.equals(count, that.count) && Objects.equals(block, that.block);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, block, noack);
  }
}
