package io.redis.test.annotations;

import java.lang.annotation.*;

/**
 * Annotation to conditionally disable tests based on the test environment.
 * <p>
 * Use this annotation to skip tests that should NOT run in specific environments (e.g., Docker
 * container environment or Redis Enterprise environment).
 * <p>
 * This is the inverse of {@link EnabledOnEnv} - tests annotated with this will be disabled when
 * running in the specified environment(s).
 * <p>
 * The annotation can be applied at both method and class level. Method-level annotations take
 * precedence over class-level annotations.
 * <p>
 * Example usage:
 * 
 * <pre>
 * &#64;SkipOnEnv("docker")
 * public void testNotInDocker() {
 *   // This test is skipped in Docker environment
 * }
 *
 * &#64;SkipOnEnv("re")
 * public void testNotInRedisEnterprise() {
 *   // This test is skipped in Redis Enterprise environment
 * }
 * </pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface SkipOnEnv {
  /**
   * The environment(s) in which the test should be disabled. Valid values are defined in
   * {@code TestEnvUtil} (e.g., "docker", "re").
   * @return array of environment identifiers
   */
  String[] value();

  /**
   * Optional message to display when the test is disabled.
   * @return the reason message
   */
  String message() default "";
}
