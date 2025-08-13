package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class SetParams extends BaseSetExParams<SetParams> {

  private Keyword existance;

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

  /**
   * Set the specified expire time, in seconds.
   * @param remainingSeconds
   * @return SetParams
   */
  @Override
  public SetParams ex(long remainingSeconds) {
    return super.ex(remainingSeconds);
  }

  /**
   * Set the specified expire time, in milliseconds.
   * @param remainingMilliseconds
   * @return SetParams
   */
  @Override
  public SetParams px(long remainingMilliseconds) {
    return super.px(remainingMilliseconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in seconds.
   * @param timestampSeconds
   * @return SetParams
   */
  @Override
  public SetParams exAt(long timestampSeconds) {
    return super.exAt(timestampSeconds);
  }

  /**
   * Set the specified Unix time at which the key will expire, in milliseconds.
   * @param timestampMilliseconds
   * @return SetParams
   */
  @Override
  public SetParams pxAt(long timestampMilliseconds) {
    return super.pxAt(timestampMilliseconds);
  }

  /**
   * Retain the time to live associated with the key.
   *
   * @deprecated Since 6.1.0 use {@link #keepTtl()} instead.
   * @return SetParams
   */
  @Override
  public SetParams keepttl() {
    return keepTtl();
  }

  /**
   * Retain the time to live associated with the key.
   * @return SetParams
   */
  @Override
  public SetParams keepTtl() {
    return super.keepTtl();
  }

  @Override
  public void addParams(CommandArguments args) {
    if (existance != null) {
      args.add(existance);
    }

    super.addParams(args);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SetParams setParams = (SetParams) o;
    return Objects.equals(existance, setParams.existance) && super.equals((BaseSetExParams) o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(existance, super.hashCode());
  }
}
