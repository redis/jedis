package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jedis.PairImpl.newPair;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.googlecode.jedis.Protocol.Command;

class NettyConnection implements Connection {

    private final CommandEncoder commandEncoder = new CommandEncoder();
    private final BlockingQueue<Pair<ResponseType, List<byte[]>>> queue = new ArrayBlockingQueue<Pair<ResponseType, List<byte[]>>>(
	    128);

    private ChannelFactory factory;
    private ClientBootstrap bootstrap;
    private ChannelFuture future;
    private JedisConfig jedisConfig;
    private boolean infiniteTimeout = false;

    @Override
    public byte[] bulkReply() {
	Pair<ResponseType, List<byte[]>> result = fromQueue();

	if ((result.getSecond()).size() == 1) {
	    return (result.getSecond()).get(0);
	}
	return null;
    }

    @Override
    public void connect() throws Throwable {

	factory = new NioClientSocketChannelFactory(
		Executors.newCachedThreadPool(),
		Executors.newCachedThreadPool());
	bootstrap = new ClientBootstrap(factory);

	bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
	    @Override
	    public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline p = Channels.pipeline();
		p.addLast("framer", new SingleLineFramer());
		p.addLast("decoder", new ResponseDecoder(queue));
		p.addLast("encoder", commandEncoder);
		return p;
	    }
	});

	// bootstrap.setOption("tcpNoDelay", true);
	// bootstrap.setOption("keepAlive", true);
	// bootstrap.setOption("connectTimeoutMillis",
	// jedisConfig.getTimeout());

	// Start the connection attempt.
	future = bootstrap.connect(new InetSocketAddress(jedisConfig.getHost(),
		jedisConfig.getPort()));
	// Wait until the connection attempt succeeds or fails.
	future.awaitUninterruptibly();
	if (!future.isSuccess()) {
	    bootstrap.releaseExternalResources();
	    throw future.getCause();
	}
    }

    @Override
    public void disconnect() {
	// disconnect the channel
	future.getChannel().disconnect().awaitUninterruptibly();
	// Wait until the connection is closed or the connection attempt fails.
	future.getChannel().getCloseFuture().awaitUninterruptibly();
	// Shut down thread pools to exit.
	factory.releaseExternalResources();
    }

    private Pair<ResponseType, List<byte[]>> fromQueue() {
	Pair<ResponseType, List<byte[]>> result = null;

	try {
	    if (infiniteTimeout) {
		result = queue.take();
	    } else {
		result = queue.poll(jedisConfig.getTimeout(),
			TimeUnit.MILLISECONDS);
	    }
	} catch (InterruptedException e) {
	    throw new JedisException(e.getLocalizedMessage());
	}

	// could not get element from queue
	if (result == null) {
	    throw new JedisException("connection timeout");
	}

	if (result.getFirst() == ResponseType.Error) {
	    throw new JedisException(new String(result.getSecond().get(0),
		    UTF_8));
	}
	return result;
    }

    @Override
    public JedisConfig getJedisConfig() {
	return jedisConfig;
    }

    @Override
    public Long integerReply() {
	Pair<ResponseType, List<byte[]>> result = fromQueue();

	if (result.getSecond().get(0) == null) {
	    return null;
	}
	return Long.valueOf(new String(result.getSecond().get(0), UTF_8));
    }

    @Override
    public Boolean integerReplyAsBoolean() {
	return integerReply() == 1L;
    }

    @Override
    public boolean isConnected() {
	return future != null && future.getChannel().isConnected();
    }

    @Override
    public List<byte[]> multiBulkReply() {
	return fromQueue().getSecond();
    }

    @Override
    public void rollbackTimeout() {
	infiniteTimeout = false;
    }

    @Override
    public void sendCommand(Command cmd, byte[]... args) {
	future.getChannel().write(newPair(cmd, args));
    }

    @Override
    public void setJedisConfig(JedisConfig jedisConfig) {
	this.jedisConfig = jedisConfig;
    }

    @Override
    public void setTimeoutInfinite() {
	infiniteTimeout = true;
    }

    @Override
    public byte[] statusCodeReply() {
	return fromQueue().getSecond().get(0);
    }

    @Override
    public Boolean statusCodeReplyAsBoolean() {
	return Arrays.equals(statusCodeReply(), Protocol.Keyword.OK.raw);
    }

}
