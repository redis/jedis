package redis.clients.jedis.asyncio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import redis.clients.jedis.exceptions.JedisConnectionException;

import static redis.clients.jedis.Protocol.*;

public class CommandResponseHandler extends ChannelDuplexHandler {

    protected final ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(8192 * 8);
//    protected volatile Deque<ChannelPromise> promiseList = new ArrayDeque<>(512);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
//        promiseList.add(promise);
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf input = (ByteBuf) msg;

        try {
            buffer.writeBytes(input);

            decode(ctx, buffer);

        } finally {
            input.release();
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) {
        int length, end;
        ByteBuffer bytes;

        byte type = buffer.readByte();
        System.out.println((char) type);

        switch (type) {
            // TODO: skip push
            //
            case PLUS_BYTE:
                bytes = readLine(buffer);
                break;
            case DOLLAR_BYTE:
                end = findLineEnd(buffer);
                length = (int) readLong(buffer, buffer.readerIndex(), end);
                bytes = readBytes(buffer, length);
                break;
            default:
                throw new JedisConnectionException("Unknown reply: " + (char) type);
        }
//        promiseList.remove();
        System.out.println(StandardCharsets.US_ASCII.decode(bytes).toString());
    }

    private int findLineEnd(ByteBuf buffer) {
        int start = buffer.readerIndex();
        int index = buffer.indexOf(start, buffer.writerIndex(), (byte) '\n');
        return (index > 0 && buffer.getByte(index - 1) == '\r') ? index : -1;
    }

    private long readLong(ByteBuf buffer, int start, int end) {
        long value = 0;

        boolean negative = buffer.getByte(start) == '-';
        int offset = negative ? start + 1 : start;
        while (offset < end - 1) {
            int digit = buffer.getByte(offset++) - '0';
            value = value * 10 - digit;
        }
        if (!negative) {
            value = -value;
        }
        buffer.readerIndex(end + 1);

        return value;
    }

    private ByteBuffer readLine(ByteBuf buffer) {
        ByteBuffer bytes = null;
        int end = findLineEnd(buffer);
        if (end > -1) {
            int start = buffer.readerIndex();
            bytes = buffer.nioBuffer(start, end - start - 1);
            buffer.readerIndex(end + 1);
            buffer.markReaderIndex();
        }
        return bytes;
    }

    private ByteBuffer readBytes(ByteBuf buffer, int count) {
        ByteBuffer bytes = null;
        if (buffer.readableBytes() >= count + 2) {
            bytes = buffer.nioBuffer(buffer.readerIndex(), count);
            buffer.readerIndex(buffer.readerIndex() + count + 2);
        }
        return bytes;
    }

}
