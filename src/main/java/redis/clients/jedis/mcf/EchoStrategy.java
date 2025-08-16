package redis.clients.jedis.mcf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;

public class EchoStrategy implements HealthCheckStrategy {
    private static final Logger log = LoggerFactory.getLogger(EchoStrategy.class);

    private int interval;
    private int timeout;
    private UnifiedJedis jedis;
    private int minConsecutiveSuccessCount;

    public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
        this(hostAndPort, jedisClientConfig, 1000, 1000, 3);
    }

    public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig, int interval, int timeout,
        int minConsecutiveSuccessCount) {
        this.interval = interval;
        this.timeout = timeout;
        this.minConsecutiveSuccessCount = minConsecutiveSuccessCount;
        this.jedis = new UnifiedJedis(hostAndPort, jedisClientConfig);
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
            return "HealthCheck".equals(jedis.echo("HealthCheck")) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
        } catch (Exception e) {
            log.error("Error while performing health check", e);
            return HealthStatus.UNHEALTHY;
        }
    }

    @Override
    public void close() {
        jedis.close();
    }

    public static final StrategySupplier DEFAULT = (hostAndPort, jedisClientConfig) -> {
        return new EchoStrategy(hostAndPort, jedisClientConfig);
    };

}
