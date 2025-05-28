package redis.clients.jedis.util;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisInfo;
import io.redis.test.utils.RedisVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.lang.reflect.Method;
import java.util.Optional;

import static redis.clients.jedis.util.RedisVersionUtil.forcedVersion;

public class RedisVersionCondition implements ExecutionCondition {
  private static final Logger logger = LoggerFactory.getLogger(RedisVersionCondition.class);

  private final HostAndPort hostPort;
  private final JedisClientConfig config;

  public RedisVersionCondition(EndpointConfig endpoint) {
    this.hostPort = endpoint.getHostAndPort();
    this.config = endpoint.getClientConfigBuilder().build();
  }

  public RedisVersionCondition(HostAndPort hostPort, JedisClientConfig config) {
    this.hostPort = hostPort;
    this.config = config;
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try (Jedis jedisClient = new Jedis(hostPort, config)) {
      SinceRedisVersion versionAnnotation = getAnnotation(context);
      if (versionAnnotation != null) {
        RedisVersion currentVersion;

        if (forcedVersion != null) {
          logger.info("Using forced Redis server version from environment variable: " + forcedVersion);
          currentVersion = forcedVersion;
        } else {
          RedisInfo info = RedisInfo.parseInfoServer(jedisClient.info("server"));
          currentVersion = RedisVersion.of(info.getRedisVersion());
        }

        RedisVersion minRequiredVersion = RedisVersion.of(versionAnnotation.value());
        if (currentVersion.isLessThan(minRequiredVersion)) {
          return ConditionEvaluationResult.disabled("Test requires Redis version " + minRequiredVersion + " or later, but found " + currentVersion);
        }
      }
    } catch (Exception e) {
      return ConditionEvaluationResult.disabled("Failed to check Redis version: " + e.getMessage());
    }
    return ConditionEvaluationResult.enabled("Redis version is sufficient");
  }

  private SinceRedisVersion getAnnotation(ExtensionContext context) {
    Optional<SinceRedisVersion> methodAnnotation = AnnotationUtils.findAnnotation(context.getTestMethod(), SinceRedisVersion.class);
    if (methodAnnotation.isPresent()) {
      return methodAnnotation.get();
    }

    Optional<SinceRedisVersion> classAnnotation = AnnotationUtils.findAnnotation(context.getRequiredTestClass(), SinceRedisVersion.class);
    return classAnnotation.orElse(null);
  }
}