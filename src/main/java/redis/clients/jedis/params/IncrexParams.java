package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Parameters for the integer-bounded form of the {@code INCREX} command. Accepts {@code long}
 * lower/upper bounds; paired with the
 * {@link redis.clients.jedis.commands.StringCommands#increx(String, long, IncrexParams) increx(key,
 * long, IncrexParams)} overload (sends {@code BYINT} on the wire).
 * <p>
 * For float-bounded INCREX use {@link IncrexFloatParams}.
 * <p>
 * Usage examples:
 *
 * <pre>
 *   new IncrexParams().lbound(0).ubound(100).saturate().ex(60)
 *   IncrexParams.increxParams().ex(30).enx()
 * </pre>
 *
 * @since 8.0
 */
public class IncrexParams extends BaseIncrexParams<IncrexParams> {

  private Long lbound;
  private Long ubound;

  public static IncrexParams increxParams() {
    return new IncrexParams();
  }

  public IncrexParams lbound(long lbound) {
    this.lbound = lbound;
    return this;
  }

  public IncrexParams ubound(long ubound) {
    this.ubound = ubound;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (lbound != null) {
      args.add(Keyword.LBOUND);
      args.add(lbound);
    }
    if (ubound != null) {
      args.add(Keyword.UBOUND);
      args.add(ubound);
    }

    super.addParams(args);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IncrexParams)) return false;
    if (!super.equals(o)) return false;
    IncrexParams that = (IncrexParams) o;
    return Objects.equals(lbound, that.lbound) && Objects.equals(ubound, that.ubound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lbound, ubound);
  }
}
