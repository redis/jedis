package redis.server.stub.command.server;

import redis.clients.jedis.CommandArguments;
import redis.server.stub.RespResponse;
import redis.server.stub.command.RedisCommand;
import redis.server.stub.command.CommandContext;

/**
 * PING [message]
 * 
 * Returns PONG if no message is provided.
 * Returns the message if provided (echo).
 * 
 * Used for:
 * - Testing connection
 * - Measuring latency
 * - Keepalive
 */
public class PingCommand implements RedisCommand {
    
    @Override
    public String execute(CommandArguments args, CommandContext ctx) {
        // args.size() includes the command itself, so size-1 is the argument count
        int argCount = args.size() - 1;

        if (argCount == 0) {
            // PING -> return simple string "PONG"
            return RespResponse.pong();
        } else {
            // PING message -> echo message as bulk string
            // args.get(1) is the first argument (args.get(0) is the command)
            byte[] message = args.get(1).getRaw();
            return RespResponse.bulkString(message);
        }
    }

    @Override
    public String getName() {
        return "PING";
    }
}

