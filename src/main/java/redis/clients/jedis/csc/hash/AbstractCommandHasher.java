package redis.clients.jedis.csc.hash;

import redis.clients.jedis.Builder;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.args.Rawable;

public abstract class AbstractCommandHasher implements CommandLongHasher {
  
  @Override
  public final long hash(CommandObject command) {
    long[] nums = new long[command.getArguments().size() + 1];
    int idx = 0;
    for (Rawable raw : command.getArguments()) {
      nums[idx++] = hashRawable(raw);
    }
    nums[idx] = hashBuilder(command.getBuilder());
    return hashLongs(nums);
  }

  protected abstract long hashLongs(long[] longs);

  protected abstract long hashRawable(Rawable raw);

  protected long hashBuilder(Builder builder) {
    return builder.hashCode();
  }
}
