package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JedisSentinelSlavePoolTest {

    private static final String MASTER_NAME = "mymaster";

    protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
    protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(2);

    protected final Set<String> sentinels = new HashSet<>();

    private final String password = "foobared";

    @BeforeEach
    public void setUp() throws Exception {
        sentinels.clear();

        sentinels.add(sentinel1.toString());
        sentinels.add(sentinel2.toString());
    }

    @Test
    public void repeatedSentinelPoolInitialization() {

        for (int i = 0; i < 20; ++i) {
            GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

            JedisSentinelSlavePool pool = new JedisSentinelSlavePool(MASTER_NAME, sentinels, config, 1000,
                    password, 2);
            pool.getResource().close();
            pool.destroy();
        }
    }

    @Test
    public void initializeWithNotAvailableSentinelsShouldThrowException() {
        Set<String> wrongSentinels = new HashSet<String>();
        wrongSentinels.add(new HostAndPort("localhost", 65432).toString());
        wrongSentinels.add(new HostAndPort("localhost", 65431).toString());

        assertThrows(JedisConnectionException.class,
                () -> new JedisSentinelSlavePool(MASTER_NAME, wrongSentinels).close());
    }

    @Test
    public void initializeWithNotMonitoredMasterNameShouldThrowException() {
        final String wrongMasterName = "wrongMasterName";
        assertThrows(JedisException.class, () -> new JedisSentinelSlavePool(wrongMasterName, sentinels).close());
    }

    @Test
    public void checkJedisIsSlave() {
        GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(1);
        config.setBlockWhenExhausted(false);
        JedisSentinelSlavePool pool = new JedisSentinelSlavePool(MASTER_NAME, sentinels, config, 1000,
                password, 2);

        Jedis jedis = pool.getResource();
        assertThrows(JedisDataException.class, () -> jedis.set("hello", "jedis"));
    }

    @Test
    public void customClientName() {
        GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(1);
        config.setBlockWhenExhausted(false);
        JedisSentinelSlavePool pool = new JedisSentinelSlavePool(MASTER_NAME, sentinels, config, 1000,
                password, 0, "my_shiny_client_name");

        Jedis jedis = pool.getResource();

        try {
            assertEquals("my_shiny_client_name", jedis.clientGetname());
        } finally {
            jedis.close();
            pool.destroy();
        }

    }
}
