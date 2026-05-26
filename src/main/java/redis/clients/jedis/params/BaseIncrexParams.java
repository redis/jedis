package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * Abstract base for {@code INCREX} parameters. Owns options common to both the integer-bounded
 * variant ({@link IncrexParams}) and the float-bounded variant ({@link IncrexFloatParams}):
 * {@code SATURATE}, the {@code EX}/{@code PX}/{@code EXAT}/{@code PXAT}/{@code PERSIST} expiration
 * block, and {@code ENX}.
 * <p>
 * Bounds ({@code LBOUND}/{@code UBOUND}) live on the concrete subclasses so the bound type matches
 * the increment type at compile time &mdash; integer-mode {@code increx} only accepts
 * {@link IncrexParams}, float-mode {@code increx} only accepts {@link IncrexFloatParams}.
 * @param <T> the concrete subtype, for fluent method chaining
 * @since 8.0
 */
public abstract class BaseIncrexParams<T extends BaseIncrexParams<T>> implements IParams {

  private boolean saturate = false;
  private boolean enx = false;
  private Keyword expiration;
  private Long expirationValue;

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }

  private T expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return self();
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
   * @return parameter object
   */
  public T exAt(long seconds) {
    return expiration(Keyword.EXAT, seconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
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

  /**
   * Saturate the result on out-of-bounds: the value is clamped to the violated bound (or the
   * implicit numeric-type limit when no explicit bound is set), and the second element of the reply
   * reflects the actual applied increment. Without {@code SATURATE}, an out-of-bounds operation is
   * silently rejected: the key value and TTL are unchanged and the reply is
   * {@code [current_value, 0]}.
   * @return parameter object
   */
  public T saturate() {
    this.saturate = true;
    return self();
  }

  /**
   * Apply the expiry only if the key does not already have one ({@code Expiry only if Not eXists}).
   * If the key already has a TTL, the existing TTL is preserved.
   * @return parameter object
   */
  public T enx() {
    this.enx = true;
    return self();
  }

  /**
   * Wire-format emission. Order: bounds &rarr; {@code SATURATE} &rarr; expiry &rarr; {@code ENX}.
   */
  @Override
  public void addParams(CommandArguments args) {

    if (saturate) {
      args.add(Keyword.SATURATE);
    }

    if (expiration != null) {
      args.add(expiration);
      if (expirationValue != null) {
        args.add(expirationValue);
      }
    }

    if (enx) {
      args.add(Keyword.ENX);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BaseIncrexParams<?> that = (BaseIncrexParams<?>) o;
    return saturate == that.saturate && enx == that.enx && expiration == that.expiration
        && Objects.equals(expirationValue, that.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(saturate, enx, expiration, expirationValue);
  }
}
