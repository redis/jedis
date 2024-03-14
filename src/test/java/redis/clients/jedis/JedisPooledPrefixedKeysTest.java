package redis.clients.jedis;

import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.providers.PooledConnectionProvider;

public class JedisPooledPrefixedKeysTest extends PrefixedKeysTest {
    private final HostAndPort hostAndPort = HostAndPorts.getRedisServers().get(7);

    @Override
    UnifiedJedis prefixingJedis() {
        PooledConnectionProvider connectionProvider = new PooledConnectionProvider(hostAndPort);
        return new JedisPooled(new DefaultCommandExecutor(connectionProvider), connectionProvider, new CommandObjectsWithPrefixedKeys("test-prefix:"), RedisProtocol.RESP3);
    }

    @Override
    UnifiedJedis nonPrefixingJedis() {
        PooledConnectionProvider connectionProvider = new PooledConnectionProvider(hostAndPort);
        return new JedisPooled(new DefaultCommandExecutor(connectionProvider), connectionProvider, new CommandObjects(), RedisProtocol.RESP3);
    }
}
