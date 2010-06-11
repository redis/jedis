package redis.clients.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    public static final String DOLLAR = "$";
    public static final String ASTERISK = "*";
    public static final String PLUS = "+";
    public static final String COLON = ":";
    public static final String COMMAND_DELIMITER = "\r\n";
    public static final int DEFAULT_PORT = 6379;

    public static final byte DOLLAR_BYTE = DOLLAR.getBytes()[0];
    public static final byte ASTERISK_BYTE = ASTERISK.getBytes()[0];
    public static final byte PLUS_BYTE = PLUS.getBytes()[0];
    public static final byte COLON_BYTE = COLON.getBytes()[0];

    public String buildCommand(String name, String... args) {
	StringBuilder builder = new StringBuilder();
	builder.append(ASTERISK).append(args.length + 1).append(
		COMMAND_DELIMITER);
	builder.append(DOLLAR).append(name.length()).append(COMMAND_DELIMITER);
	builder.append(name).append(COMMAND_DELIMITER);
	for (String arg : args) {
	    builder.append(DOLLAR).append(arg.length()).append(
		    COMMAND_DELIMITER).append(arg).append(COMMAND_DELIMITER);
	}
	return builder.toString();
    }

    public String getBulkReply(InputStream is) {
	String ret = null;
	try {
	    if ((byte) is.read() == DOLLAR_BYTE) {
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

    private String readLine(InputStream is) throws IOException {
	byte b;
	StringBuilder sb = new StringBuilder();

	while ((b = (byte) is.read()) != -1) {
	    if (b == '\r') {
		b = (byte) is.read();
		if (b == '\n') {
		    break;
		}
	    }
	    sb.append((char) b);
	}
	return sb.toString();
    }

    public String getSingleLineReply(InputStream is) {
	String ret = null;
	try {
	    if ((byte) is.read() == PLUS_BYTE) {
		ret = readLine(is);
	    }
	} catch (IOException e) {
	    // TODO Not sure that I should return null
	    return null;
	}
	return ret;
    }

    public int getIntegerReply(InputStream is) {
	int ret = 0;
	try {
	    if ((byte) is.read() == COLON_BYTE) {
		String num = readLine(is);
		ret = Integer.parseInt(num);
	    }
	} catch (IOException e) {
	    // TODO Not sure that I should return 0
	    return 0;
	}
	return ret;
    }

    public List<String> getMultiBulkReply(InputStream is) {
	List<String> ret = new ArrayList<String>();
	try {
	    if ((byte) is.read() == ASTERISK_BYTE) {
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