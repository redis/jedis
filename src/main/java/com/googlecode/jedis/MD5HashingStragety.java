package com.googlecode.jedis;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class MD5HashingStragety implements HashingStrategy {

    private Charset charset = Protocol.DEFAULT_CHARSET;
    private MessageDigest md5 = null;
    private final String hashName = "MD5";

    public MD5HashingStragety() {
	try {
	    this.md5 = MessageDigest.getInstance(hashName);
	} catch (NoSuchAlgorithmException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public Charset getCharset() {
	return charset;
    }

    public String getHashName() {
	return this.hashName;
    }

    public long hash64(byte[] key) {
	md5.reset();
	md5.update(key);
	byte[] bKey = md5.digest();
	long res = ((long) (bKey[3] & 0xFF) << 24)
		| ((long) (bKey[2] & 0xFF) << 16)
		| ((long) (bKey[1] & 0xFF) << 8) | (bKey[0] & 0xFF);
	return res;
    }

    public long hash64(String key) {
	return hash64(key.getBytes(charset));
    }

    public void setCharset(Charset charset) {
	this.charset = charset;
    }

}
