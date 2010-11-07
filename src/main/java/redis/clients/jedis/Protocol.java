package redis.clients.jedis;

import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static redis.clients.util.RedisOutputStream.CHARSET;

public final class Protocol {

    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_TIMEOUT = 2000;

    public static final byte DOLLAR_BYTE = '$';
    public static final byte ASTERISK_BYTE = '*';
    public static final byte PLUS_BYTE = '+';
    public static final byte MINUS_BYTE = '-';
    public static final byte COLON_BYTE = ':';

    public void sendCommand(RedisOutputStream os, String name, String... args) {
        try {
            os.write(ASTERISK_BYTE);
            os.writeIntCrLf(args.length + 1);
            os.write(DOLLAR_BYTE);
            os.writeIntCrLf(name.length());
            os.writeAsciiCrLf(name);

            for (String str : args) {
                os.write(DOLLAR_BYTE);
                final int size = RedisOutputStream.utf8Length(str);
                os.writeIntCrLf(size);
                if (size == str.length())
                    os.writeAsciiCrLf(str);
                else {
                    os.writeUtf8CrLf(str);
                }
            }
            os.flush();
        } catch (IOException e) {
            throw new JedisException(e);
        }
    }

    private void processError(RedisInputStream is) {
        String message = is.readLine();
        throw new JedisException(message);
    }

    private Object process(RedisInputStream is) {
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

    private String processStatusCodeReply(RedisInputStream is) {
        return is.readLine();
    }

    private String processBulkReply(RedisInputStream is) {
        int len = Integer.parseInt(is.readLine());
        if (len == -1) {
            return null;
        }
        byte[] read = new byte[len];
        int offset = 0;
        try {
            while (offset < len) {
                offset += is.read(read, offset, (len - offset));
            }
            // read 2 more bytes for the command delimiter
            is.readByte();
            is.readByte();
        } catch (IOException e) {
            throw new JedisException(e);
        }

        return new String(read, CHARSET);
    }

    private Integer processInteger(RedisInputStream is) {
        String num = is.readLine();
        return Integer.valueOf(num);
    }

    private List<Object> processMultiBulkReply(RedisInputStream is) {
        int num = Integer.parseInt(is.readLine());
        if (num == -1) {
            return null;
        }
        List<Object> ret = new ArrayList<Object>(num);
        for (int i = 0; i < num; i++) {
            ret.add(process(is));
        }
        return ret;
    }

    public Object read(RedisInputStream is) {
        return process(is);
    }
}