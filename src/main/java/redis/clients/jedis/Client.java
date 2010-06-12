package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class Client {
    private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private boolean connected = false;
    private Protocol protocol = new Protocol();
    private OutputStream outputStream;
    private InputStream inputStream;

    public Client(String host) {
	super();
	this.host = host;
    }

    protected Client sendCommand(String name, String... args) {
	protocol.sendCommand(outputStream, name, args);
	return this;
    }

    public Client(String host, int port) {
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

    public Client() {
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

    protected String getStatusCodeReply() throws JedisException {
	return protocol.getSingleLineReply(inputStream);
    }

    public String getBulkReply() throws JedisException {
	return protocol.getBulkReply(inputStream);
    }

    public int getIntegerReply() throws JedisException {
	return protocol.getIntegerReply(inputStream);
    }

    public List<String> getMultiBulkReply() throws JedisException {
	return protocol.getMultiBulkReply(inputStream);
    }
}