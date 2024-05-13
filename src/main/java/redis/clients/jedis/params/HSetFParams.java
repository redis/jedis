package redis.clients.jedis.params;

import java.util.Objects;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.ExpiryOption;

public class HSetFParams implements IParams {

  private boolean doNotCreate;
  private boolean doNotCreateFields;
  private boolean doNotOverwriteFields;

  private ExpiryOption condition;

  private Keyword expiration;
  private Long expirationValue;

  public HSetFParams() {
  }

  public static HSetFParams hsetfParams() {
    return new HSetFParams();
  }

  public HSetFParams doNotCreate() {
    this.doNotCreate = true;
    return this;
  }

  /**
   * Same as {@link HSetFParams#doNotCreate()}.
   * @return this object
   */
  public HSetFParams doNotCreateKey() {
    return doNotCreate();
  }

  public HSetFParams doNotCreateFields() {
    this.doNotCreateFields = true;
    this.doNotOverwriteFields = false;
    return this;
  }

  public HSetFParams doNotOverwriteFields() {
    this.doNotCreateFields = false;
    this.doNotOverwriteFields = true;
    return this;
  }

  public HSetFParams condition(ExpiryOption condition) {
    this.condition = condition;
    return this;
  }

  public HSetFParams nx() {
    return condition(ExpiryOption.NX);
  }

  public HSetFParams xx() {
    return condition(ExpiryOption.XX);
  }

  public HSetFParams gt() {
    return condition(ExpiryOption.GT);
  }

  public HSetFParams lt() {
    return condition(ExpiryOption.LT);
  }

  private HSetFParams expiration(Keyword type, Long value) {
    this.expiration = type;
    this.expirationValue = value;
    return this;
  }

  public HSetFParams ex(long seconds) {
    return expiration(Keyword.EX, seconds);
  }

  public HSetFParams px(long milliseconds) {
    return expiration(Keyword.PX, milliseconds);
  }

  public HSetFParams exAt(long seconds) {
    return expiration(Keyword.EXAT, seconds);
  }

  public HSetFParams pxAt(long milliseconds) {
    return expiration(Keyword.PXAT, milliseconds);
  }

  public HSetFParams keepTtl() {
    return expiration(Keyword.KEEPTTL, null);
  }

  @Override
  public void addParams(CommandArguments args) {
    if (doNotCreate) {
      args.add(Keyword.DC);
    }
    if (doNotCreateFields) {
      args.add(Keyword.DCF);
    } else if (doNotOverwriteFields) {
      args.add(Keyword.DOF);
    }

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
    HSetFParams setParams = (HSetFParams) o;
    return Objects.equals(condition, setParams.condition) && Objects.equals(expiration, setParams.expiration)
            && Objects.equals(expirationValue, setParams.expirationValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, expiration, expirationValue);
  }
}
