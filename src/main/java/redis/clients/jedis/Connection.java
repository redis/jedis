package redis.clients.jedis;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Connection {
    private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private boolean connected = false;
    private Protocol protocol = new Protocol();
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private int pipelinedCommands = 0;

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
	if (!connected) {
	    socket = new Socket(host, port);
	    connected = socket.isConnected();
	    outputStream = new DataOutputStream(socket.getOutputStream());
	    inputStream = new DataInputStream(new BufferedInputStream(socket
		    .getInputStream()));
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