package redis.clients.jedis.commands;

import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

import java.util.Map;

/**
 * Common interface for sharded and non-sharded Jedis
 */
public interface JedisCommands extends JedisBaseCommands {

    Long move(String key, int dbIndex);

    ScanResult<String> sscan(String key, String cursor, ScanParams params);

    ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

    Long bitpos(String key, boolean value);

    Long bitpos(String key, boolean value, BitPosParams params);

    Double hincrByFloat(String key, String field, double value);

    ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

    String restoreReplace(String key, int ttl, byte[] serializedValue);
}
