package redis.clients.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import redis.clients.jedis.Protocol;

public interface Hashing {
    public static final Hashing MURMUR_HASH = new MurmurHash();

    public static final Hashing MD5 = new Hashing() {
	private MessageDigest md5 = null; // avoid recurring construction
	
	public long hash(String key) {
		return hash(key.getBytes(Protocol.UTF8));
	}

	public long hash(byte[] key) {
	    if (md5 == null) {
		try {
		    md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		    throw new IllegalStateException(
			    "++++ no md5 algorythm found");
		}
	    }

	    md5.reset();
	    md5.update(key);
	    byte[] bKey = md5.digest();
	    long res = ((long) (bKey[3] & 0xFF) << 24)
		    | ((long) (bKey[2] & 0xFF) << 16)
		    | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
	    return res;
	}
    };

    public long hash(String key);
    public long hash(byte[] key);
}