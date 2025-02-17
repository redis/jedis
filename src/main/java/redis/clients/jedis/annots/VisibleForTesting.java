package redis.clients.jedis.annots;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * A member or type annotated with {@link VisibleForTesting} declares that it is only visible for
 * testing purposes.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.TYPE })
public @interface VisibleForTesting {
}
