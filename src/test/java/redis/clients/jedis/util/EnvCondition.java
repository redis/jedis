package redis.clients.jedis.util;

import io.redis.test.annotations.SkipOnEnv;
import io.redis.test.annotations.EnabledOnEnv;
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
 * This condition works with the {@link EnabledOnEnv} and {@link SkipOnEnv} annotations to
 * conditionally execute tests depending on the current test environment (e.g., Docker, Redis
 * Enterprise).
 * <p>
 * The environment is determined using {@link TestEnvUtil}.
 * <p>
 * Example usage with JUnit 5:
 * 
 * <pre>
 * &#64;ExtendWith(EnvCondition.class)
 * &#64;EnabledOnEnv("docker")
 * public class DockerOnlyTest {
 *   // Tests in this class only run in Docker environment
 * }
 *
 * &#64;ExtendWith(EnvCondition.class)
 * &#64;SkipOnEnv("re")
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

    // Check SkipOnEnv first (takes precedence)
    SkipOnEnv disabledAnnotation = getDisabledAnnotation(context);
    if (disabledAnnotation != null) {
      return evaluateDisabledOnEnv(disabledAnnotation, currentEnv);
    }

    // Check EnabledOnEnv
    EnabledOnEnv enabledAnnotation = getEnabledAnnotation(context);
    if (enabledAnnotation != null) {
      return evaluateEnabledOnEnv(enabledAnnotation, currentEnv);
    }

    return ConditionEvaluationResult.enabled("No @EnabledOnEnv or @SkipOnEnv annotation found");
  }

  private ConditionEvaluationResult evaluateEnabledOnEnv(EnabledOnEnv annotation,
      String currentEnv) {
    String[] enabledEnvs = annotation.value();

    boolean isEnabled = Arrays.stream(enabledEnvs)
        .anyMatch(env -> env.equalsIgnoreCase(currentEnv));

    if (isEnabled) {
      logger.debug("Test enabled: current environment '{}' matches one of {}", currentEnv,
        Arrays.toString(enabledEnvs));
      return ConditionEvaluationResult
          .enabled("Current environment '" + currentEnv + "' is in the enabled list");
    }

    String message = annotation.message();
    String disabledReason = message.isEmpty()
        ? "Test requires environment " + Arrays.toString(enabledEnvs)
            + ", but current environment is '" + currentEnv + "'"
        : message;

    logger.debug("Test disabled: {}", disabledReason);
    return ConditionEvaluationResult.disabled(disabledReason);
  }

  private ConditionEvaluationResult evaluateDisabledOnEnv(SkipOnEnv annotation, String currentEnv) {
    String[] disabledEnvs = annotation.value();

    boolean isDisabled = Arrays.stream(disabledEnvs)
        .anyMatch(env -> env.equalsIgnoreCase(currentEnv));

    if (isDisabled) {
      String message = annotation.message();
      String disabledReason = message.isEmpty()
          ? "Test is disabled in environment '" + currentEnv + "'"
          : message;

      logger.debug("Test disabled: {}", disabledReason);
      return ConditionEvaluationResult.disabled(disabledReason);
    }

    logger.debug("Test enabled: current environment '{}' is not in disabled list {}", currentEnv,
      Arrays.toString(disabledEnvs));
    return ConditionEvaluationResult
        .enabled("Current environment '" + currentEnv + "' is not in the disabled list");
  }

  /**
   * Retrieves the {@link EnabledOnEnv} annotation from the test method or class. Method-level
   * annotations take precedence over class-level annotations.
   * @param context the extension context
   * @return the annotation, or null if not present
   */
  private EnabledOnEnv getEnabledAnnotation(ExtensionContext context) {
    Optional<EnabledOnEnv> methodAnnotation = AnnotationUtils
        .findAnnotation(context.getTestMethod(), EnabledOnEnv.class);
    if (methodAnnotation.isPresent()) {
      return methodAnnotation.get();
    }

    Optional<EnabledOnEnv> classAnnotation = AnnotationUtils
        .findAnnotation(context.getRequiredTestClass(), EnabledOnEnv.class);
    return classAnnotation.orElse(null);
  }

  /**
   * Retrieves the {@link SkipOnEnv} annotation from the test method or class. Method-level
   * annotations take precedence over class-level annotations.
   * @param context the extension context
   * @return the annotation, or null if not present
   */
  private SkipOnEnv getDisabledAnnotation(ExtensionContext context) {
    Optional<SkipOnEnv> methodAnnotation = AnnotationUtils.findAnnotation(context.getTestMethod(),
      SkipOnEnv.class);
    if (methodAnnotation.isPresent()) {
      return methodAnnotation.get();
    }

    Optional<SkipOnEnv> classAnnotation = AnnotationUtils
        .findAnnotation(context.getRequiredTestClass(), SkipOnEnv.class);
    return classAnnotation.orElse(null);
  }
}
