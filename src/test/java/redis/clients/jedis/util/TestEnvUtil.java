package redis.clients.jedis.util;

import java.util.Optional;

public class TestEnvUtil {
    // Redis servers running inside docker
    public static final String ENV_OSS_DOCKER = "oss-docker";

    public static final String ENV_OSS_SOURCE = "oss-source";

    public static final String ENV_REDIS_ENTERPRISE = "re";

    private static final String TEST_ENV_PROVIDER = System.getenv().getOrDefault("TEST_ENV_PROVIDER",
        ENV_OSS_DOCKER);

    private static final String TESTMODULE_SO_PATH = Optional.ofNullable(System.getenv("TESTMODULE_SO"))
            .orElseGet(() -> isContainerEnv()
                    ? "/redis/work/modules/testmodule.so"
                    : "/tmp/testmodule.so");

    private static final String ENDPOINTS_CONFIG_PATH = Optional.ofNullable(System.getenv("REDIS_ENDPOINTS_CONFIG_PATH"))
      .orElseGet(() -> TEST_ENV_PROVIDER.equals(ENV_OSS_SOURCE)
          ? "src/test/resources/endpoints_source.json"
          : "src/test/resources/endpoints.json");

    public static boolean isContainerEnv() {
        return TEST_ENV_PROVIDER.equals(ENV_OSS_DOCKER);
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
