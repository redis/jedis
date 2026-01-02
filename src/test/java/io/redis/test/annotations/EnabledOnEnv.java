package io.redis.test.annotations;

import java.lang.annotation.*;

/**
 * Annotation to conditionally enable tests based on the test environment.
 * <p>
 * Use this annotation to skip tests that should only run in specific environments (e.g., Docker
 * container environment or Redis Enterprise environment).
 * <p>
 * The annotation can be applied at both method and class level. Method-level annotations take
 * precedence over class-level annotations.
 * <p>
 * Example usage:
 * 
 * <pre>
 * &#64;EnabledOnEnv("docker")
 * public void testOnlyInDocker() {
 *   // This test only runs in Docker environment
 * }
 *
 * &#64;EnabledOnEnv("re")
 * public void testOnlyInRedisEnterprise() {
 *   // This test only runs in Redis Enterprise environment
 * }
 * </pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface EnabledOnEnv {
  /**
   * The environment(s) in which the test should be enabled. Valid values are defined in
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
