package redis.clients.jedis.json;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public enum ExistenceModifier implements ProtocolCommand {
    DEFAULT(""),
    NOT_EXISTS("NX"),
    MUST_EXIST("XX");

    private final byte[] raw;

    ExistenceModifier(String alt) {
        raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }
}
