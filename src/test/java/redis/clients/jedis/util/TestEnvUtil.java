package redis.clients.jedis.util;

import java.util.Optional;

public class TestEnvUtil {
    private static final String TEST_ENV_PROVIDER = System.getenv().getOrDefault("TEST_ENV_PROVIDER", "docker");
    private static final String TESTMODULE_SO = Optional.ofNullable(System.getenv("TESTMODULE_SO"))
            .orElseGet(() -> isContainerEnv()
                    ? "/redis/work/modules/testmodule.so"
                    : "/tmp/testmodule.so");

    public static String testModuleSo() {
        return TESTMODULE_SO;
    }

    public static boolean isContainerEnv() {
        return TEST_ENV_PROVIDER.equals("docker");
    }
}
