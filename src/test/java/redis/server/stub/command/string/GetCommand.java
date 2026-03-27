package redis.server.stub.command.string;

import redis.clients.jedis.CommandArguments;
import redis.server.stub.RespResponse;
import redis.server.stub.command.RedisCommand;
import redis.server.stub.command.CommandContext;
import redis.server.stub.RedisKey;
import redis.server.stub.StoredValue;

/**
 * GET key
 * 
 * Get the value of key. If the key does not exist, null is returned.
 * An error is returned if the value stored at key is not a string.
 * 
 * Return value:
 * - Bulk string reply: the value of key
 * - Null bulk string reply: if key does not exist
 */
public class GetCommand implements RedisCommand {
    
    @Override
    public String execute(CommandArguments args, CommandContext ctx) {
        // Validate arguments
        int argCount = args.size() - 1;
        if (argCount < 1) {
            return RespResponse.error("ERR wrong number of arguments for 'get' command");
        }
        byte[] keyBytes = args.get(1).getRaw();
        RedisKey key = RedisKey.of(keyBytes);
        int db = ctx.getClient().getDatabase();

        // Get value from store (primitive operation)
        StoredValue value = ctx.getDataStore().get(db, key);

        if (value == null) {
            // Key doesn't exist or expired
            return RespResponse.nullBulkString();
        }

        // Type checking - GET only works on strings
        if (!value.isString()) {
            return RespResponse.error("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        // Return value as bulk string
        return RespResponse.bulkString(value.getBytes());
    }

    @Override
    public String getName() {
        return "GET";
    }
}

