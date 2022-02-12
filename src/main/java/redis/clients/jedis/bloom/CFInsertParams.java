package redis.clients.jedis.bloom;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.CAPACITY;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.NOCREATE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

// [CAPACITY {capacity}] [NOCREATE]
public class CFInsertParams implements IParams {

  private Long capacity;
  private boolean noCreate = false;

  public static CFInsertParams insertParams() {
    return new CFInsertParams();
  }

  public CFInsertParams capacity(long capacity) {
    this.capacity = capacity;
    return this;
  }

  public CFInsertParams noCreate() {
    this.noCreate = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (capacity != null) {
      args.add(CAPACITY).add(toByteArray(capacity));
    }
    if (noCreate) {
      args.add(NOCREATE);
    }
  }
}
