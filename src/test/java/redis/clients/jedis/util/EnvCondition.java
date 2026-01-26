package redis.clients.jedis.util;

import io.redis.test.annotations.ConditionalOnEnv;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * JUnit 5 execution condition that enables or disables tests based on the test environment.
 * <p>
 * This condition works with the {@link ConditionalOnEnv} annotation to conditionally execute tests
 * depending on the current test environment (e.g., Docker, Redis Enterprise).
 * <p>
 * The environment is determined using {@link TestEnvUtil}.
 * <p>
 * Example usage with JUnit 5:
 *
 * <pre>
 * &#64;ExtendWith(EnvCondition.class)
 * &#64;ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_DOCKER, enabled = true)
 * public class DockerOnlyTest {
 *   // Tests in this class only run in Docker environment
 * }
 *
 * &#64;ExtendWith(EnvCondition.class)
 * &#64;ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
 * public class NotOnEnterpriseTest {
 *   // Tests in this class are skipped in Redis Enterprise environment
 * }
 * </pre>
 */
public class EnvCondition implements ExecutionCondition {
  private static final Logger logger = LoggerFactory.getLogger(EnvCondition.class);

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    String currentEnv = TestEnvUtil.getTestEnvProvider();

    ConditionalOnEnv annotation = getConditionalOnEnvAnnotation(context);
    if (annotation != null) {
      return evaluateConditionalOnEnv(annotation, currentEnv);
    }

    return ConditionEvaluationResult.enabled("No @ConditionalOnEnv annotation found");
  }

  private ConditionEvaluationResult evaluateConditionalOnEnv(ConditionalOnEnv annotation,
      String currentEnv) {
    String[] envs = annotation.value();
    boolean enabled = annotation.enabled();

    boolean matches = Arrays.stream(envs).anyMatch(env -> env.equalsIgnoreCase(currentEnv));

    if (enabled) {
      // enabled = true: test runs ONLY when environment matches
      if (matches) {
        logger.debug("Test enabled: current environment '{}' matches one of {}", currentEnv,
          Arrays.toString(envs));
        return ConditionEvaluationResult
            .enabled("Current environment '" + currentEnv + "' is in the enabled list");
      }

      String message = annotation.message();
      String disabledReason = message.isEmpty()
          ? "Test requires environment " + Arrays.toString(envs) + ", but current environment is '"
              + currentEnv + "'"
          : message;

      logger.debug("Test disabled: {}", disabledReason);
      return ConditionEvaluationResult.disabled(disabledReason);
    } else {
      // enabled = false: test is SKIPPED when environment matches
      if (matches) {
        String message = annotation.message();
        String disabledReason = message.isEmpty()
            ? "Test is disabled in environment '" + currentEnv + "'"
            : message;

        logger.debug("Test disabled: {}", disabledReason);
        return ConditionEvaluationResult.disabled(disabledReason);
      }

      logger.debug("Test enabled: current environment '{}' is not in disabled list {}", currentEnv,
        Arrays.toString(envs));
      return ConditionEvaluationResult
          .enabled("Current environment '" + currentEnv + "' is not in the disabled list");
    }
  }

  /**
   * Retrieves the {@link ConditionalOnEnv} annotation from the test method or class. Method-level
   * annotations take precedence over class-level annotations.
   * @param context the extension context
   * @return the annotation, or null if not present
   */
  private ConditionalOnEnv getConditionalOnEnvAnnotation(ExtensionContext context) {
    Optional<ConditionalOnEnv> methodAnnotation = AnnotationUtils
        .findAnnotation(context.getTestMethod(), ConditionalOnEnv.class);
    if (methodAnnotation.isPresent()) {
      return methodAnnotation.get();
    }

    Optional<ConditionalOnEnv> classAnnotation = AnnotationUtils
        .findAnnotation(context.getRequiredTestClass(), ConditionalOnEnv.class);
    return classAnnotation.orElse(null);
  }
}
