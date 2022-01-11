package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;

import static redis.clients.jedis.Protocol.Keyword.BYLEX;
import static redis.clients.jedis.Protocol.Keyword.BYSCORE;
import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.Keyword.REV;


public class ZRangeParams implements IParams{
  private boolean byScore = false;
  private boolean byLex = false;
  private boolean limit = false;
  private boolean rev = false;

  private int offset;
  private int count;

  public ZRangeParams() {}

  public static ZRangeParams ZRangeParams() {
    return new ZRangeParams();
  }

  public ZRangeParams byScore() {
    this.byScore = true;
    return this;
  }

  public ZRangeParams byLex() {
    this.byLex = true;
    return this;
  }

  public ZRangeParams limit(int offset, int count) {
    this.limit = true;
    this.offset = offset;
    this.count = count;
    return this;
  }

  public ZRangeParams reverse() {
    this.rev = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (this.byScore) {
      args.add(BYSCORE);
    } else if (this.byLex) {
      args.add(BYLEX);
    }

    if (this.rev) {
      args.add(REV);
    }

    if (this.limit) {
      args.add(LIMIT);
      args.add(this.offset);
      args.add(this.count);
    }
  }
}
