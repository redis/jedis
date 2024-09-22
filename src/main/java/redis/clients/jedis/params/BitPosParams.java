package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.BitCountOption;

import java.util.Objects;

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

  /**
   * {@link BitPosParams#start(long) START} must be set for END option.
   */
  public BitPosParams end(long end) {
    this.end = end;
    return this;
  }

  /**
   * Both {@link BitPosParams#start(long) START} and {@link BitPosParams#end(long) END} both must be
   * set for MODIFIER option.
   */
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BitPosParams that = (BitPosParams) o;
    return Objects.equals(start, that.start) && Objects.equals(end, that.end) && Objects.equals(modifier, that.modifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, modifier);
  }
}
