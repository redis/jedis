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
      this.config = endpoint.getClientConfigBuilder().serverDefaultProtocol().build();
    }
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    ensureInitialized();
    try (Jedis jedisClient = new Jedis(hostPort, config)) {
      RedisVersion minRequiredVersion = getMaxRequiredVersion(context);
      if (minRequiredVersion != null) {
        RedisVersion currentVersion;

        if (forcedVersion != null) {
          logger.info("Using forced Redis server version from environment variable: " + forcedVersion);
          currentVersion = forcedVersion;
        } else {
          RedisInfo info = RedisInfo.parseInfoServer(jedisClient.info("server"));
          currentVersion = RedisVersion.of(info.getRedisVersion());
        }

        if (currentVersion.isLessThan(minRequiredVersion)) {
          return ConditionEvaluationResult.disabled("Test requires Redis version " + minRequiredVersion + " or later, but found " + currentVersion);
        }
      }
    } catch (Exception e) {
      return ConditionEvaluationResult.disabled("Failed to check Redis version: " + e.getMessage());
    }
    return ConditionEvaluationResult.enabled("Redis version is sufficient");
  }

  /**
   * Returns the highest required Redis version from both class-level and method-level
   * {@link SinceRedisVersion} annotations. This ensures that when a class requires e.g. 8.0.0
   * and a method requires 7.4.0, the class-level constraint is not bypassed.
   */
  private RedisVersion getMaxRequiredVersion(ExtensionContext context) {
    Optional<SinceRedisVersion> methodAnnotation = AnnotationUtils.findAnnotation(context.getTestMethod(), SinceRedisVersion.class);
    Optional<SinceRedisVersion> classAnnotation = AnnotationUtils.findAnnotation(context.getRequiredTestClass(), SinceRedisVersion.class);

    RedisVersion methodVersion = methodAnnotation.map(a -> RedisVersion.of(a.value())).orElse(null);
    RedisVersion classVersion = classAnnotation.map(a -> RedisVersion.of(a.value())).orElse(null);

    if (methodVersion == null) return classVersion;
    if (classVersion == null) return methodVersion;
    return classVersion.isGreaterThan(methodVersion) ? classVersion : methodVersion;
  }
}