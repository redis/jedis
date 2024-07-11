package redis.clients.jedis.asyncio;

import java.util.concurrent.CompletableFuture;
import redis.clients.jedis.asyncio.replies.CommandReply;

public class CommandObject<T> extends CompletableFuture<T> {

    private final CommandArguments arguments;
    private final CommandReply<T> reply;

    public CommandObject(CommandArguments arguments, CommandReply<T> reply) {
        this.arguments = arguments;
        this.reply = reply;
    }

    public CommandArguments getArguments() {
        return arguments;
    }

    public CommandReply<T> getReply() {
        return reply;
    }
}
