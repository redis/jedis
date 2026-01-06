package redis.clients.jedis.util;

import java.util.Optional;

public class TestEnvUtil {
    // Redis servers running inside docker
    public static final String ENV_DOCKER = "docker";

    public static final String ENV_LEGACY = "local";

    public static final String ENV_REDIS_ENTERPRISE = "re";

    private static final String TEST_ENV_PROVIDER = System.getenv().getOrDefault("TEST_ENV_PROVIDER", ENV_DOCKER);

    private static final String TESTMODULE_SO_PATH = Optional.ofNullable(System.getenv("TESTMODULE_SO"))
            .orElseGet(() -> isContainerEnv()
                    ? "/redis/work/modules/testmodule.so"
                    : "/tmp/testmodule.so");

    private static final String ENDPOINTS_CONFIG_PATH = Optional.ofNullable(System.getenv("REDIS_ENDPOINTS_CONFIG_PATH"))
      .orElseGet(() -> isContainerEnv()
          ? "src/test/resources/endpoints.json"
          : "src/test/resources/endpoints_local.json");

    public static boolean isContainerEnv() {
        return TEST_ENV_PROVIDER.equals(ENV_DOCKER);
    }

    public static String getTestEnvProvider() {
        return TEST_ENV_PROVIDER;
    }

    public static String testModuleSoPath() {
        return TESTMODULE_SO_PATH;
    }

    public static String getEndpointsConfigPath() {
        return ENDPOINTS_CONFIG_PATH;
    }
}
