package redis.clients.jedis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    public void sendCommand(DataOutputStream os, String name, String... args) {
	StringBuilder builder = new StringBuilder(ASTERISK + (args.length + 1)
		+ COMMAND_DELIMITER + DOLLAR + name.length()
		+ COMMAND_DELIMITER + name + COMMAND_DELIMITER);
	for (String arg : args) {
	    builder.append(DOLLAR).append(arg.length()).append(
		    COMMAND_DELIMITER).append(arg).append(COMMAND_DELIMITER);
	}
	try {
	    os.write(builder.toString().getBytes());
	} catch (IOException e) {
	    throw new JedisException(e);
	}
    }

    public void processError(DataInputStream is) {
	String message = readLine(is);
	throw new JedisException(message);
    }

    private String readLine(DataInputStream is) {
	byte b;
	byte c;
	StringBuilder sb = new StringBuilder();

	try {
	    while ((b = is.readByte()) != -1) {
		if (b == '\r') {
		    c = is.readByte();
		    if (c == '\n') {
			break;
		    }
		    sb.append((char) b);
		    sb.append((char) c);
		} else {
		    sb.append((char) b);
		}
	    }
	} catch (IOException e) {
	    throw new JedisException(e);
	}
	return sb.toString();
    }

    private Object process(DataInputStream is) {
	try {
	    byte b = is.readByte();
	    if (b == MINUS_BYTE) {
		processError(is);
	    } else if (b == ASTERISK_BYTE) {
		return processMultiBulkReply(is);
	    } else if (b == COLON_BYTE) {
		return processInteger(is);
	    } else if (b == DOLLAR_BYTE) {
		return processBulkReply(is);
	    } else if (b == PLUS_BYTE) {
		return processStatusCodeReply(is);
	    } else {
		throw new JedisException("Unknown reply: " + (char) b);
	    }
	} catch (IOException e) {
	    throw new JedisException(e);
	}
	return null;
    }

    private Object processStatusCodeReply(DataInputStream is) {
	String ret = null;
	ret = readLine(is);
	return ret;
    }

    private Object processBulkReply(DataInputStream is) {
	int len = Integer.parseInt(readLine(is));
	if (len == -1) {
	    return null;
	}
	byte[] read = new byte[len];
	try {
	    is.read(read);
	    // read 2 more bytes for the command delimiter
	    is.read();
	    is.read();
	} catch (IOException e) {
	    throw new JedisException(e);
	}

	return new String(read);
    }

    private Object processInteger(DataInputStream is) {
	int ret = 0;
	String num = readLine(is);
	ret = Integer.parseInt(num);
	return ret;
    }

    private Object processMultiBulkReply(DataInputStream is) {
	int num = Integer.parseInt(readLine(is));
	if (num == -1) {
	    return null;
	}
	List<Object> ret = new ArrayList<Object>();
	for (int i = 0; i < num; i++) {
	    ret.add(process(is));
	}
	return ret;
    }

    public Object read(DataInputStream is) {
	return process(is);
    }
}