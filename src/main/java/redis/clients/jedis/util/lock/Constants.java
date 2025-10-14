package redis.clients.jedis.util.lock;

public class Constants {
    public static final String ACQUIRE_LOCK_SCRIPT = "if (redis.call('EXISTS', KEYS[1]) == 0) then " +
            "redis.call('HINCRBY', KEYS[1], ARGV[1], 1); " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "return -1; " +
            "end; " +
            "if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('HINCRBY', KEYS[1], ARGV[1], 1); " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "return -1; " +
            "end; " +
            "return redis.call('PTTL', KEYS[1]);";

    public static final String ACQUIRE_UNLOCK_SCRIPT = "if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('HINCRBY', KEYS[1], ARGV[1], -1); " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "if (tonumber(redis.call('HGET', KEYS[1], ARGV[1])) < 1) then " +
            "redis.call('DEL', KEYS[1]); " +
            "redis.call('PUBLISH', KEYS[1], 1); " +
            "return 1; " +
            "end; " +
            "return 2; " +
            "end; " +
            "return 0;";

    public static final String ACQUIRE_FORCE_UNLOCK_SCRIPT = "if (redis.call('DEL', KEYS[1]) == 1) then " +
            "redis.call('PUBLISH', KEYS[1], 1); " +
            "return 1; " +
            "end; " +
            "return 0;";

    public static final String UPDATE_LOCK_TTL_SCRIPT = "if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then " +
            "redis.call('PEXPIRE', KEYS[1], ARGV[2]); " +
            "end;";

    public static final int DEFAULT_KEY_TTL = 0x7530;

    public static int DEFAULT_UPDATE_TIME = 0xa;
}
