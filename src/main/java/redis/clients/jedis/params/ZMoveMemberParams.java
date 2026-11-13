package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Optional arguments for the {@code ZMOVEMEMBER} command: a condition on the destination sorted
 * set.
 * <p>
 * {@link #nx()} and {@link #xx()} are mutually exclusive; the last call wins. The condition is
 * emitted before the {@code MEMBERS} block; an instance with no condition emits nothing.
 * @since 8.1
 */
public class ZMoveMemberParams implements IParams {

  private Keyword existence;

  public ZMoveMemberParams() {
  }

  public static ZMoveMemberParams zMoveMemberParams() {
    return new ZMoveMemberParams();
  }

  /**
   * Move a member only if the destination does not have it. A member left out stays in the source.
   * @return ZMoveMemberParams
   */
  public ZMoveMemberParams nx() {
    this.existence = Keyword.NX;
    return this;
  }

  /**
   * Move a member only if the destination already has it. A member left out stays in the source.
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
