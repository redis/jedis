package com.googlecode.jedis.util;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;

import com.googlecode.jedis.Pair;
import com.googlecode.jedis.PairImpl;

/**
 * Encoders from and to byte[].
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public class Encoders {

    /**
     * Double to UTF-8 byte[].
     * 
     * @param value
     * @return byte[]
     */
    static public byte[] asByte(final double value) {
	return asByte(String.valueOf(value));
    }

    /**
     * Long to UTF-8 byte[].
     * 
     * @param value
     * @return byte[]
     */
    static public byte[] asByte(final long value) {
	return asByte(String.valueOf(value));
    }

    /**
     * String pair to UTF-8 byte pair.
     * 
     * @param value
     * @return byte pair.
     */
    static public Pair<byte[], byte[]> asByte(Pair<String, String> value) {
	return PairImpl.newPair(asByte(value.getFirst()),
		asByte(value.getSecond()));
    }

    /**
     * String to UTF-8 bytes.
     * 
     * @param value
     * @return byte array
     * @throws NullPointerException
     *             if value is null
     */
    static public byte[] asByte(String value) {
	checkNotNull(value);
	return value.getBytes(UTF_8);
    }

    /**
     * String array to UTF-8 bytes array, null values allowed.
     * 
     * @param value
     * @return byte array
     */
    static public byte[][] asByte(String[] value) {
	byte[][] ret = new byte[value.length][];
	for (int i = 0; i < value.length; i++) {
	    ret[i] = asByte(value[i]);
	}
	return ret;
    }

    /**
     * Byte array to UTF-8 String.
     * 
     * @param value
     *            byte array or null
     * @return string or null if input is null
     */
    static public String asString(byte[] value) {
	return (value != null) ? new String(value, UTF_8) : null;
    }

    /**
     * Long to String.
     * 
     * @param value
     * @return a string
     */
    static public String asString(long value) {
	return String.valueOf(value);
    }
}
