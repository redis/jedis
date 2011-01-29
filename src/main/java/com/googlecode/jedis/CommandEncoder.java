package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.googlecode.jedis.Protocol.Command;

@Sharable
final class CommandEncoder extends OneToOneEncoder {

    private static final byte[] CRNL = { '\r', '\n' };

    @Override
    protected Object encode(final ChannelHandlerContext ctx,
	    final Channel channel, final Object msg) throws Exception {
	if (!(msg instanceof List<?>)) {
	    return msg;
	}

	@SuppressWarnings("unchecked")
	final List<Pair<Command, byte[][]>> commands = (List<Pair<Command, byte[][]>>) msg;

	// TODO: benchmark how much this affect speed issues
	final ChannelBuffer buffer = dynamicBuffer(commands.size() * 256);

	for (final Pair<Command, byte[][]> command : commands) {

	    final Command cmdName = command.getFirst();
	    final byte[][] args = command.getSecond();

	    buffer.writeByte('*');
	    buffer.writeBytes(String.valueOf(1 + args.length).getBytes(UTF_8));
	    buffer.writeBytes(CRNL);

	    buffer.writeByte('$');
	    buffer.writeBytes(cmdName.lenght);
	    buffer.writeBytes(CRNL);

	    buffer.writeBytes(cmdName.raw);
	    buffer.writeBytes(CRNL);

	    for (final byte[] arg : args) {
		buffer.writeByte('$');
		buffer.writeBytes(String.valueOf(arg.length).getBytes(UTF_8));
		buffer.writeBytes(CRNL);

		buffer.writeBytes(arg);
		buffer.writeBytes(CRNL);
	    }
	}

	return buffer;
    }

}
