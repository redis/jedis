package io.redis.test.utils;

import java.util.HashMap;
import java.util.Map;

public class RedisInfo {
    private final Map<String, String> infoMap;

    public RedisInfo() {
        this.infoMap = new HashMap<>();
    }

    public void setField(String key, String value) {
        infoMap.put(key, value);
    }

    public String getField(String key) {
        return infoMap.get(key);
    }

    public String getRedisVersion() {
        return infoMap.get("redis_version");
    }

    public String getOs() {
        return infoMap.get("os");
    }

    public String getMode() {
        return infoMap.get("redis_mode");
    }

    public String getPorts() {
        return infoMap.get("tcp_port"); // Assuming "tcp_port" is the key for ports
    }

    @Override
    public String toString() {
        return "RedisInfo{" +
                "infoMap=" + infoMap +
                '}';
    }

    public static RedisInfo parseInfoServer(String infoOutput) {
        RedisInfo redisInfo = new RedisInfo();

        String[] lines = infoOutput.split("\n");

        for (String line : lines) {
            // Only parse lines that contain a colon (indicating a key-value pair)
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    redisInfo.setField(parts[0].trim(), parts[1].trim());
                }
            }
        }

        // You can still check for required fields if necessary
        // Example: Ensure that specific fields are set
        if (redisInfo.getField("redis_version") == null || redisInfo.getField("redis_mode") == null) {
            throw new IllegalArgumentException("Missing required fields in Redis server info.");
        }

        return redisInfo;
    }
}