package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    public static final String DOLLAR = "$";
    public static final String ASTERISK = "*";
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String COLON = ":";
    public static final String COMMAND_DELIMITER = "\r\n";
    public static final int DEFAULT_PORT = 6379;

    public static final byte DOLLAR_BYTE = DOLLAR.getBytes()[0];
    public static final byte ASTERISK_BYTE = ASTERISK.getBytes()[0];
    public static final byte PLUS_BYTE = PLUS.getBytes()[0];
    public static final byte MINUS_BYTE = MINUS.getBytes()[0];
    public static final byte COLON_BYTE = COLON.getBytes()[0];

    public void sendCommand(OutputStream os, String name, String... args) {
	StringBuilder builder = new StringBuilder();
	builder.append(ASTERISK).append(args.length + 1).append(
		COMMAND_DELIMITER);
	builder.append(DOLLAR).append(name.length()).append(COMMAND_DELIMITER);
	builder.append(name).append(COMMAND_DELIMITER);
	for (String arg : args) {
	    builder.append(DOLLAR).append(arg.length()).append(
		    COMMAND_DELIMITER).append(arg).append(COMMAND_DELIMITER);
	}
	try {
	    os.write(builder.toString().getBytes());
	} catch (IOException e) {
	    // TODO don't know what to do here!
	}
    }

    public void processError(InputStream is) throws JedisException {
	String message = readLine(is);
	throw new JedisException(message);
    }

    private String readLine(InputStream is) {
	byte b;
	StringBuilder sb = new StringBuilder();

	try {
	    while ((b = (byte) is.read()) != -1) {
		if (b == '\r') {
		    b = (byte) is.read();
		    if (b == '\n') {
			break;
		    }
		}
		sb.append((char) b);
	    }
	} catch (IOException e) {
	    // TODO Dont know what to do here!
	}
	return sb.toString();
    }

    public String getBulkReply(InputStream is) throws JedisException {
	String ret = null;
	try {
	    byte b = (byte) is.read();
	    if (b == MINUS_BYTE) {
		processError(is);
	    }

	    if (b == DOLLAR_BYTE) {
		int len = Integer.parseInt(readLine(is));
		if (len == -1) {
		    return null;
		}
		byte[] read = new byte[len];
		is.read(read);
		// read 2 more bytes for the command delimiter
		is.read();
		is.read();

		ret = new String(read);

	    }
	} catch (IOException e) {
	    // TODO Not sure that I should return null
	    return null;
	}
	return ret;
    }

    public String getSingleLineReply(InputStream is) throws JedisException {
	String ret = null;
	try {
	    byte b = (byte) is.read();
	    if (b == MINUS_BYTE) {
		processError(is);
	    }

	    if (b == PLUS_BYTE) {
		ret = readLine(is);
	    }
	} catch (IOException e) {
	    // TODO Not sure that I should return null
	    return null;
	}
	return ret;
    }

    public int getIntegerReply(InputStream is) throws JedisException {
	int ret = 0;
	try {
	    byte b = (byte) is.read();
	    if (b == MINUS_BYTE) {
		processError(is);
	    }
	    if (b == COLON_BYTE) {
		String num = readLine(is);
		ret = Integer.parseInt(num);
	    }
	} catch (IOException e) {
	    // TODO Not sure that I should return 0
	    e.printStackTrace();
	    return 0;
	}
	return ret;
    }

    public List<String> getMultiBulkReply(InputStream is) throws JedisException {
	List<String> ret = new ArrayList<String>();
	try {
	    byte b = (byte) is.read();
	    if (b == MINUS_BYTE) {
		processError(is);
	    }
	    if (b == ASTERISK_BYTE) {
		int num = Integer.parseInt(readLine(is));
		for (int i = 0; i < num; i++) {
		    ret.add(getBulkReply(is));
		}
	    }
	} catch (IOException e) {
	    // TODO Not sure that I should return null
	    return null;
	}
	return ret;
    }
}