package redis.clients.jedis.util;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisInfo;
import io.redis.test.utils.RedisVersion;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.lang.reflect.Method;

import static redis.clients.jedis.util.RedisVersionUtil.forcedVersion;

public class RedisVersionRule implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(RedisVersionRule.class);

    private final HostAndPort hostPort;
    private final JedisClientConfig config;


    public RedisVersionRule(EndpointConfig endpoint) {
        this.hostPort = endpoint.getHostAndPort();
        this.config = endpoint.getClientConfigBuilder().build();
    }

    public RedisVersionRule(HostAndPort hostPort, JedisClientConfig config) {
        this.hostPort = hostPort;
        this.config = config;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try ( Jedis jedisClient = new Jedis(hostPort, config)) {
                    SinceRedisVersion descriptionVersionAnnotation = description.getAnnotation(SinceRedisVersion.class);
                    if (descriptionVersionAnnotation != null) {
                        checkRedisVersion(jedisClient, descriptionVersionAnnotation);
                    }

                    SinceRedisVersion classVersionAnnotation = description.getTestClass().getAnnotation(SinceRedisVersion.class);
                    if (classVersionAnnotation != null) {
                        checkRedisVersion(jedisClient, classVersionAnnotation);
                    }

                    SinceRedisVersion methodVersionAnnotation = getMethodAnnotation(description);
                    if (methodVersionAnnotation != null) {
                        checkRedisVersion(jedisClient, methodVersionAnnotation);
                    }

                    // Return the base statement to execute the test
                    base.evaluate();
                }
            }
            private void checkRedisVersion(Jedis jedisClient, SinceRedisVersion versionAnnotation) {

                // Check if the environment variable is set
                RedisVersion currentVersion;

                if (forcedVersion != null) {
                    logger.info("Using forced Redis server version from environment variable: " + forcedVersion);
                    currentVersion = forcedVersion;
                } else {
                    RedisInfo info = RedisInfo.parseInfoServer(jedisClient.info("server"));
                    currentVersion = RedisVersion.of(info.getRedisVersion());
                }

                RedisVersion minRequiredVersion = RedisVersion.of(versionAnnotation.value());
                if (currentVersion.isLessThan(minRequiredVersion)) {
                    Assume.assumeTrue("Test requires Redis version " + minRequiredVersion + " or later, but found " + currentVersion, false);
                }
            }

            private SinceRedisVersion getMethodAnnotation(Description description) {
                try {
                    // description.getAnnotation() does not return any method level annotation when used
                    // with parametrised tests
                    String methodName = description.getMethodName();
                    if (methodName != null) {
                        Class<?> testClass = description.getTestClass();
                        if (testClass != null) {
                            for (Method method : testClass.getDeclaredMethods()) {
                                if (method.getName().equals(methodName)) {
                                    return method.getAnnotation(SinceRedisVersion.class);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Could not resolve EnabledOnCommand annotation", e);
                }
                return null;
            }
        };
    }
}
