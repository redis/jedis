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
 *   new IncrexParams().ubound(100).overflow(Overflow.SAT).ex(60)
 *   new IncrexParams().lbound(0).ubound(100).overflow(Overflow.REJECT)
 *   IncrexParams.increxParams().ex(30).enx()
 * </pre>
 *
 * @since 8.0
 */
public class IncrexParams extends BaseGetExParams<IncrexParams> {

  public enum Overflow {
    FAIL, SAT, REJECT
  }

  private Number lbound;
  private Number ubound;
  private Overflow overflow;
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

  public IncrexParams overflow(Overflow overflow) {
    this.overflow = overflow;
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

    if (overflow != null) {
      args.add(Keyword.OVERFLOW);
      switch (overflow) {
        case FAIL:
          args.add(Keyword.FAIL);
          break;
        case SAT:
          args.add(Keyword.SAT);
          break;
        case REJECT:
          args.add(Keyword.REJECT);
          break;
      }
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
    return enx == that.enx && Objects.equals(lbound, that.lbound)
        && Objects.equals(ubound, that.ubound) && overflow == that.overflow;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lbound, ubound, overflow, enx);
  }
}
