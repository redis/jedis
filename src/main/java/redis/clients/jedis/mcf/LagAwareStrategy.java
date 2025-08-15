package redis.clients.jedis.mcf;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;
import redis.clients.jedis.RedisCredentials;

public class LagAwareStrategy implements HealthCheckStrategy {

    private static Logger log = LoggerFactory.getLogger(LagAwareStrategy.class);

    private int interval;
    private int timeout;
    private int minConsecutiveSuccessCount;
    private RedisRestAPI redisRestAPI;
    private String bdbId;

    public LagAwareStrategy(HostAndPort hostAndPort, Supplier<RedisCredentials> credentialsSupplier) {
        this(hostAndPort, credentialsSupplier, 1000, 1000, 3);
    }

    public LagAwareStrategy(Endpoint restEndpoint, Supplier<RedisCredentials> credentialsSupplier,
        int healthCheckInterval, int healthCheckTimeout, int minConsecutiveSuccessCount) {
        this.interval = healthCheckInterval;
        this.timeout = healthCheckTimeout;
        this.minConsecutiveSuccessCount = minConsecutiveSuccessCount;
        this.redisRestAPI = new RedisRestAPI(restEndpoint, credentialsSupplier, healthCheckTimeout);
    }

    @Override
    public int getInterval() {
        return interval;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public int minConsecutiveSuccessCount() {
        return minConsecutiveSuccessCount;
    }

    @Override
    public HealthStatus doHealthCheck(Endpoint endpoint) {
        try {
            String bdb = bdbId;
            if (bdb == null) {
                List<String> bdbs = redisRestAPI.getBdbs();
                if (bdbs.size() > 0) {
                    bdb = bdbs.get(0);
                    bdbId = bdb;
                }
            }
            if (bdb == null) {
                log.warn("No available database found for health check for endpoint %s", endpoint);
                return HealthStatus.UNHEALTHY;
            }
            if (redisRestAPI.checkBdbAvailability(bdb, true)) {
                return HealthStatus.HEALTHY;
            }
        } catch (Exception e) {
            log.error("Error while checking database availability", e);
            bdbId = null;
        }
        return HealthStatus.UNHEALTHY;
    }

    public static StrategySupplier getDefaultSupplier(Map<Endpoint, LagAwareStrategy> lagAwareStrategies) {
        return (hostAndPort, jedisClientConfig) -> {
            return lagAwareStrategies.get(hostAndPort);
        };
    }
}
