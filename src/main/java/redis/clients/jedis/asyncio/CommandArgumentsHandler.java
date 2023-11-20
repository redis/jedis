package redis.clients.jedis.asyncio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import java.util.Arrays;
import java.util.List;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

public class CommandArgumentsHandler extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buf) throws Exception {
        try {
            buf.markWriterIndex();

            if (msg instanceof List) {
                write(buf, (List<String>) msg);
            } else if (msg instanceof String) {
                write(buf, Arrays.asList(((String) msg).split("\\s+")));
            } else {
                throw new UnsupportedMessageTypeException(msg.getClass());
            }

        } catch (RuntimeException e) {
            buf.resetWriterIndex();

            // TODO: what to do ??
        }
    }

    private void write(ByteBuf buf, List<String> msg) throws Exception {

        buf.writeByte('*');
        buf.writeBytes(Protocol.toByteArray(msg.size()));
        buf.writeByte('\r');
        buf.writeByte('\n');

        msg.forEach(arg -> {
            buf.writeByte('$');
            buf.writeBytes(Protocol.toByteArray(arg.length()));
            buf.writeByte('\r');
            buf.writeByte('\n');
            buf.writeBytes(SafeEncoder.encode(arg));
            buf.writeByte('\r');
            buf.writeByte('\n');
        });
    }
}
