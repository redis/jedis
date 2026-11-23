package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Optional destination condition for {@code ZMOVEMEMBER}: {@link #nx()} or {@link #xx()}; the last
 * call wins.
 * @since 8.1
 */
public class ZMoveMemberParams implements IParams {

  private Keyword existence;

  /**
   * Creates params with no destination condition.
   */
  public ZMoveMemberParams() {
  }

  /**
   * Creates params with no destination condition; add one with {@link #nx()} or {@link #xx()}.
   * @return ZMoveMemberParams
   */
  public static ZMoveMemberParams zMoveMemberParams() {
    return new ZMoveMemberParams();
  }

  /**
   * Move a member only if the destination does not have it.
   * @return ZMoveMemberParams
   */
  public ZMoveMemberParams nx() {
    this.existence = Keyword.NX;
    return this;
  }

  /**
   * Move a member only if the destination already has it.
   * @return ZMoveMemberParams
   */
  public ZMoveMemberParams xx() {
    this.existence = Keyword.XX;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (existence != null) {
      args.add(existence);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZMoveMemberParams that = (ZMoveMemberParams) o;
    return existence == that.existence;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(existence);
  }
}
