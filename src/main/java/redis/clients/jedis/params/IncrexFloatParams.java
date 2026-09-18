package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Parameters for the floating-point-bounded form of the {@code INCREX} command. Accepts
 * {@code double} lower/upper bounds; paired with the
 * {@link redis.clients.jedis.commands.StringCommands#increx(String, double, IncrexFloatParams)
 * increx(key, double, IncrexFloatParams)} overload (sends {@code BYFLOAT} on the wire).
 * <p>
 * For integer-bounded INCREX use {@link IncrexParams}.
 * <p>
 * Usage examples:
 *
 * <pre>
 *   new IncrexFloatParams().lbound(0.0).ubound(100.0).saturate().ex(60)
 *   IncrexFloatParams.increxFloatParams().ex(30).enx()
 * </pre>
 *
 * @since 8.0
 */
public class IncrexFloatParams extends BaseIncrexParams<IncrexFloatParams> {

  private Double lbound;
  private Double ubound;

  public static IncrexFloatParams increxFloatParams() {
    return new IncrexFloatParams();
  }

  public IncrexFloatParams lbound(double lbound) {
    this.lbound = lbound;
    return this;
  }

  public IncrexFloatParams ubound(double ubound) {
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
    if (!(o instanceof IncrexFloatParams)) return false;
    if (!super.equals(o)) return false;
    IncrexFloatParams that = (IncrexFloatParams) o;
    return Objects.equals(lbound, that.lbound) && Objects.equals(ubound, that.ubound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lbound, ubound);
  }
}
