package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.IDLE;
import static redis.clients.jedis.Protocol.Keyword.TIME;
import static redis.clients.jedis.Protocol.Keyword.RETRYCOUNT;
import static redis.clients.jedis.Protocol.Keyword.FORCE;

import redis.clients.jedis.CommandArguments;

public class XClaimParams implements IParams {

  private Long idleTime;
  private Long idleUnixTime;
  private Integer retryCount;
  private boolean force;

  public XClaimParams() {
  }

  public static XClaimParams xClaimParams() {
    return new XClaimParams();
  }

  /**
   * Set the idle time (last time it was delivered) of the message.
   * @param idleTime
   * @return XClaimParams
   */
  public XClaimParams idle(long idleTime) {
    this.idleTime = idleTime;
    return this;
  }

  /**
   * Set the idle time to a specific Unix time (in milliseconds).
   * @param idleUnixTime
   * @return XClaimParams
   */
  public XClaimParams time(long idleUnixTime) {
    this.idleUnixTime = idleUnixTime;
    return this;
  }

  /**
   * Set the retry counter to the specified value.
   * @param count
   * @return XClaimParams
   */
  public XClaimParams retryCount(int count) {
    this.retryCount = count;
    return this;
  }

  /**
   * Creates the pending message entry in the PEL even if certain specified IDs are not already in
   * the PEL assigned to a different client.
   * @return XClaimParams
   */
  public XClaimParams force() {
    this.force = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (idleTime != null) {
      args.add(IDLE).add(idleTime);
    }
    if (idleUnixTime != null) {
      args.add(TIME).add(idleUnixTime);
    }
    if (retryCount != null) {
      args.add(RETRYCOUNT).add(retryCount);
    }
    if (force) {
      args.add(FORCE);
    }
  }
}
