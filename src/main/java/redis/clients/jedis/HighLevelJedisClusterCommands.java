package redis.clients.jedis;

public interface HighLevelJedisClusterCommands {
    public ScanResult<String> scan(final String cursor, final ScanParams params) ;
}
