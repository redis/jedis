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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * 
 * @author Moritz Heuser
 * 
 */
class ResponseDecoder extends SimpleChannelUpstreamHandler {

    private static final byte[] NULL = { '-', '1' };

    private final BlockingQueue<Pair<ResponseType, List<byte[]>>> queue;

    private List<byte[]> multiBulkStore;
    private boolean multiBulkMode = false;
    private int multiBulkArgs;

    public ResponseDecoder(BlockingQueue<Pair<ResponseType, List<byte[]>>> queue) {
	this.queue = queue;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	    throws Exception {
	super.messageReceived(ctx, e);

	@SuppressWarnings("unchecked")
	Pair<ResponseType, byte[]> responseLine = (Pair<ResponseType, byte[]>) e
		.getMessage();

	ResponseType responseType = responseLine.getFirst();
	byte[] line = responseLine.getSecond();

	switch (responseType) {
	case MultiBulk:
	    multiBulkArgs = Integer.valueOf(new String(line, UTF_8));
	    if (multiBulkArgs <= 0) {
		// empty multibulk response, blpop and brpop are returning -1
		List<byte[]> response = ImmutableList.of();
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
		    List<byte[]> result = Lists.newArrayList(multiBulkStore);
		    multiBulkStore.clear();
		    multiBulkMode = false;
		    toQueue(newPair(ResponseType.MultiBulk, result));
		}
	    } else {
		// can't use guava's immutable list because of null values
		toQueue(newPair(responseType, Collections.singletonList(line)));
	    }
	    break;
	default:
	    toQueue(newPair(responseType, (List<byte[]>) ImmutableList.of(line)));
	}

    }

    private void toQueue(Pair<ResponseType, List<byte[]>> next) {
	try {
	    queue.offer(next, 1, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
}
