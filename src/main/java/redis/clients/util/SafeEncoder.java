package redis.clients.util;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * The only reason to have this is to be able to compatible with java 1.5 :(
 * 
 */
public class SafeEncoder {
    public static byte[][] encodeMany(final String... strs) {
	final byte[][] many = new byte[strs.length][];
	for (int i = 0; i < strs.length; ++i) {
	    many[i] = encode(strs[i]);
	}
	return many;
    }

    public static byte[][] encodeMany(final List<String> strs){
	final byte[][] many = new byte[strs.size()][];
	for (int i = 0; i < strs.size(); ++i){
	    many[i] = encode(strs.get(i));
	}
	return many;
    }
	
    public static byte[] encode(final String str) {
	try {
	    if (str == null) {
		throw new JedisDataException(
			"value sent to redis cannot be null");
	    }
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
