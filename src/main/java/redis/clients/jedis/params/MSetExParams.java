package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class MSetExParams extends BaseSetExParams<MSetExParams> {

  private Keyword existance;

  public static MSetExParams setParams() {
    return new MSetExParams();
  }

  /**
   * Only set the key if it does not already exist.
   * @return {@code this}
   */
  public MSetExParams nx() {
    this.existance = Keyword.NX;
    return this;
  }

  /**
   * Only set the key if it already exist.
   * @return {@code this}
   */
  public MSetExParams xx() {
    this.existance = Keyword.XX;
    return this;
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
    MSetExParams setParams = (MSetExParams) o;
    return Objects.equals(existance, setParams.existance) && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(existance, super.hashCode());
  }
}
