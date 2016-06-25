package redis.clients.jedis;

public interface JedisClusterCustomizedCommands {
    public ScanResult<String> scan(final String cursor, final ScanParams params) ;
}
