package redis.clients.jedis;

import java.io.IOException;

public class Jedis extends Client {
    public Jedis(String host) {
	super(host);
    }

    public String ping() {
	// TODO: I want to be able to do the followin
	// return protocol.runCommand(os,
	// "PING").getSingleLineReply(inputStream);
	// or even maybe
	// return protocol.runCommand("PING").getSingleLineReply();

	String command = protocol.buildCommand("PING");
	try {
	    outputStream.write(command.getBytes());
	    return protocol.getSingleLineReply(inputStream);
	} catch (IOException e) {
	    // TODO Not sure what to do here
	    return null;
	}
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
