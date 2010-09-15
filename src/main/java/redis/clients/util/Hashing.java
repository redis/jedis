package redis.clients.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Hashing {
    public static final Hashing MURMURE_HASH = new Hashing() {
	public long hash(String key) {
	    // 'm' and 'r' are mixing constants generated offline.
	    // They're not really 'magic', they just happen to work well.
	    byte[] data = key.getBytes();
	    int seed = 0x1234ABCD;
	    int m = 0x5bd1e995;
	    int r = 24;

	    // Initialize the hash to a 'random' value
	    int len = data.length;
	    int h = seed ^ len;

	    int i = 0;
	    while (len >= 4) {
		int k = data[i + 0] & 0xFF;
		k |= (data[i + 1] & 0xFF) << 8;
		k |= (data[i + 2] & 0xFF) << 16;
		k |= (data[i + 3] & 0xFF) << 24;

		k *= m;
		k ^= k >>> r;
		k *= m;

		h *= m;
		h ^= k;

		i += 4;
		len -= 4;
	    }

	    switch (len) {
	    case 3:
		h ^= (data[i + 2] & 0xFF) << 16;
	    case 2:
		h ^= (data[i + 1] & 0xFF) << 8;
	    case 1:
		h ^= (data[i + 0] & 0xFF);
		h *= m;
	    }

	    h ^= h >>> 13;
	    h *= m;
	    h ^= h >>> 15;

	    return h;
	}
    };
    public static final Hashing MD5 = new Hashing() {
	private MessageDigest md5 = null; // avoid recurring construction

	public long hash(String key) {
	    if (md5 == null) {
		try {
		    md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		    throw new IllegalStateException(
			    "++++ no md5 algorythm found");
		}
	    }

	    md5.reset();
	    md5.update(key.getBytes());
	    byte[] bKey = md5.digest();
	    long res = ((long) (bKey[3] & 0xFF) << 24)
		    | ((long) (bKey[2] & 0xFF) << 16)
		    | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
	    return res;
	}
    };

    public abstract long hash(String key);
}