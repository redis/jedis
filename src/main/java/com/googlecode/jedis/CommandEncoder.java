package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.valueOf;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.googlecode.jedis.Protocol.Command;

@Sharable
class CommandEncoder extends OneToOneEncoder {

    private static final byte[] CRNL = { '\r', '\n' };

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel,
	    Object msg) throws Exception {
	if (!(msg instanceof Pair<?, ?>)) {
	    return msg;
	}

	@SuppressWarnings("unchecked")
	Pair<Command, byte[][]> pair = (Pair<Command, byte[][]>) msg;

	Command cmd = pair.getFirst();
	byte[][] args = pair.getSecond();

	ChannelBuffer buffer = dynamicBuffer();

	buffer.writeByte('*');
	buffer.writeBytes(valueOf(1 + args.length).getBytes(UTF_8));
	buffer.writeBytes(CRNL);

	buffer.writeByte('$');
	buffer.writeBytes(valueOf(cmd.raw.length).getBytes(UTF_8));
	buffer.writeBytes(CRNL);

	buffer.writeBytes(cmd.raw);
	buffer.writeBytes(CRNL);

	for (byte[] arg : args) {
	    buffer.writeByte('$');
	    buffer.writeBytes(valueOf(arg.length).getBytes(UTF_8));
	    buffer.writeBytes(CRNL);

	    buffer.writeBytes(arg);
	    buffer.writeBytes(CRNL);
	}

	return buffer;
    }

}
