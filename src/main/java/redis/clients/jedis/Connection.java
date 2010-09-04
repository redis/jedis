package redis.clients.jedis;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Connection {
    private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private Protocol protocol = new Protocol();
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private int pipelinedCommands = 0;
    private int timeout = 2000;

    public int getTimeout() {
	return timeout;
    }

    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    public void setTimeoutInfinite() {
	try {
	    socket.setSoTimeout(0);
	} catch (SocketException ex) {
	    throw new JedisException(ex);
	}
    }

    public void rollbackTimeout() {
	try {
	    socket.setSoTimeout(timeout);
	} catch (SocketException ex) {
	    throw new JedisException(ex);
	}
    }

    public Connection(String host) {
	super();
	this.host = host;
    }

    protected Connection sendCommand(String name, String... args) {
	if (!isConnected()) {
	    throw new JedisException("Please connect Jedis before using it.");
	}
	protocol.sendCommand(outputStream, name, args);
	pipelinedCommands++;
	return this;
    }

    public Connection(String host, int port) {
	super();
	this.host = host;
	this.port = port;
    }

    public String getHost() {
	return host;
    }

    public void setHost(String host) {
	this.host = host;
    }

    public int getPort() {
	return port;
    }

    public void setPort(int port) {
	this.port = port;
    }

    public Connection() {
    }

    public void connect() throws UnknownHostException, IOException {
	if (!isConnected()) {
	    socket = new Socket(host, port);
	    socket.setSoTimeout(timeout);
	    outputStream = new DataOutputStream(socket.getOutputStream());
	    inputStream = new DataInputStream(new BufferedInputStream(socket
		    .getInputStream()));
	}
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
		throw new JedisException(ex);
	    }
	}
    }

    public boolean isConnected() {
	return socket != null && socket.isBound() && !socket.isClosed()
		&& socket.isConnected() && !socket.isInputShutdown()
		&& !socket.isOutputShutdown();
    }

    protected String getStatusCodeReply() {
	pipelinedCommands--;
	return (String) protocol.read(inputStream);
    }

    public String getBulkReply() {
	pipelinedCommands--;
	return (String) protocol.read(inputStream);
    }

    public int getIntegerReply() {
	pipelinedCommands--;
	return (Integer) protocol.read(inputStream);
    }

    @SuppressWarnings("unchecked")
    public List<String> getMultiBulkReply() {
	pipelinedCommands--;
	return (List<String>) protocol.read(inputStream);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getObjectMultiBulkReply() {
	pipelinedCommands--;
	return (List<Object>) protocol.read(inputStream);
    }

    public List<Object> getAll() {
	List<Object> all = new ArrayList<Object>();
	while (pipelinedCommands > 0) {
	    all.add(protocol.read(inputStream));
	    pipelinedCommands--;
	}
	return all;
    }
}