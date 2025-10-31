package redis.clients.jedis.util;

import java.util.Optional;

public class TestEnvUtil {
    // Redis servers running inside docker
    public static final String ENV_DOCKER = "docker";

    private static final String TEST_ENV_PROVIDER = System.getenv().getOrDefault("TEST_ENV_PROVIDER", ENV_DOCKER);

    private static final String TESTMODULE_SO_PATH = Optional.ofNullable(System.getenv("TESTMODULE_SO"))
            .orElseGet(() -> isContainerEnv()
                    ? "/redis/work/modules/testmodule.so"
                    : "/tmp/testmodule.so");

    public static boolean isContainerEnv() {
        return TEST_ENV_PROVIDER.equals(ENV_DOCKER);
    }

    public static String testModuleSoPath() {
        return TESTMODULE_SO_PATH;
    }
}
