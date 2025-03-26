package redis.clients.jedis;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import org.mockito.MockedConstruction;
import redis.clients.jedis.csc.TestCache;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockConstruction;

/**
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@SinceRedisVersion("6.0.0")
public class JedisPoolUnitTest {
    private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl");

    @Test
    public void compareACLToStringWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            JedisPool pool = new JedisPool(endpoint.getHost(), endpoint.getPort(), endpoint.getUsername(), endpoint.getPassword());

            pool.close();
        }
    }

    @Test
    public void compareACLToPoolConfigWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    String username = (String) context.arguments().get(4);
                    String password = (String) context.arguments().get(5);

                    assertEquals(endpoint.getUsername(), username);
                    assertEquals(endpoint.getPassword(), password);
                })) {

            JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), endpoint.getUsername(), endpoint.getPassword());

            pool.close();
        }
    }

    @Test
    public void compareACLToPoolConfigAndTimeoutWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    String username = (String) context.arguments().get(4);
                    String password = (String) context.arguments().get(5);

                    assertEquals(endpoint.getUsername(), username);
                    assertEquals(endpoint.getPassword(), password);
                })) {

            JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT, endpoint.getUsername(), endpoint.getPassword());

            pool.close();
        }
    }

    @Test
    public void compareACLToPoolConfigAndTimeoutSslWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    String username = (String) context.arguments().get(5);
                    String password = (String) context.arguments().get(6);

                    assertEquals(endpoint.getUsername(), username);
                    assertEquals(endpoint.getPassword(), password);
                })) {

            JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT, endpoint.getUsername(), endpoint.getPassword(), false);

            pool.close();
        }
    }



    @Test
    public void compareACLWithConfigForJedisPooled() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            JedisPooled jedisPooled = new JedisPooled(endpoint.getHost(), endpoint.getPort(), endpoint.getUsername(), endpoint.getPassword());

            jedisPooled.close();
        }
    }

    @Test
    public void compareACLToConnectionConfigWithConfigForJedisPooled() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            JedisPooled jedisPooled = new JedisPooled(new ConnectionPoolConfig(), endpoint.getHost(), endpoint.getPort(), endpoint.getUsername(), endpoint.getPassword());

            jedisPooled.close();
        }
    }

    @Test
    public void compareACLToConnectionConfigWithConfigAndTimeoutForJedisPooled() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            JedisPooled jedisPooled = new JedisPooled(new ConnectionPoolConfig(), endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT, endpoint.getUsername(), endpoint.getPassword());

            jedisPooled.close();
        }
    }

    @Test
    public void compareACLToConnectionConfigAndTimeoutSslWithConfigForJedisPooled() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            JedisPooled jedisPooled = new JedisPooled(new ConnectionPoolConfig(), endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT, endpoint.getUsername(), endpoint.getPassword(), false);

            jedisPooled.close();
        }
    }

    @Test
    public void compareACLWithConfigForUnifiedJedis() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            DefaultJedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
                    .user(endpoint.getUsername())
                    .password(endpoint.getPassword()).build();

            UnifiedJedis unifiedJedis = new UnifiedJedis(endpoint.getHostAndPort(), jedisClientConfig);

            unifiedJedis.close();
        }
    }

    @Test
    public void compareACLToURIWithConfigForUnifiedJedis() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            UnifiedJedis unifiedJedis = new UnifiedJedis(URI.create("redis://" + endpoint.getUsername() + ":" + endpoint.getPassword() + "@" + endpoint.getHost() + ":" + endpoint.getPort()));

            unifiedJedis.close();
        }
    }

    @Test
    public void compareACLToURIAndConfigWithConfigForUnifiedJedis() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            UnifiedJedis unifiedJedis = new UnifiedJedis(URI.create("redis://" + endpoint.getUsername() + ":" + endpoint.getPassword() + "@" + endpoint.getHost() + ":" + endpoint.getPort()),
                    DefaultJedisClientConfig.builder().build());

            unifiedJedis.close();
        }
    }

    @Test
    public void compareACLToCacheAndConfigWithConfigForUnifiedJedis() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            UnifiedJedis unifiedJedis = new UnifiedJedis(endpoint.getHostAndPort(),
                    DefaultJedisClientConfig.builder().user(endpoint.getUsername()).password(endpoint.getPassword()).protocol(RedisProtocol.RESP3).build(),
                    new TestCache());

            unifiedJedis.close();
        }
    }
}
