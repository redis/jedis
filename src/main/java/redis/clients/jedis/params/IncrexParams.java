package redis.clients.jedis.params;

import java.util.Objects;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Parameters for the {@code INCREX} command.
 * <p>
 * Usage examples:
 *
 * <pre>
 *   new IncrexParams().ubound(100).saturate().ex(60)
 *   new IncrexParams().lbound(0).ubound(100).saturate()
 *   IncrexParams.increxParams().ex(30).enx()
 * </pre>
 *
 * @since 8.0
 */
public class IncrexParams extends BaseGetExParams<IncrexParams> {

  private Number lbound;
  private Number ubound;
  private boolean saturate = false;
  private boolean enx = false;

  public static IncrexParams increxParams() {
    return new IncrexParams();
  }

  public IncrexParams lbound(long lbound) {
    this.lbound = lbound;
    return this;
  }

  public IncrexParams lbound(double lbound) {
    this.lbound = lbound;
    return this;
  }

  public IncrexParams ubound(long ubound) {
    this.ubound = ubound;
    return this;
  }

  public IncrexParams ubound(double ubound) {
    this.ubound = ubound;
    return this;
  }

  public IncrexParams saturate() {
    this.saturate = true;
    return this;
  }

  public IncrexParams enx() {
    this.enx = true;
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

    if (saturate) {
      args.add(Keyword.SATURATE);
    }

    // expiration (EX/PX/EXAT/PXAT/PERSIST) from BaseGetExParams
    super.addParams(args);

    if (enx) {
      args.add(Keyword.ENX);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IncrexParams)) return false;
    if (!super.equals(o)) return false;
    IncrexParams that = (IncrexParams) o;
    return enx == that.enx && saturate == that.saturate && Objects.equals(lbound, that.lbound)
        && Objects.equals(ubound, that.ubound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lbound, ubound, saturate, enx);
  }
}
