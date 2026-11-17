package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Parameters of the SADDEX command: an optional whole-command condition on the members (MNX or MXX)
 * and an optional expiration for the members (EX, PX, EXAT, PXAT or KEEPTTL).
 * <p>
 * Each of the two groups is a single slot where the last call wins. The arguments are emitted in
 * the order {@code [MNX|MXX] [EX seconds|PX milliseconds|EXAT unix-time-seconds|PXAT
 * unix-time-milliseconds|KEEPTTL]}; the {@code MEMBERS} block is added by the command itself.
 * <p>
 * Example usage:
 * 
 * <pre>
 * {
 *   &#64;code
 *   SAddExParams params = SAddExParams.sAddExParams().mnx().ex(3600);
 * }
 * </pre>
 * 
 * @see HSetExParams
 * @since 8.1
 */
public class SAddExParams extends BaseSetExParams<SAddExParams> {

  private Keyword condition;

  public static SAddExParams sAddExParams() {
    return new SAddExParams();
  }

  /**
   * Only add the members if none of them already exists.
   * @return SAddExParams
   */
  public SAddExParams mnx() {
    this.condition = Keyword.MNX;
    return this;
  }

  /**
   * Only apply the command if all of the members already exist.
   * @return SAddExParams
   */
  public SAddExParams mxx() {
    this.condition = Keyword.MXX;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (condition != null) {
      args.add(condition);
    }
    super.addParams(args);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SAddExParams that = (SAddExParams) o;
    return Objects.equals(condition, that.condition) && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, super.hashCode());
  }
}
