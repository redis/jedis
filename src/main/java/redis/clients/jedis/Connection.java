package redis.clients.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

public class Connection implements Closeable {
    private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private int pipelinedCommands = 0;
    private int timeout = Protocol.DEFAULT_TIMEOUT;

    public Socket getSocket() {
	return socket;
    }

    public int getTimeout() {
	return timeout;
    }

    public void setTimeout(final int timeout) {
	this.timeout = timeout;
    }

    public void setTimeoutInfinite() {
	try {
	    if (!isConnected()) {
		connect();
	    }
	    socket.setKeepAlive(true);
	    socket.setSoTimeout(0);
	} catch (SocketException ex) {
	    throw new JedisException(ex);
	}
    }

    public void rollbackTimeout() {
	try {
	    socket.setSoTimeout(timeout);
	    socket.setKeepAlive(false);
	} catch (SocketException ex) {
	    throw new JedisException(ex);
	}
    }

    public Connection(final String host) {
	super();
	this.host = host;
    }

    protected void flush() {
	try {
	    outputStream.flush();
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}
    }

    protected Connection sendCommand(final Command cmd, final String... args) {
	final byte[][] bargs = new byte[args.length][];
	for (int i = 0; i < args.length; i++) {
	    bargs[i] = SafeEncoder.encode(args[i]);
	}
	return sendCommand(cmd, bargs);
    }

    protected Connection sendCommand(final Command cmd, final byte[]... args) {
	connect();
	Protocol.sendCommand(outputStream, cmd, args);
	pipelinedCommands++;
	return this;
    }

    protected Connection sendCommand(final Command cmd) {
	connect();
	Protocol.sendCommand(outputStream, cmd, new byte[0][]);
	pipelinedCommands++;
	return this;
    }

    public Connection(final String host, final int port) {
	super();
	this.host = host;
	this.port = port;
    }

    public String getHost() {
	return host;
    }

    public void setHost(final String host) {
	this.host = host;
    }

    public int getPort() {
	return port;
    }

    public void setPort(final int port) {
	this.port = port;
    }

    public Connection() {

    }

    public void connect() {
	if (!isConnected()) {
	    try {
		socket = new Socket();
		// ->@wjw_add
		socket.setReuseAddress(true);
		socket.setKeepAlive(true); // Will monitor the TCP connection is
					   // valid
		socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to
					    // ensure timely delivery of data
		socket.setSoLinger(true, 0); // Control calls close () method,
					     // the underlying socket is closed
					     // immediately
		// <-@wjw_add

		socket.connect(new InetSocketAddress(host, port), timeout);
		socket.setSoTimeout(timeout);
		outputStream = new RedisOutputStream(socket.getOutputStream());
		inputStream = new RedisInputStream(socket.getInputStream());
	    } catch (IOException ex) {
		throw new JedisConnectionException(ex);
	    }
	}
    }

    @Override
    public void close() {
	disconnect();
   }

    public void disconnect() {
	if (isConnected()) {
	    try {
		inputStream.close();
		outputStream.close();
		if (!socket.isClosed()) {
		    socket.close();
		}
	    } catch (IOException ex) {
		throw new JedisConnectionException(ex);
	    }
	}
    }

    public boolean isConnected() {
	return socket != null && socket.isBound() && !socket.isClosed()
		&& socket.isConnected() && !socket.isInputShutdown()
		&& !socket.isOutputShutdown();
    }

    protected String getStatusCodeReply() {
	flush();
	pipelinedCommands--;
	final byte[] resp = (byte[]) Protocol.read(inputStream);
	if (null == resp) {
	    return null;
	} else {
	    return SafeEncoder.encode(resp);
	}
    }

    public String getBulkReply() {
	final byte[] result = getBinaryBulkReply();
	if (null != result) {
	    return SafeEncoder.encode(result);
	} else {
	    return null;
	}
    }

    public byte[] getBinaryBulkReply() {
	flush();
	pipelinedCommands--;
	return (byte[]) Protocol.read(inputStream);
    }

    public Long getIntegerReply() {
	flush();
	pipelinedCommands--;
	return (Long) Protocol.read(inputStream);
    }

    public List<String> getMultiBulkReply() {
	return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
    }

    @SuppressWarnings("unchecked")
    public List<byte[]> getBinaryMultiBulkReply() {
	flush();
	pipelinedCommands--;
	return (List<byte[]>) Protocol.read(inputStream);
    }

    public void resetPipelinedCount() {
        pipelinedCommands = 0;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getRawObjectMultiBulkReply() {
        return (List<Object>) Protocol.read(inputStream);
    }

    public List<Object> getObjectMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return getRawObjectMultiBulkReply();
    }

    @SuppressWarnings("unchecked")
    public List<Long> getIntegerMultiBulkReply() {
	flush();
	pipelinedCommands--;
	return (List<Long>) Protocol.read(inputStream);
    }

    public List<Object> getAll() {
	return getAll(0);
    }

    public List<Object> getAll(int except) {
	List<Object> all = new ArrayList<Object>();
	flush();
	while (pipelinedCommands > except) {
	    try {
		all.add(Protocol.read(inputStream));
	    } catch (JedisDataException e) {
		all.add(e);
	    }
	    pipelinedCommands--;
	}
	return all;
    }

    public Object getOne() {
	flush();
	pipelinedCommands--;
	return Protocol.read(inputStream);
    }
}
