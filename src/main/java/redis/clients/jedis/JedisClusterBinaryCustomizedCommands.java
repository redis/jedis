package redis.clients.jedis;

public interface JedisClusterBinaryCustomizedCommands {
    public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params) ;
}
