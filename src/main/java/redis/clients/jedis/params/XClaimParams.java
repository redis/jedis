package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

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

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    List<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);
    Collections.addAll(byteParams, args);

    if (contains(IDLE)) {
      byteParams.add(SafeEncoder.encode(IDLE));
      byteParams.add(Protocol.toByteArray((long) getParam(IDLE)));
    }
    if (contains(TIME)) {
      byteParams.add(SafeEncoder.encode(TIME));
      byteParams.add(Protocol.toByteArray((long) getParam(TIME)));
    }
    if (contains(RETRYCOUNT)) {
      byteParams.add(SafeEncoder.encode(RETRYCOUNT));
      byteParams.add(Protocol.toByteArray((int) getParam(RETRYCOUNT)));
    }
    if (contains(FORCE)) {
      byteParams.add(SafeEncoder.encode(FORCE));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
