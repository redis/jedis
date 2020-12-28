package redis.clients.jedis.tests.utils;

import redis.clients.jedis.Jedis;

public class RedisVersionUtil {
    String completeVersion = null;

    public RedisVersionUtil(Jedis jedis) {
        String info = jedis.info("server");
        String[] splitted = info.split("\\s+|:");
        for (int i = 0; i < splitted.length; i++) {
            if (splitted[i].equalsIgnoreCase("redis_version")) {
                completeVersion = splitted[i + 1];
                i = splitted.length; // out of the loop
            }
        }
        this.completeVersion = completeVersion;
    }

    public int getRedisMajorVersionNumber() {
        if (completeVersion == null) {
            return 0;
        } else {
            return Integer.parseInt(completeVersion.substring(0, completeVersion.indexOf(".")));
        }
    }
}
