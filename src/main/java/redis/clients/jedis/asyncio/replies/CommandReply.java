package redis.clients.jedis.asyncio.replies;

import java.nio.ByteBuffer;

public class CommandReply<T> {

    private T reply = null;

    public CommandReply() {
    }

    public T get() {
        return reply;
    }

    public void setReply(T reply) {
        this.reply = reply;
    }

    public void parse(ByteBuffer bytes) {
        // override
    }
}
