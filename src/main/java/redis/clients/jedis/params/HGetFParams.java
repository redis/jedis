package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;
import redis.clients.jedis.args.ExpiryOption;

public class HGetFParams implements IParams {

  private ExpiryOption condition;

  private Keyword expiration;
  private Long expirationValue;

  public HGetFParams() {
  }

  public static HGetFParams hgetfParams() {
    return new HGetFParams();
  }

  public HGetFParams condition(ExpiryOption condition) {
    this.condition = condition;
    return this;
  }

  public HGetFParams nx() {
    return condition(ExpiryOption.NX);
  }

  public HGetFParams xx() {
    return condition(ExpiryOption.XX);
  }

  public HGetFParams gt() {
    return condition(ExpiryOption.GT);
  }

  public HGetFParams lt() {
    return condition(ExpiryOption.LT);
  }

  private HGetFParams expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return this;
  }

  public HGetFParams ex(long seconds) {
    return expiration(Keyword.EX, seconds);
  }

  public HGetFParams px(long milliseconds) {
    return expiration(Keyword.PX, milliseconds);
  }

  public HGetFParams exAt(long seconds) {
    return expiration(Keyword.EXAT, seconds);
  }

  public HGetFParams pxAt(long milliseconds) {
    return expiration(Keyword.PXAT, milliseconds);
  }

  public HGetFParams persist() {
    return expiration(Keyword.PERSIST, null);
  }

  @Override
  public void addParams(CommandArguments args) {
    if (condition != null) {
      args.add(condition);
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
    HGetFParams that = (HGetFParams) o;
    return expiration == that.expiration && Objects.equals(expirationValue, that.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expiration, expirationValue);
  }
}
