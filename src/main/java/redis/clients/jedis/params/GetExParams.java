package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class GetExParams implements IParams {

  private Keyword expiration;
  private Long expirationValue;

  private boolean persist;

  public GetExParams() {
  }

  public static GetExParams getExParams() {
    return new GetExParams();
  }

  private GetExParams expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return this;
  }

  /**
   * Set the specified expire time, in seconds.
   * @return GetExParams
   */
  public GetExParams ex(long secondsToExpire) {
    return expiration(Keyword.EX, secondsToExpire);
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @return GetExParams
   */
  public GetExParams px(long millisecondsToExpire) {
    return expiration(Keyword.PX, millisecondsToExpire);
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param seconds
   * @return GetExParams
   */
  public GetExParams exAt(long seconds) {
    return expiration(Keyword.EXAT, seconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param milliseconds
   * @return GetExParams
   */
  public GetExParams pxAt(long milliseconds) {
    return expiration(Keyword.PXAT, milliseconds);
  }

  /**
   * Remove the time to live associated with the key.
   * @return GetExParams
   */
  public GetExParams persist() {
    return expiration(Keyword.PERSIST, null);
  }

  @Override
  public void addParams(CommandArguments args) {
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
    GetExParams that = (GetExParams) o;
    return persist == that.persist && expiration == that.expiration && Objects.equals(expirationValue, that.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expiration, expirationValue, persist);
  }
}
