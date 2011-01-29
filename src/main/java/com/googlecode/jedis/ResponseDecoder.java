package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jedis.PairImpl.newPair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Decodes single lines to a proper return type.
 * 
 * TODO: pipelined mode!
 * 
 * @author Moritz Heuser
 * 
 */
final class ResponseDecoder extends SimpleChannelUpstreamHandler {

    private static Logger log = LoggerFactory.getLogger("org.googlecode.jedis");

    private static final byte[] NULL = { '-', '1' };

    private int multiBulkArgs;

    private boolean multiBulkMode = false;
    private List<byte[]> multiBulkStore;
    private final BlockingQueue<Pair<ResponseType, List<byte[]>>> queue;

    public ResponseDecoder(
	    final BlockingQueue<Pair<ResponseType, List<byte[]>>> queue) {
	this.queue = queue;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
	    final MessageEvent e) throws Exception {
	super.messageReceived(ctx, e);

	@SuppressWarnings("unchecked")
	final Pair<ResponseType, byte[]> responseLine = (Pair<ResponseType, byte[]>) e
		.getMessage();

	final ResponseType responseType = responseLine.getFirst();
	byte[] line = responseLine.getSecond();

	switch (responseType) {
	case MultiBulk:
	    multiBulkArgs = Integer.valueOf(new String(line, UTF_8));
	    if (multiBulkArgs <= 0) {
		// empty multibulk response, blpop and brpop are returning -1
		final List<byte[]> response = ImmutableList.of();
		toQueue(newPair(ResponseType.MultiBulk, response));
	    } else {
		multiBulkMode = true;
		multiBulkStore = Lists.newArrayListWithCapacity(multiBulkArgs);
	    }
	    break;
	case Bulk:
	    if (Arrays.equals(line, NULL)) {
		line = null;
	    }
	    if (multiBulkMode) {
		multiBulkArgs--;
		multiBulkStore.add(line);
		if (multiBulkArgs == 0) {
		    // can't use guava's immutable list because of null values
		    final List<byte[]> result = Lists
			    .newArrayList(multiBulkStore);
		    multiBulkStore.clear();
		    multiBulkMode = false;
		    toQueue(newPair(ResponseType.MultiBulk, result));
		}
	    } else {
		// can't use guava's immutable list because of possible null
		// values
		toQueue(newPair(responseType, Collections.singletonList(line)));
	    }
	    break;
	default:
	    toQueue(newPair(responseType, (List<byte[]>) ImmutableList.of(line)));
	}

    }

    private void toQueue(final Pair<ResponseType, List<byte[]>> next) {
	try {
	    queue.offer(next, 1, TimeUnit.MINUTES);
	} catch (final InterruptedException e) {
	    log.error("Could not put a Response to the queue!", e);
	}
    }
}
