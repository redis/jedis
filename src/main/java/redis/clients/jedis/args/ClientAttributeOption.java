package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * CLIENT SETINFO command attr option
 * since redis 7.2
 */
public enum ClientAttributeOption implements Rawable {
    LIB_NAME("LIB-NAME"),
    LIB_VER("LIB-VER");

    private final byte[] raw;

    private ClientAttributeOption(String str) {
        this.raw = SafeEncoder.encode(str);
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }
}
