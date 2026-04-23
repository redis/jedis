package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

/**
 * Parameters for the {@code XNACK} command. Supports optional {@code RETRYCOUNT count} and
 * {@code FORCE} arguments.
 */
public class XNackParams implements IParams {

  private Long retryCount;
  private boolean force;

  public XNackParams() {
  }

  public static XNackParams xNackParams() {
    return new XNackParams();
  }

  /**
   * Overrides the mode's implicit delivery counter adjustment with an exact value. Useful for AOF
   * rewrite and cases where explicit control is desired.
   * @param count must be &gt;= 0
   * @return XNackParams
   */
  public XNackParams retryCount(long count) {
    this.retryCount = count;
    return this;
  }

  /**
   * Creates a new unowned PEL entry for any ID not already in the group's PEL, rather than silently
   * skipping it. Intended primarily for AOF rewrite.
   * @return XNackParams
   */
  public XNackParams force() {
    this.force = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (retryCount != null) {
      args.add(Keyword.RETRYCOUNT).add(retryCount);
    }
    if (force) {
      args.add(Keyword.FORCE);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XNackParams that = (XNackParams) o;
    return force == that.force && Objects.equals(retryCount, that.retryCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(retryCount, force);
  }
}
