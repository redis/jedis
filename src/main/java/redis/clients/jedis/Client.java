package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    protected String host;
    protected int port = Protocol.DEFAULT_PORT;
    protected Socket socket;
    protected boolean connected = false;
    protected Protocol protocol = new Protocol();
    protected OutputStream outputStream;
    protected InputStream inputStream;

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

}