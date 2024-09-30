package redis.clients.jedis;

import redis.clients.jedis.resps.StreamEntryBinary;

import java.util.List;

public class JedisBinaryStream extends Jedis {
    public JedisBinaryStream(String host) {
        super(host);
    }

    public List<StreamEntryBinary> xrangeBinary(byte[] key, byte[] start, byte[] end, int count) {
        List<Object> rawResponse = super.xrange(key, start, end, count);
        return BuilderFactory.STREAM_ENTRY_BINARY.build(rawResponse);
    }

    public List<StreamEntryBinary> xrangeBinary(byte[] key, byte[] start, byte[] end) {
        List<Object> rawResponse = super.xrange(key, start, end);
        return BuilderFactory.STREAM_ENTRY_BINARY.build(rawResponse);
    }
}
