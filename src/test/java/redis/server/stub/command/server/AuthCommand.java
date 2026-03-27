package redis.server.stub.command.server;

import redis.clients.jedis.CommandArguments;
import redis.server.stub.RespResponse;
import redis.server.stub.command.RedisCommand;
import redis.server.stub.command.CommandContext;

/**
 * AUTH [username] password
 * 
 * Authenticates the current connection.
 * 
 * Redis 6+ supports ACL with username and password.
 * Redis <6 supports only password.
 * 
 * For MVP: We accept any authentication without validation.
 * 
 * Return value:
 * - Simple string reply: OK if authentication successful
 */
public class AuthCommand implements RedisCommand {
    
    @Override
    public String execute(CommandArguments args, CommandContext ctx) {
        int argCount = args.size() - 1;

        if (argCount < 1) {
            return RespResponse.error("ERR wrong number of arguments for 'auth' command");
        }

        ctx.getClient().setAuthenticated(true);

        return RespResponse.ok();
    }
    
    @Override
    public String getName() {
        return "AUTH";
    }
}

