package redis.clients.jedis.annots;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes for experimental development.
 * <p>
 * Classes with this annotation may be renamed, changed or even removed in a future version. This
 * annotation doesn't mean that the implementation has an 'experimental' quality.
 * <p>
 * If a type is marked with this annotation, all its members are considered experimental.
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface Experimental { }
