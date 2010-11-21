package redis.clients.util;

import java.io.UnsupportedEncodingException;

import redis.clients.jedis.JedisException;
import redis.clients.jedis.Protocol;

/**
 * The only reason to have this is to be able to compatible with java 1.5 :(
 * 
 */
public class SafeEncoder {
    public static byte[] encode(final String str) {
        try {
            return str.getBytes(Protocol.CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new JedisException(e);
        }
    }

    public static String encode(final byte[] data) {
        try {
            return new String(data, Protocol.CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new JedisException(e);
        }
    }
}
