package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

/**
 * BaseSetExParams is a base class for setting expiration parameters for Redis keys.
 * It provides methods to set expiration times in seconds or milliseconds, 
 * as well as Unix timestamps for expiration.
 * 
 * <ul>
 *   <li>{@link #ex(long)} - Set the specified expire time, in seconds.</li>
 *   <li>{@link #px(long)} - Set the specified expire time, in milliseconds.</li>
 *   <li>{@link #exAt(long)} - Set the specified Unix time at which the key will expire, in seconds.</li>
 *   <li>{@link #pxAt(long)} - Set the specified Unix time at which the key will expire, in milliseconds.</li>
 *   <li>{@link #keepTtl()} - Retain the time to live associated with the key.</li>
 * </ul>
 * 
 * @param <T> the type of the subclass extending this base class
 */
class BaseSetExParams<T extends BaseSetExParams<T>> implements IParams {

  private Keyword expiration;
  private Long expirationValue;

  private T expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return (T) this;
  }

  /**
   * Set the specified expire time, in seconds.
   * @param remainingSeconds
   * @return params object
   */
  public T ex(long remainingSeconds) {
    return expiration(Keyword.EX, remainingSeconds);
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @param remainingMilliseconds
   * @return params object
   */
  public T px(long remainingMilliseconds) {
    return expiration(Keyword.PX, remainingMilliseconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param timestampSeconds
   * @return params object
   */
  public T exAt(long timestampSeconds) {
    return expiration(Keyword.EXAT, timestampSeconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param timestampMilliseconds
   * @return params object
   */
  public T pxAt(long timestampMilliseconds) {
    return expiration(Keyword.PXAT, timestampMilliseconds);
  }

  /**
   * @deprecated Use {@link BaseSetExParams#keepTtl()}.
   * @return params object
   */
  @Deprecated
  public T keepttl() {
    return keepTtl();
  }

  /**
   * Retain the time to live associated with the key.
   * @return params object
   */
  public T keepTtl() {
    return expiration(Keyword.KEEPTTL, null);
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
    BaseSetExParams setParams = (BaseSetExParams) o;
    return Objects.equals(expiration, setParams.expiration) 
      && Objects.equals(expirationValue, setParams.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expiration, expirationValue);
  }
}
