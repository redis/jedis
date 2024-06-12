package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

public class ZParams implements IParams {

  public enum Aggregate implements Rawable {

    SUM, MIN, MAX;

    private final byte[] raw;

    private Aggregate() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  private final List<Object> params = new ArrayList<>();

  public ZParams weights(final double... weights) {
    params.add(Keyword.WEIGHTS);
    for (final double weight : weights) {
      params.add(weight);
    }
    return this;
  }

  public ZParams aggregate(final Aggregate aggregate) {
    params.add(Keyword.AGGREGATE);
    params.add(aggregate);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addObjects(params);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ZParams zParams = (ZParams) o;
    return Objects.equals(params, zParams.params);
  }

  @Override
  public int hashCode() {
    return Objects.hash(params);
  }
}
