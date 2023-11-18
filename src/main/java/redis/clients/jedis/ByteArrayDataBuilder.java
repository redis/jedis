package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

public class ByteArrayDataBuilder implements DataBuilder {

    @Override
    public Object build(Object data) {
        if (!(data instanceof byte[])) {
            throw new IllegalArgumentException("Data must be of type byte[]");
        }
        return SafeEncoder.encode((byte[]) data);
    }
}
