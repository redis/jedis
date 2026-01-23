package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

/**
 * Parameters for the XCFGSET command to configure idempotent producer settings for a stream.
 */
public class XCfgSetParams implements IParams {

  private Integer idmpDuration;
  private Integer idmpMaxsize;

  public static XCfgSetParams xCfgSetParams() {
    return new XCfgSetParams();
  }

  /**
   * Set the duration (in seconds) that Redis keeps each idempotent ID.
   * Minimum value: 1 second; Maximum value: 86400 seconds (24h); default value: 100 seconds.
   *
   * @param duration duration in seconds
   * @return XCfgSetParams
   */
  public XCfgSetParams idmpDuration(int duration) {
    this.idmpDuration = duration;
    return this;
  }

  /**
   * Set the maximum number of most recent idempotent IDs that Redis keeps for each producer ID.
   * Minimum value: 1; Maximum value: 10000; default value: 100.
   *
   * @param maxsize maximum number of idempotent IDs per producer
   * @return XCfgSetParams
   */
  public XCfgSetParams idmpMaxsize(int maxsize) {
    this.idmpMaxsize = maxsize;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (idmpDuration != null) {
      args.add("IDMP-DURATION").add(idmpDuration);
    }
    if (idmpMaxsize != null) {
      args.add("IDMP-MAXSIZE").add(idmpMaxsize);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XCfgSetParams that = (XCfgSetParams) o;
    return Objects.equals(idmpDuration, that.idmpDuration) 
        && Objects.equals(idmpMaxsize, that.idmpMaxsize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(idmpDuration, idmpMaxsize);
  }
}

