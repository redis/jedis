package redis.clients.jedis.bloom;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.EXPANSION;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.NONSCALING;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

public class BFReserveParams implements IParams {

  private Integer expansion;
  private boolean nonScaling = false;

  public static BFReserveParams reserveParams() {
    return new BFReserveParams();
  }

  public BFReserveParams expansion(int expansion) {
    this.expansion = expansion;
    return this;
  }

  public BFReserveParams nonScaling() {
    this.nonScaling = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (expansion != null) {
      args.add(EXPANSION).add(toByteArray(expansion));
    }
    if (nonScaling) {
      args.add(NONSCALING);
    }
  }
}
