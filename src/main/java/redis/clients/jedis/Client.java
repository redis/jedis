package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import redis.clients.jedis.Protocol;

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

    public String ping() {
	String command = protocol.buildCommand("PING");
	try {
	    outputStream.write(command.getBytes());
	    return protocol.getSingleLineReply(inputStream);
	} catch (IOException e) {
	    // TODO Not sure what to do here
	    return null;
	}
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

    public String set(String key, String value) {
	String command = protocol.buildCommand("SET", key, value);
	try {
	    outputStream.write(command.getBytes());
	    return protocol.getSingleLineReply(inputStream);
	} catch (IOException e) {
	    // TODO Not sure what to do here
	    return null;
	}
    }

    public String get(String key) {
	String command = protocol.buildCommand("GET", key);
	try {
	    outputStream.write(command.getBytes());
	    return protocol.getBulkReply(inputStream);
	} catch (IOException e) {
	    // TODO Not sure what to do here
	    return null;
	}
    }
}