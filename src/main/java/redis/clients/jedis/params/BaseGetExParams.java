package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Abstract base class for setting expiration parameters for Redis GET commands.
 * This class provides methods to set various expiration options such as EX, PX, EXAT, PXAT, and PERSIST.
 *
 * <ul>
 *   <li>{@link #ex(long)} - Set the specified expire time, in seconds.</li>
 *   <li>{@link #px(long)} - Set the specified expire time, in milliseconds.</li>
 *   <li>{@link #exAt(long)} - Set the specified Unix time at which the key will expire, in seconds.</li>
 *   <li>{@link #pxAt(long)} - Set the specified Unix time at which the key will expire, in milliseconds.</li>
 *   <li>{@link #persist()} - Remove the time to live associated with the key.</li>
 * </ul>
 * 
 * @param <T> the type of the subclass extending this base class
 */
abstract class BaseGetExParams<T extends BaseGetExParams> implements IParams {

  private Keyword expiration;
  private Long expirationValue;

  private T expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return (T) this;
  }

  /**
   * Set the specified expire time, in seconds.
   * @return parameter object
   */
  public T ex(long secondsToExpire) {
    return expiration(Keyword.EX, secondsToExpire);
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @return parameter object
   */
  public T px(long millisecondsToExpire) {
    return expiration(Keyword.PX, millisecondsToExpire);
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param seconds
   * @return parameter object
   */
  public T exAt(long seconds) {
    return expiration(Keyword.EXAT, seconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param milliseconds
   * @return parameter object
   */
  public T pxAt(long milliseconds) {
    return expiration(Keyword.PXAT, milliseconds);
  }

  /**
   * Remove the time to live associated with the key.
   * @return parameter object
   */
  public T persist() {
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
    BaseGetExParams that = (BaseGetExParams) o;
    return expiration == that.expiration && Objects.equals(expirationValue, that.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expiration, expirationValue);
  }
}
