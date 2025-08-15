package redis.clients.jedis.mcf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;
import redis.clients.jedis.UnifiedJedis;

public class EchoStrategy implements HealthCheckStrategy {
    private static final Logger log = LoggerFactory.getLogger(EchoStrategy.class);

    public static class Config extends HealthCheckStrategy.Config {
        protected final HostAndPort hostAndPort;
        protected final JedisClientConfig jedisClientConfig;

        public Config(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
            this(hostAndPort, jedisClientConfig, 1000, 1000, 3);
        }

        public Config(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig, int interval, int timeout,
            int minConsecutiveSuccessCount) {
            super(interval, timeout, minConsecutiveSuccessCount);

            this.hostAndPort = hostAndPort;
            this.jedisClientConfig = jedisClientConfig;
        }
    }

    private int interval;
    private int timeout;
    private UnifiedJedis jedis;
    private int minConsecutiveSuccessCount;

    public EchoStrategy(Config config) {
        this.interval = config.interval;
        this.timeout = config.timeout;
        this.minConsecutiveSuccessCount = config.minConsecutiveSuccessCount;
        this.jedis = new UnifiedJedis(config.hostAndPort, config.jedisClientConfig);
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

    public static final StrategySupplier<Config> DEFAULT = (Config config) -> {
        return new EchoStrategy(config);
    };
}
