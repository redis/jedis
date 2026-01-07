package io.redis.test.annotations;

import java.lang.annotation.*;

/**
 * Annotation to conditionally enable or disable tests based on the test environment.
 * <p>
 * This unified annotation replaces both {@code @EnabledOnEnv} and {@code @SkipOnEnv} annotations,
 * providing a single way to control test execution based on environment.
 * <p>
 * The annotation can be applied at both method and class level. Method-level annotations take
 * precedence over class-level annotations.
 * <p>
 * Example usage:
 * 
 * <pre>
 * // Enable test only in a specific environment
 * &#64;ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_DOCKER, enabled = true)
 * public void testOnlyInDocker() {
 *   // This test only runs in Docker environment
 * }
 *
 * // Skip test in a specific environment
 * &#64;ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
 * public void testNotInRedisEnterprise() {
 *   // This test is skipped in Redis Enterprise environment
 * }
 *
 * // Enable test in multiple environments
 * &#64;ConditionalOnEnv(value = {TestEnvUtil.ENV_OSS_DOCKER, TestEnvUtil.ENV_OSS_SOURCE}, enabled = true)
 * public void testInDockerOrSource() {
 *   // This test runs in Docker or Source environments
 * }
 * </pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ConditionalOnEnv {
  /**
   * The environment(s) to match against. Valid values are defined in
   * {@code TestEnvUtil} (e.g., "oss-docker", "oss-source", "re").
   * @return array of environment identifiers
   */
  String[] value();

  /**
   * Whether the test should be enabled or disabled when the current environment
   * matches one of the specified values.
   * <p>
   * When {@code enabled = true}: the test runs ONLY when the current environment
   * matches one of the specified values.
   * <p>
   * When {@code enabled = false}: the test is SKIPPED when the current environment
   * matches one of the specified values.
   * 
   * @return true to enable the test in matching environments, false to disable
   */
  boolean enabled();

  /**
   * Optional message to display when the test is disabled.
   * @return the reason message
   */
  String message() default "";
}

