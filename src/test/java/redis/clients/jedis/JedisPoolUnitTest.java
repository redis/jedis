package redis.clients.jedis;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import org.mockito.MockedConstruction;
import redis.clients.jedis.csc.TestCache;
import redis.clients.jedis.util.JedisURIHelper;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockConstruction;

/**
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@SinceRedisVersion("6.0.0")
public class JedisPoolUnitTest {
    private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl");

    /**
     * to verify that the data(username/password) is correctly passed to the JedisFactory from the (JedisPool) constructor.
     */
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
    public void compareACLToURIWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    URI uri = (URI) context.arguments().get(0);

                    String username = JedisURIHelper.getUser(uri);
                    String password = JedisURIHelper.getPassword(uri);

                    assertEquals(endpoint.getUsername(), username);
                    assertEquals(endpoint.getPassword(), password);
                })) {

            JedisPool pool = new JedisPool(endpoint.getURIBuilder().defaultCredentials().build());

            pool.close();
        }
    }

    @Test
    public void compareACLToClientConfigWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig config = (DefaultJedisClientConfig) context.arguments().get(1);

                    String username = config.getUser();
                    String password = config.getPassword();

                    assertEquals(endpoint.getUsername(), username);
                    assertEquals(endpoint.getPassword(), password);
                })) {

            JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());

            pool.close();
        }
    }

    @Test
    public void compareACLToClientConfigAndSocketFactoryWithConfig() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig config = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), config.getUser());
                    assertEquals(endpoint.getPassword(), config.getPassword());
                })) {

            JedisPool pool = new JedisPool(new JedisPoolConfig(), new DefaultJedisSocketFactory(), endpoint.getClientConfigBuilder().build());

            pool.close();
        }
    }

    /**
     * to verify that the data(username/password) is correctly passed to the ConnectionFactory from the (JedisPooled) constructor.
     */
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
    public void compareACLToURIWithConfigForJedisPooled() {
        try (MockedConstruction<JedisFactory> ignored = mockConstruction(JedisFactory.class,
                (mock, context) -> {
                    URI uri = (URI) context.arguments().get(0);

                    String username = JedisURIHelper.getUser(uri);
                    String password = JedisURIHelper.getPassword(uri);

                    assertEquals(endpoint.getUsername(), username);
                    assertEquals(endpoint.getPassword(), password);
                })) {

            JedisPooled pool = new JedisPooled(new ConnectionPoolConfig(), endpoint.getURIBuilder().defaultCredentials().build().toString());

            pool.close();
        }
    }


    /**
     * to verify that the data(username/password) is correctly passed to the ConnectionFactory from the (UnifiedJedis) constructor.
     */
    @Test
    public void compareACLWithConfigForUnifiedJedis() {
        try (MockedConstruction<ConnectionFactory> ignored = mockConstruction(ConnectionFactory.class,
                (mock, context) -> {
                    DefaultJedisClientConfig jedisClientConfig = (DefaultJedisClientConfig) context.arguments().get(1);

                    assertEquals(endpoint.getUsername(), jedisClientConfig.getUser());
                    assertEquals(endpoint.getPassword(), jedisClientConfig.getPassword());
                })) {

            UnifiedJedis unifiedJedis = new UnifiedJedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());

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

            UnifiedJedis unifiedJedis = new UnifiedJedis(endpoint.getURIBuilder().defaultCredentials().build());

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

            UnifiedJedis unifiedJedis = new UnifiedJedis(endpoint.getURIBuilder().defaultCredentials().build(),
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
