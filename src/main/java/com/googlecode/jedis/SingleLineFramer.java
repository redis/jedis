package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jedis.PairImpl.newPair;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * Frames a single response line.
 * 
 * @author Moritz Heuser
 * 
 */
class SingleLineFramer extends FrameDecoder {

    private static ChannelBufferIndexFinder CrAndLn = new ChannelBufferIndexFinder() {
	@Override
	public boolean find(ChannelBuffer buffer, int guessedIndex) {
	    return (guessedIndex + 1 < buffer.writerIndex())
		    && buffer.getByte(guessedIndex) == '\r'
		    && buffer.getByte(guessedIndex + 1) == '\n';
	}
    };

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel,
	    ChannelBuffer buffer) throws Exception {

	byte[] line;
	ResponseType responseType = ResponseType.Unknown;

	// get first byte and mark reader beginn
	if (!buffer.readable()) {
	    return null;
	}
	buffer.markReaderIndex();
	responseType = ResponseType.get(buffer.readByte());

	switch (responseType) {
	case Bulk:
	    line = decodeBulk(buffer);
	    break;
	default:
	    line = decodeSingleLine(buffer);
	}

	if (line == null) {
	    // get more data
	    buffer.resetReaderIndex();
	    return null;
	}

	// line complete
	return newPair(responseType, line);
    }

    private byte[] decodeBulk(ChannelBuffer buffer) {

	// get bulk lenght
	byte[] bulkLenghtStr = decodeSingleLine(buffer);
	if (bulkLenghtStr == null) {
	    return null;
	}

	int bulkLenght = Integer.valueOf(new String(bulkLenghtStr, UTF_8));

	// test for null bulk reply
	if (bulkLenght == -1) {
	    return bulkLenghtStr;
	}

	// Enough data in the buffer?
	if ((buffer.readerIndex() + bulkLenght + 2) > buffer.writerIndex()) {
	    return null;
	}

	byte[] line = new byte[bulkLenght];
	buffer.readBytes(line);
	buffer.skipBytes(2); // CRLF
	return line;
    }

    private byte[] decodeSingleLine(ChannelBuffer buffer) {
	byte[] line = null;

	int lineLenght = buffer.bytesBefore(CrAndLn);
	if (lineLenght == -1) {
	    return null;
	}

	line = new byte[lineLenght];
	buffer.readBytes(line);
	buffer.skipBytes(2); // CRLF
	return line;
    }

}
