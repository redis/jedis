package redis.clients.jedis;

public interface HighLevelBinaryJedisClusterCommands {
    public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params) ;
}
