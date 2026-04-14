package redis.server.stub.command.string;

import redis.clients.jedis.CommandArguments;
import redis.server.stub.RedisDataStore;
import redis.server.stub.RedisKey;
import redis.server.stub.RespResponse;
import redis.server.stub.StoredValue;
import redis.server.stub.CommandContext;
import redis.server.stub.RedisCommand;

/**
 * SET key value Set key to hold the string value. If key already holds a value, it is overwritten.
 * Phase 1 MVP: Simple SET without options (no EX, PX, NX, XX) Return value: - Simple string reply:
 * OK
 */
public class SetCommand implements RedisCommand {

  @Override
  public String execute(CommandArguments args, CommandContext ctx) {
    // Validate arguments: SET key value (Phase 1: no options)
    int argCount = args.size() - 1;
    if (argCount < 2) {
      return RespResponse.error("ERR wrong number of arguments for 'set' command");
    }

    // Binary-safe key and value handling
    RedisKey key = RedisKey.of(args.get(1).getRaw());
    byte[] value = args.get(2).getRaw();

    // Phase 1: Simple SET - just store the value (no EX, PX, NX, XX)
    RedisDataStore store = ctx.getDataStore();
    int db = ctx.getClient().getDatabase();
    store.set(db, key, StoredValue.bytes(value));

    return RespResponse.ok();
  }

  @Override
  public String getName() {
    return "SET";
  }
}
