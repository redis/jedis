package redis.clients.jedis;

public interface HighLevelJedisClusterBinaryCommands {
    public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params) ;
}
