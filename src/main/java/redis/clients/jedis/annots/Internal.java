package redis.clients.jedis.annots;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes or methods as an internal development API. It indicates that the
 * annotated element must not be considered as a public API.
 * <p>
 * Classes or methods with this annotation may change across releases.
 * <p>
 * If a type is marked with this annotation, all its members are considered internal.
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
public @interface Internal {
}