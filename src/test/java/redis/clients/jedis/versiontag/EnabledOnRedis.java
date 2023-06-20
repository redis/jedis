package redis.clients.jedis.versiontag;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ExtendWith(CustomExecutionCondition.class)
public @interface EnabledOnRedis {
    RedisType[] value();
}

