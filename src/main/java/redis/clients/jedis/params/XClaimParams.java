package redis.clients.jedis.params;

public class XClaimParams extends Params {

  private static final String IDLE = "IDLE";
  private static final String TIME = "TIME";
  private static final String RETRYCOUNT = "RETRYCOUNT";
  private static final String FORCE = "FORCE";

  public XClaimParams() {
  }

  public static XClaimParams xclaimParams() {
    return new XClaimParams();
  }

  /**
   * Set the idle time (last time it was delivered) of the message.
   * @param idleTime
   * @return XClaimParams
   */
  public XClaimParams idle(long idleTime) {
    addParam(IDLE, idleTime);
    return this;
  }

  /**
   * Set the idle time to a specific Unix time (in milliseconds).
   * @param idleUnixTime
   * @return XClaimParams
   */
  public XClaimParams time(long idleUnixTime) {
    addParam(TIME, idleUnixTime);
    return this;
  }

  /**
   * Set the retry counter to the specified value.
   * @param count
   * @return XClaimParams
   */
  public XClaimParams retryCount(int count) {
    addParam(RETRYCOUNT, count);
    return this;
  }

  /**
   * Creates the pending message entry in the PEL even if certain specified IDs are not already in
   * the PEL assigned to a different client.
   * @return XClaimParams
   */
  public XClaimParams force() {
    addParam(FORCE);
    return this;
  }
}
