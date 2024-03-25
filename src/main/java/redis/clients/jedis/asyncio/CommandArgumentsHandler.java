package redis.clients.jedis.asyncio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;

public class CommandArgumentsHandler extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buf) throws Exception {
        try {
            buf.markWriterIndex();

            if (msg instanceof CommandObject) {
                write(buf, ((CommandObject<?>) msg).getArguments());
            } else {
                throw new UnsupportedMessageTypeException(msg.getClass());
            }

        } catch (RuntimeException e) {
            buf.resetWriterIndex();

            // TODO: what to do ??
        }
    }

    private void write(ByteBuf buf, CommandArguments args) throws Exception {

        buf.writeByte('*');
        buf.writeBytes(toByteArray(args.size()));
        buf.writeByte('\r');
        buf.writeByte('\n');

        args.forEach(arg -> {
            byte[] raw = arg.getRaw();
            buf.writeByte('$');
            buf.writeBytes(toByteArray(raw.length));
            buf.writeByte('\r');
            buf.writeByte('\n');
            buf.writeBytes(raw);
            buf.writeByte('\r');
            buf.writeByte('\n');
        });
    }

    private byte[] toByteArray(int i) {
        return Integer.toString(i).getBytes();
    }
}
