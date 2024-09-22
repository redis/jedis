package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XReadParams that = (XReadParams) o;
    return Objects.equals(count, that.count) && Objects.equals(block, that.block);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, block);
  }
}
