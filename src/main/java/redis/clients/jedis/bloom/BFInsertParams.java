package redis.clients.jedis.bloom;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.CAPACITY;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.ERROR;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.EXPANSION;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.NOCREATE;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.NONSCALING;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

// [CAPACITY {cap}] [ERROR {error}] [EXPANSION {expansion}] [NOCREATE] [NONSCALING]
public class BFInsertParams implements IParams {

  private Long capacity;
  private Double errorRate;
  private Integer expansion;
  private boolean noCreate = false;
  private boolean nonScaling = false;

  public static BFInsertParams insertParams() {
    return new BFInsertParams();
  }

  public BFInsertParams capacity(long capacity) {
    this.capacity = capacity;
    return this;
  }

  public BFInsertParams error(double errorRate) {
    this.errorRate = errorRate;
    return this;
  }

  public BFInsertParams expansion(int expansion) {
    this.expansion = expansion;
    return this;
  }

  public BFInsertParams noCreate() {
    this.noCreate = true;
    return this;
  }

  public BFInsertParams nonScaling() {
    this.nonScaling = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (capacity != null) {
      args.add(CAPACITY).add(toByteArray(capacity));
    }
    if (errorRate != null) {
      args.add(ERROR).add(toByteArray(errorRate));
    }
    if (expansion != null) {
      args.add(EXPANSION).add(toByteArray(expansion));
    }
    if (noCreate) {
      args.add(NOCREATE);
    }
    if (nonScaling) {
      args.add(NONSCALING);
    }
  }
}
