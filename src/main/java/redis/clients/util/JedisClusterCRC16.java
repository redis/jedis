package redis.clients.util;

public class JedisClusterCRC16 {
    public final static int polynomial = 0x1021; // Represents x^16+x^12+x^5+1
    

    public static int getSlot(String key) {
	int s = key.indexOf("{");
	if (s > -1) {
	    int e = key.indexOf("}", s+1);
	    if (e > -1 && e != s+1) {
		key = key.substring(s+1, e);
	    }
	}
	return getCRC16(key) % 16384;
    }
    
    public static int getSlot(byte[] key) {
        int s = -1;
        int e = -1;
        boolean sFound = false;
        for (int i = 0; i < key.length; i++) {
            if (key[i] == '{' && !sFound) {
                s = i;
                sFound = true;
            }
            if (key[i] == '}' && sFound) {
                e = i;
                break;
            }
        }
        if (s > -1 && e > -1 && e != s + 1) {
            return getCRC16(key, s, e) % 16384;
        }
        return getCRC16(key, 0, key.length) % 16384;
    }

    private static int getCRC16(String key) {
	int crc = 0x0000;
	for (byte b : key.getBytes()) {
	    for (int i = 0; i < 8; i++) {
		boolean bit = ((b >> (7 - i) & 1) == 1);
		boolean c15 = ((crc >> 15 & 1) == 1);
		crc <<= 1;
		// If coefficient of bit and remainder polynomial = 1 xor crc
		// with polynomial
		if (c15 ^ bit)
		    crc ^= polynomial;
	    }
	}

	return crc &= 0xffff ;
    }
    
    private static int getCRC16(byte[] key, int s, int e) {
        int crc = 0x0000;
        for (int i = s; i < e; i++){
            for (int j = 0; j < 8; j++) {
                boolean bit = ((key[i] >> (7 - j) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                // If coefficient of bit and remainder polynomial = 1 xor crc
                // with polynomial
                if (c15 ^ bit)
                    crc ^= polynomial;
                }
        }
     
        return crc &= 0xffff ;
    }
}