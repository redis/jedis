package com.googlecode.jedis;

import static com.googlecode.jedis.Protocol.DEFAULT_CHARSET;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.googlecode.jedis.Protocol.Command;

class Connection {

    private static final Logger log = LoggerFactory.getLogger(Connection.class);

    private Socket socket;
    private final Protocol protocol = new Protocol();
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private boolean pipelineMode = false;
    private int pipelinedCommands = 0;

    JedisConfig config;

    protected Connection() {
	config = new JedisConfig();
    }

    protected void connect() throws UnknownHostException, IOException {
	if (!isConnected()) {
	    socket = new Socket(config.getHost(), config.getPort());
	    socket.setSoTimeout(config.getTimeout());
	    outputStream = new RedisOutputStream(socket.getOutputStream());
	    inputStream = new RedisInputStream(socket.getInputStream());
	}
    }

    protected void disconnect() {
	if (isConnected()) {
	    try {
		inputStream.close();
		outputStream.close();
		if (!socket.isClosed()) {
		    socket.close();
		}
	    } catch (IOException ex) {
		throw new JedisException(ex);
	    }
	}
    }

    protected List<byte[]> getAll() {
	List<byte[]> all = Lists.newArrayList();

	while (pipelinedCommands > 0) {
	    all.add((byte[]) protocol.read(inputStream));
	    pipelinedCommands--;
	}

	if (log.isDebugEnabled()) {
	    Collection<String> allAsString = Collections2.transform(all,
		    new Function<byte[], String>() {
			@Override
			public String apply(byte[] input) {
			    return (input != null) ? new String(input,
				    DEFAULT_CHARSET) : "nil";
			}
		    });
	    log.debug("-> {}", Joiner.on(" ").join(allAsString));
	}

	return all;
    }

    protected byte[] getBinaryBulkReply() {
	byte[] reply = null;
	if (!pipelineMode) {
	    pipelinedCommands--;
	    reply = (byte[]) protocol.read(inputStream);
	    if (log.isDebugEnabled()) {
		String str = (reply != null) ? new String(reply,
			DEFAULT_CHARSET) : "nil";
		log.debug("redis -> {}", str);
	    }
	}
	return reply;
    }

    @SuppressWarnings("unchecked")
    protected List<byte[]> getBinaryMultiBulkReply() {
	List<byte[]> reply = null;
	if (!pipelineMode) {
	    reply = Collections.emptyList();
	    Object input = protocol.read(inputStream);
	    pipelinedCommands--;
	    if (input != null) {
		reply = (List<byte[]>) input;
	    }
	}
	return reply;
    }

    protected Boolean getBooleanIntegerReply() {
	Boolean reply = false;
	if (!pipelineMode) {
	    reply = getIntegerReply().equals(1L);
	}
	return reply;
    }

    protected Boolean getBooleanStatusCodeReply() {
	Boolean reply = false;
	if (!pipelineMode) {
	    reply = getStatusCodeReply().equals("OK");
	}
	return reply;
    }

    protected String getBulkReply() {
	String reply = null;
	if (!pipelineMode) {
	    final byte[] breply = getBinaryBulkReply();

	    if (breply != null) {
		reply = new String(breply, DEFAULT_CHARSET);
	    }

	    log.debug("redis -> {}", reply);
	}
	return reply;
    }

    protected JedisConfig getConfig() {
	return config;
    }

    protected Long getIntegerReply() {
	Long reply = null;
	if (!pipelineMode) {
	    reply = (Long) protocol.read(inputStream);

	    pipelinedCommands--;
	    log.debug("redis -> {}", reply);
	}
	return reply;
    }

    protected List<String> getMultiBulkReply() {
	ArrayList<String> result = null;
	if (!pipelineMode) {
	    result = Lists.newArrayList();

	    for (byte[] ba : getBinaryMultiBulkReply()) {
		result.add((ba != null) ? new String(ba,
			Protocol.DEFAULT_CHARSET) : (String) null);
	    }

	    if (log.isDebugEnabled()) {
		log.debug("redis -> {}",
			Joiner.on(" ").useForNull("nil").join(result));
	    }
	}
	return result;
    }

    protected List<? extends Object> getObjectMultiBulkReply() {
	List<? extends Object> reply = null;
	if (!pipelineMode) {
	    reply = Collections.emptyList();

	    Object input = protocol.read(inputStream);

	    pipelinedCommands--;

	    if (input != null) {
		reply = (List<?>) input;
	    }
	}
	return reply;
    }

    protected Object getOne() {
	Object reply = null;
	if (!pipelineMode) {
	    pipelinedCommands--;
	    reply = protocol.read(inputStream);
	}
	return reply;
    }

    protected String getStatusCodeReply() {
	String reply = null;
	if (!pipelineMode) {

	    final byte[] input = (byte[]) protocol.read(inputStream);

	    pipelinedCommands--;

	    if (input != null) {
		reply = new String(input, DEFAULT_CHARSET);
	    }

	    log.debug("redis -> {}", reply);
	}
	return reply;
    }

    protected boolean isConnected() {
	return socket != null && socket.isBound() && !socket.isClosed()
		&& socket.isConnected() && !socket.isInputShutdown()
		&& !socket.isOutputShutdown();
    }

    protected boolean isPipelineMode() {
	return pipelineMode;
    }

    protected void rollbackTimeout() {
	try {
	    socket.setSoTimeout(config.getTimeout());
	} catch (SocketException ex) {
	    throw new JedisException(ex);
	}
    }

    private void safeConnect() {
	try {
	    connect();
	} catch (UnknownHostException e) {
	    log.error("Could not connect to redis-server {}", config.toString());
	    throw new JedisException("Could not connect to redis-server", e);
	} catch (IOException e) {
	    log.error("Could not connect to redis-server {}", config.toString());
	    throw new JedisException("Could not connect to redis-server", e);
	}
    }

    protected Connection sendCommand(Command cmd) {
	return sendCommand(cmd, new byte[0][0]);
    }

    // see: Effective Java 2: Item 42, performance
    protected Connection sendCommand(final Command cmd, final byte[]... args) {

	safeConnect();

	protocol.sendCommand(outputStream, cmd, args);
	pipelinedCommands++;

	// test if log string is needed, construction is expensive
	if (log.isDebugEnabled()) {
	    StringBuilder sb = new StringBuilder();
	    sb.append(cmd);
	    for (byte[] arg : args) {
		sb.append(" ");
		sb.append((arg != null) ? new String(arg, DEFAULT_CHARSET)
			: "nil");
	    }
	    log.debug("redis <- {}", sb.toString());
	}
	return this;
    }

    // TODO: Effective Java 2: Item 42, performance
    protected Connection sendCommand(final Command cmd, final String... args) {
	final byte[][] bargs = new byte[args.length][];
	for (int i = 0; i < args.length; i++) {
	    bargs[i] = args[i].getBytes(DEFAULT_CHARSET);
	}
	return sendCommand(cmd, bargs);
    }

    protected void setConfig(JedisConfig config) {
	// make defensive copy
	this.config = config.copy();
    }

    protected void setPipelineMode(boolean pipelineMode) {
	this.pipelineMode = pipelineMode;
    }

    protected void setTimeoutInfinite() {
	try {
	    socket.setSoTimeout(0);
	} catch (SocketException ex) {
	    log.error("Could not set timeout to infinite");
	    throw new JedisException(ex);
	}
    }
}