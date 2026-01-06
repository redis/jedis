package redis.clients.jedis.util;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisInfo;
import io.redis.test.utils.RedisVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.util.Optional;
import java.util.function.Supplier;

import static redis.clients.jedis.util.RedisVersionUtil.forcedVersion;

public class RedisVersionCondition implements ExecutionCondition {
  private static final Logger logger = LoggerFactory.getLogger(RedisVersionCondition.class);

  private final Supplier<EndpointConfig> endpointSupplier;
  private HostAndPort hostPort;
  private JedisClientConfig config;

  public RedisVersionCondition(Supplier<EndpointConfig> endpointSupplier) {
    this.endpointSupplier = endpointSupplier;
    this.hostPort = null;
    this.config = null;
  }

  public RedisVersionCondition(HostAndPort hostPort, JedisClientConfig config) {
    this.endpointSupplier = null;
    this.hostPort = hostPort;
    this.config = config;
  }

  private void ensureInitialized() {
    if (hostPort == null && endpointSupplier != null) {
      EndpointConfig endpoint = endpointSupplier.get();
      this.hostPort = endpoint.getHostAndPort();
      this.config = endpoint.getClientConfigBuilder().build();
    }
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try {
      ensureInitialized();
    } catch (TestAbortedException e) {
      return ConditionEvaluationResult.disabled(e.getMessage());
    }

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