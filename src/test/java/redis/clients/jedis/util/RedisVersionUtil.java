package redis.clients.jedis.util;

import io.redis.test.utils.RedisInfo;
import io.redis.test.utils.RedisVersion;
import redis.clients.jedis.*;

public class RedisVersionUtil {

  static final String FORCE_REDIS_SERVER_VERSION_ENV = "forceRedisServerVersion";

  static final RedisVersion forcedVersion = System.getenv(FORCE_REDIS_SERVER_VERSION_ENV) != null
          ? RedisVersion.of(System.getenv(FORCE_REDIS_SERVER_VERSION_ENV))
          : null;

  public static RedisVersion getRedisVersion(Connection conn) {
    if (forcedVersion != null) {
      return forcedVersion;
    }

    try (Jedis jedis = new Jedis(conn)) {
      return getRedisVersion(jedis);
    }
  }

  public static RedisVersion getRedisVersion(UnifiedJedis jedis) {
    if (forcedVersion != null) {
      return forcedVersion;
    }

    Object response = SafeEncoder.encodeObject(jedis.sendCommand(Protocol.Command.INFO, "server"));
    RedisInfo info = RedisInfo.parseInfoServer(response.toString());
    return RedisVersion.of(info.getRedisVersion());
  }

  public static RedisVersion getRedisVersion(Jedis jedis) {
    if (forcedVersion != null) {
      return forcedVersion;
    }

    RedisInfo info = RedisInfo.parseInfoServer(jedis.info("server"));
    return RedisVersion.of(info.getRedisVersion());
  }

  public static RedisVersion getRedisVersion(EndpointConfig endpoint) {
    if (forcedVersion != null) {
      return forcedVersion;
    }

    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      return getRedisVersion(jedis);
    }
  }
}
