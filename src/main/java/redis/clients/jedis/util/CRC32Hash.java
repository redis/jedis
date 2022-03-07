package redis.clients.jedis.util;

/**
 * @Author chenchen
 * @Description Customized CRC32 non-encrypted hash algorithm, compatible with Python CRC32 implementation
 **/
public class CRC32Hash implements Hashing {

    private static final long[] crc32Table = new long[256];

    //CRC polynomial value for each bucket
    static {
        long crcValue;
        for (int i = 0; i < 256; i++) {
            crcValue = i;
            for (int j = 0; j < 8; j++) {
                //Parity check
                if ((crcValue & 1) == 1) {
                    //Model 2 operation <=> Moves to the right one
                    crcValue = crcValue >> 1;
                    //Xor with 1110 1101 1011 1000 1000 0011 0010 0000
                    crcValue = 0x00000000edb88320L ^ crcValue;
                } else {
                    //Model 2 operation <=> Moves to the right one
                    crcValue = crcValue >> 1;
                }
            }
            crc32Table[i] = crcValue;
        }
    }


    @Override
    public long hash(String key) {
        return hash(SafeEncoder.encode(key));
    }

    @Override
    public long hash(byte[] key) {
        //The initial value of value is 2^32
        long value = 0x00000000ffffffffL;
        for (byte b : key) {
            //Xor is performed on value before and
            int index = (int) ((value ^ b) & 0xff);
            // The calculated bucket is xOR for data whose subscript shifts one byte right with value
            value = crc32Table[index] ^ (value >> 8);
        }
        // key Xor with 2^32 after all bits are traversed
        value = value ^ 0x00000000ffffffffL;
        return value;
    }


}
