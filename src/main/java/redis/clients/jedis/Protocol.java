package redis.clients.jedis;

import redis.clients.util.RedisOutputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

public class Protocol {
    public static final Charset CHARSET = Charset.forName("UTF-8");

    private static final ThreadLocal<CharsetEncoder> CHARSET_ENCODER = new ThreadLocal<CharsetEncoder> (){
        @Override
        protected CharsetEncoder initialValue() {
            return CHARSET.newEncoder();
        }
    };

    public static final String DOLLAR = "$";
    public static final String ASTERISK = "*";
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String COLON = ":";
    public static final String COMMAND_DELIMITER = "\r\n";
    public static final byte[] COMMAND_DELIMITER_BYTES = "\r\n"
	    .getBytes(CHARSET);
    public static final int DEFAULT_PORT = 6379;

    public static final byte DOLLAR_BYTE = DOLLAR.getBytes(CHARSET)[0];
    public static final byte ASTERISK_BYTE = ASTERISK.getBytes(CHARSET)[0];
    public static final byte PLUS_BYTE = PLUS.getBytes(CHARSET)[0];
    public static final byte MINUS_BYTE = MINUS.getBytes(CHARSET)[0];
    public static final byte COLON_BYTE = COLON.getBytes(CHARSET)[0];

    public void sendCommand(RedisOutputStream os, String name, String... args) {
        try {
            final CharsetEncoder encoder = CHARSET_ENCODER.get();

            os.write(ASTERISK_BYTE);
            os.write(String.valueOf(args.length + 1), encoder);
            os.write(COMMAND_DELIMITER_BYTES);
            os.write(DOLLAR_BYTE);
            os.write(String.valueOf(name.length()), encoder);
            os.write(COMMAND_DELIMITER_BYTES);
            os.write(name, encoder);
            os.write(COMMAND_DELIMITER_BYTES);

            for (String arg : args) {
                final byte[] bytes = arg.getBytes(CHARSET);
                int size = bytes.length;

            os.write(DOLLAR_BYTE);
            os.write(String.valueOf(size), encoder);
            os.write(COMMAND_DELIMITER_BYTES);
            os.write(bytes);
            os.write(COMMAND_DELIMITER_BYTES);
            }
            os.flush ();
        } catch (IOException e) {
            throw new JedisException(e);
        }

//		try {
//		    os.write(os.toByteArray());
	    /*
	    os.write(ASTERISK_BYTE);
	    os.write((new Integer(args.length + 1)).toString()
		    .getBytes(CHARSET));
	    os.write(COMMAND_DELIMITER_BYTES);
	    os.write(DOLLAR_BYTE);
	    os.write((new Integer(name.length())).toString().getBytes(CHARSET));
	    os.write(COMMAND_DELIMITER_BYTES);
	    os.write(name.getBytes(CHARSET));
	    os.write(COMMAND_DELIMITER_BYTES);

	    for (String arg : args) {
		byte[] barg = arg.getBytes(CHARSET);

		os.write(DOLLAR_BYTE);
		os.write((new Integer(barg.length)).toString()
			.getBytes(CHARSET));
		os.write(COMMAND_DELIMITER_BYTES);
		os.write(barg);
		os.write(COMMAND_DELIMITER_BYTES);
	    }
	    */
//	} catch (IOException e) {
//	    throw new JedisException(e);
//	}
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

    private String processStatusCodeReply(DataInputStream is) {
	return readLine(is);
    }

    private String processBulkReply(DataInputStream is) {
	int len = Integer.parseInt(readLine(is));
	if (len == -1) {
	    return null;
	}
	byte[] read = new byte[len];
	int offset = 0;
	try {
		while(offset < len) {
		    offset += is.read(read, offset, (len - offset));
		}
	    // read 2 more bytes for the command delimiter
	    is.read();
	    is.read();
	} catch (IOException e) {
	    throw new JedisException(e);
	}

	return new String(read, CHARSET);
    }

    private Integer processInteger(DataInputStream is) {
	String num = readLine(is);
	return Integer.valueOf(num);
    }

    private List<Object> processMultiBulkReply(DataInputStream is) {
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