package redis.clients.jedis.prefix;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.util.prefix.CommandObjectsWithPrefixedKeys;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.PooledConnectionProvider;

public class JedisPooledPrefixedKeysTest extends PrefixedKeysTest {
    private final HostAndPort hostAndPort = HostAndPorts.getRedisServers().get(7);

    @Override
    UnifiedJedis prefixingJedis() {
        PooledConnectionProvider connectionProvider = new PooledConnectionProvider(hostAndPort);
        return new JedisPooled(connectionProvider, new CommandObjectsWithPrefixedKeys("test-prefix:"), RedisProtocol.RESP3);
    }

    @Override
    UnifiedJedis nonPrefixingJedis() {
        PooledConnectionProvider connectionProvider = new PooledConnectionProvider(hostAndPort);
        return new JedisPooled(connectionProvider, new CommandObjects(), RedisProtocol.RESP3);
    }
}
