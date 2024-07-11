package redis.clients.jedis.asyncio.replies;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StatusReply extends CommandReply<String> {

    @Override
    public void parse(ByteBuffer bytes) {
        setReply(StandardCharsets.US_ASCII.decode(bytes).toString());
    }
}
