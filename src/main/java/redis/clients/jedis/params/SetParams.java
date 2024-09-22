package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class SetParams implements IParams {

  private Keyword existance;
  private Keyword expiration;
  private Long expirationValue;

  public SetParams() {
  }

  public static SetParams setParams() {
    return new SetParams();
  }

  /**
   * Only set the key if it does not already exist.
   * @return SetParams
   */
  public SetParams nx() {
    this.existance = Keyword.NX;
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return SetParams
   */
  public SetParams xx() {
    this.existance = Keyword.XX;
    return this;
  }

  private SetParams expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return this;
  }

  /**
   * Set the specified expire time, in seconds.
   * @param remainingSeconds
   * @return SetParams
   */
  public SetParams ex(long remainingSeconds) {
    return expiration(Keyword.EX, remainingSeconds);
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @param remainingMilliseconds
   * @return SetParams
   */
  public SetParams px(long remainingMilliseconds) {
    return expiration(Keyword.PX, remainingMilliseconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param timestampSeconds
   * @return SetParams
   */
  public SetParams exAt(long timestampSeconds) {
    return expiration(Keyword.EXAT, timestampSeconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param timestampMilliseconds
   * @return SetParams
   */
  public SetParams pxAt(long timestampMilliseconds) {
    return expiration(Keyword.PXAT, timestampMilliseconds);
  }

  /**
   * Retain the time to live associated with the key.
   * @return SetParams
   */
  // TODO: deprecate?
  public SetParams keepttl() {
    return keepTtl();
  }

  /**
   * Retain the time to live associated with the key.
   * @return SetParams
   */
  public SetParams keepTtl() {
    return expiration(Keyword.KEEPTTL, null);
  }

  @Override
  public void addParams(CommandArguments args) {
    if (existance != null) {
      args.add(existance);
    }

    if (expiration != null) {
      args.add(expiration);
      if (expirationValue != null) {
        args.add(expirationValue);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetParams setParams = (SetParams) o;
    return Objects.equals(existance, setParams.existance) && Objects.equals(expiration, setParams.expiration)
            && Objects.equals(expirationValue, setParams.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(existance, expiration, expirationValue);
  }
}
