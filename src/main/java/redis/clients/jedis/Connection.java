package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class Connection {
    private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private boolean connected = false;
    private Protocol protocol = new Protocol();
    private OutputStream outputStream;
    private InputStream inputStream;

    public Connection(String host) {
	super();
	this.host = host;
    }

    protected Connection sendCommand(String name, String... args) {
	if (!isConnected()) {
	    throw new JedisException("Please connect Jedis before using it.");
	}
	protocol.sendCommand(outputStream, name, args);
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
	if (!connected) {
	    socket = new Socket(host, port);
	    connected = socket.isConnected();
	    outputStream = socket.getOutputStream();
	    inputStream = socket.getInputStream();
	}
    }

    public void disconnect() throws IOException {
	if (connected) {
	    inputStream.close();
	    outputStream.close();
	    if (!socket.isClosed()) {
		socket.close();
	    }
	    connected = false;
	}
    }

    public boolean isConnected() {
	return connected;
    }

    protected String getStatusCodeReply() {
	return protocol.getStatusCodeReply(inputStream);
    }

    public String getBulkReply() {
	return protocol.getBulkReply(inputStream);
    }

    public int getIntegerReply() {
	return protocol.getIntegerReply(inputStream);
    }

    @SuppressWarnings("unchecked")
    public List<String> getMultiBulkReply() {
	return (List<String>) (List<?>) protocol.getMultiBulkReply(inputStream);
    }

    public List<Object> getObjectMultiBulkReply() {
	return protocol.getMultiBulkReply(inputStream);
    }

}