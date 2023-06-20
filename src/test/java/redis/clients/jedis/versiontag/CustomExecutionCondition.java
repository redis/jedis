package redis.clients.jedis.versiontag;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.System.getenv;

public class CustomExecutionCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<EnabledOnRedis> optional = AnnotationUtils.findAnnotation(context.getElement(),
                EnabledOnRedis.class);

        if (optional.isPresent()) {
            List<RedisType> typeList = Arrays.asList(optional.get().value());
            String type = System.getProperty("redisversion");
            if (type == null || type.isEmpty()) type = "REDIS_UNSTABLE";

            RedisType Redis_type = null;
            try {
                Redis_type = Enum.valueOf(RedisType.class, type.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                System.out.println("Supported engines are: ");
                for (RedisType et : RedisType.values()) {
                    System.out.println(et.name());
                }
                System.exit(1);
            }

            if (typeList.contains(Redis_type)) {
                return ConditionEvaluationResult.enabled("Test is enabled for engine " + Redis_type.name());
            } else {
                return ConditionEvaluationResult.disabled("Test is disabled for engine " + Redis_type.name());
            }
        }
        return ConditionEvaluationResult.enabled("@EnabledOnEngine is not present");
    }
}
