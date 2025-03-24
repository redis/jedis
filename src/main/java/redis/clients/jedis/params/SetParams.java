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
