package io.redis.test.utils;

import redis.clients.jedis.*;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Map;

import static redis.clients.jedis.util.TlsUtil.createTrustAllSslSocketFactory;

public class RedisVersionUtil {


  public static RedisVersion getRedisVersion(Connection conn) {

    try (Jedis jedis = new Jedis(conn)) {
      Object response = SafeEncoder.encodeObject(jedis.sendCommand(Protocol.Command.INFO, "server"));
      RedisInfo info = RedisInfo.parseInfoServer(response.toString());
      return RedisVersion.of(info.getRedisVersion());
    }
  }

  public static RedisVersion getRedisVersion(UnifiedJedis jedis) {

    Object response = SafeEncoder.encodeObject(jedis.sendCommand(Protocol.Command.INFO, "server"));
    RedisInfo info = RedisInfo.parseInfoServer(response.toString());
    return RedisVersion.of(info.getRedisVersion());
  }

  public static RedisVersion getRedisVersion(Jedis jedis) {

    RedisInfo info = RedisInfo.parseInfoServer(jedis.info("server"));
    return RedisVersion.of(info.getRedisVersion());
  }

  public static RedisVersion getRedisVersion(EndpointConfig endpoint) {
    DefaultJedisClientConfig.Builder builder = endpoint.getClientConfigBuilder();
    if (endpoint.isTls()) {
      builder.sslSocketFactory(createTrustAllSslSocketFactory());
    }
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), builder.build())) {
      return getRedisVersion(jedis);
    }
  }
}
