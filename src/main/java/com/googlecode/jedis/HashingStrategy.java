package com.googlecode.jedis;

import java.nio.charset.Charset;

/**
 * Hash to use with sharded jedis.
 * 
 * @author Jonathan Leibiusky
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public interface HashingStrategy {

    /**
     * Get the charset string will be decoded with.
     * 
     * @return the charset
     */
    Charset getCharset();

    /**
     * Get the name of the used hash function.
     * 
     * @return name of the hash
     */
    String getHashName();

    /**
     * Create a 64bit hash.
     * 
     * @param data
     *            the byte array to hash
     * @return a hash
     */
    long hash64(final byte[] data);

    /**
     * Create a 64bit hash from a String.
     * 
     * @param data
     *            String to hash
     * @return a hash
     */
    long hash64(final String data);

    /**
     * Set the charset to transform string to byte[] before hashing.
     * 
     * @param charset
     *            the charset
     */
    void setCharset(final Charset charset);

}
