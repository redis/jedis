package io.redis.test.annotations;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SinceRedisVersion {
    String value();
    String message() default "";
}
