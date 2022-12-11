package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.BitCountOption;

public class BitPosParams implements IParams {

  private Long start;
  private Long end;
  private BitCountOption modifier;

  public BitPosParams() {
  }

  // TODO: deprecate ??
  public BitPosParams(long start) {
    this.start = start;
  }

  // TODO: deprecate ??
  public BitPosParams(long start, long end) {
    this(start);

    this.end = end;
  }

  public static BitPosParams bitPosParams() {
    return new BitPosParams();
  }

  public BitPosParams start(long start) {
    this.start = start;
    return this;
  }

  public BitPosParams end(long end) {
    this.end = end;
    return this;
  }

  public BitPosParams modifier(BitCountOption modifier) {
    this.modifier = modifier;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (start != null) {
      args.add(start);
      if (end != null) {
        args.add(end);
        if (modifier != null) {
          args.add(modifier);
        }
      }
    }
  }
}
