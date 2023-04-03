package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * CLIENT SETINFO command attr option
 * since redis 7.2
 */
public enum ClientAttrOption implements Rawable {
    LIB_NAME("LIB-NAME"),
    LIB_VER("LIB-VER");

    private final byte[] raw;

    private ClientAttrOption(String str) {
        this.raw = SafeEncoder.encode(str);
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }
}
