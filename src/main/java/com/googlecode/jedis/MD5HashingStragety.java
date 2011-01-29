package com.googlecode.jedis;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MD5HashingStragety implements HashingStrategy {

    private Charset charset = Protocol.DEFAULT_CHARSET;
    private final String hashName = "MD5";
    private MessageDigest md5 = null;

    public MD5HashingStragety() {
	try {
	    md5 = MessageDigest.getInstance(hashName);
	} catch (final NoSuchAlgorithmException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    @Override
    public Charset getCharset() {
	return charset;
    }

    @Override
    public String getHashName() {
	return hashName;
    }

    @Override
    public long hash64(final byte[] key) {
	md5.reset();
	md5.update(key);
	final byte[] bKey = md5.digest();
	final long res = ((long) (bKey[3] & 0xFF) << 24)
		| ((long) (bKey[2] & 0xFF) << 16)
		| ((long) (bKey[1] & 0xFF) << 8) | (bKey[0] & 0xFF);
	return res;
    }

    @Override
    public long hash64(final String key) {
	return hash64(key.getBytes(charset));
    }

    @Override
    public void setCharset(final Charset charset) {
	this.charset = charset;
    }

}
