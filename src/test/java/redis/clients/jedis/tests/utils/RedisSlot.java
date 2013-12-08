package redis.clients.jedis.tests.utils;

public class RedisSlot {
	public final static int polynomial = 0x1021;	// Represents x^16+x^12+x^5+1
	static int crc;
	
	public RedisSlot(){
		crc = 0x0000;
	}
	
	public static int getSlot(String key) { 
        for (byte b : key.getBytes()) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                // If coefficient of bit and remainder polynomial = 1 xor crc with polynomial
                if (c15 ^ bit) crc ^= polynomial;
             }
        }

        return crc &= 0xffff % 16384;
    }	
	
}