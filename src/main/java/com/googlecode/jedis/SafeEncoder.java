package com.googlecode.jedis;

/**
 * The only reason to have this is to be able to compatible with java 1.5 :(
 * 
 */
class SafeEncoder {
    public static String encode(final byte[] data) {
	return new String(data, Protocol.DEFAULT_CHARSET);

    }

    public static byte[] encode(final String str) {
	return str.getBytes(Protocol.DEFAULT_CHARSET);

    }
}
