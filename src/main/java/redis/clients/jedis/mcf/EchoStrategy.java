package redis.clients.jedis.mcf;

import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;

public class EchoStrategy implements HealthCheckStrategy {

    private int interval;
    private int timeout;
    private UnifiedJedis jedis;

    public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
        this(hostAndPort, jedisClientConfig, 1000, 1000);
    }

    public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig, int interval, int timeout) {
        this.interval = interval;
        this.timeout = timeout;
        ConnectionFactory connFactory = new ConnectionFactory(hostAndPort, jedisClientConfig);
        try {
            this.jedis = new UnifiedJedis(connFactory.makeObject().getObject());
        } catch (Exception e) {
            throw new JedisConnectionException("HealthCheck connection Failed!", e);
        }
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
    public HealthStatus doHealthCheck(Endpoint endpoint) {
        return "HealthCheck".equals(jedis.echo("HealthCheck")) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
    }

    public static final StrategySupplier DEFAULT = (hostAndPort, jedisClientConfig) -> {
        return new EchoStrategy(hostAndPort, jedisClientConfig);
    };

}
