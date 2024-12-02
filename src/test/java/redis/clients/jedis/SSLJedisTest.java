package redis.clients.jedis;

import static redis.clients.jedis.util.TlsUtil.envTruststore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import redis.clients.jedis.util.TlsUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class SSLJedisTest {
    static Logger log = LoggerFactory.getLogger(SSLJedisTest.class);
    protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

    @BeforeClass
    public static void prepare() {
        Path trusStorePath = TlsUtil.createAndSaveEnvTruststore("redis1-2-5-8-sentinel", "changeit");
        TlsUtil.setCustomTrustStore(trusStorePath, "changeit");
    }

    @AfterClass
    public static void teardownTrustStore() {
        TlsUtil.restoreOriginalTrustStore();
    }

    @Test
    public void connectWithSsl() {
        try (Jedis jedis = new Jedis(endpoint.getHost(), endpoint.getPort(), true)) {
            jedis.auth(endpoint.getPassword());
            assertEquals("PONG", jedis.ping());
        }
    }

    @Test
    public void connectWithConfig() {
        try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
                DefaultJedisClientConfig.builder().ssl(true).build())) {
            jedis.auth(endpoint.getPassword());
            assertEquals("PONG", jedis.ping());
        }
    }

    @Test
    public void connectWithConfigInterface() {
        try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
                new JedisClientConfig() {
                    @Override
                    public boolean isSsl() {
                        return true;
                    }
                })) {
            jedis.auth(endpoint.getPassword());
            assertEquals("PONG", jedis.ping());
        }
    }

    /**
     * Tests opening a default SSL/TLS connection to redis using "rediss://" scheme url.
     */
    @Test
    public void connectWithUrl() {
        // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
        try (Jedis jedis = new Jedis(endpoint.getURI().toString())) {
            jedis.auth(endpoint.getPassword());
            assertEquals("PONG", jedis.ping());
        }
    }

    /**
     * Tests opening a default SSL/TLS connection to redis.
     */
    @Test
    public void connectWithUri() {
        // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
        try (Jedis jedis = new Jedis(endpoint.getURI())) {
            jedis.auth(endpoint.getPassword());
            assertEquals("PONG", jedis.ping());
        }
    }

}
