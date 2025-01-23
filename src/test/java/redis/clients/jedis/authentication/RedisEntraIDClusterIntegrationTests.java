package redis.clients.jedis.authentication;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;

public class RedisEntraIDClusterIntegrationTests {
    private static final Logger log = LoggerFactory
            .getLogger(RedisEntraIDClusterIntegrationTests.class);

    private static EntraIDTestContext testCtx;
    private static EndpointConfig endpointConfig;
    private static HostAndPort hnp;

    @BeforeClass
    public static void before() {
        try {
            testCtx = EntraIDTestContext.DEFAULT;
            endpointConfig = HostAndPorts.getRedisEndpoint("cluster-entraid-acl");
            hnp = endpointConfig.getHostAndPort();
        } catch (IllegalArgumentException e) {
            log.warn("Skipping test because no Redis endpoint is configured");
            org.junit.Assume.assumeTrue(false);
        }
    }

    @Test
    public void testClusterInitWithAuthXManager() {
        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
                .lowerRefreshBoundMillis(1000).clientId(testCtx.getClientId())
                .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority())
                .scopes(testCtx.getRedisScopes()).build();

        int defaultDirections = 5;
        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .authXManager(new AuthXManager(tokenAuthConfig)).build();

        ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

        try (JedisCluster jc = new JedisCluster(hnp, config, defaultDirections,
                DEFAULT_POOL_CONFIG)) {

            assertEquals("OK", jc.set("foo", "bar"));
            assertEquals("bar", jc.get("foo"));
            assertEquals(1, jc.del("foo"));
        }
    }

    @Test
    public void testClusterWithReAuth() throws InterruptedException, ExecutionException {
        TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
                // 0.00002F is to make it fit into 2 seconds, we need at least 2 attempt in 2 seconds
                // to trigger re-authentication.
                // For expiration time between 30 minutes to 12 hours 
                // token renew will happen in from 36ms up to 864ms
                // If the received token has more than 12 hours to expire, this test will probably fail, and need to be adjusted.
                .expirationRefreshRatio(0.00002F).clientId(testCtx.getClientId())
                .secret(testCtx.getClientSecret()).authority(testCtx.getAuthority())
                .scopes(testCtx.getRedisScopes()).build();

        AuthXManager authXManager = new AuthXManager(tokenAuthConfig);

        authXManager = spy(authXManager);

        List<Connection> connections = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            Connection connection = spy((Connection) invocation.getArgument(0));
            invocation.getArguments()[0] = connection;
            connections.add(connection);
            Object result = invocation.callRealMethod();
            return result;
        }).when(authXManager).addConnection(any(Connection.class));

        JedisClientConfig config = DefaultJedisClientConfig.builder().authXManager(authXManager)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        try (JedisCluster jc = new JedisCluster(Collections.singleton(hnp), config)) {
            Runnable task = () -> {
                while (latch.getCount() > 0) {
                    assertEquals("OK", jc.set("foo", "bar"));
                }
            };
            Future task1 = executorService.submit(task);
            Future task2 = executorService.submit(task);

            await().pollInterval(ONE_HUNDRED_MILLISECONDS).atMost(TWO_SECONDS)
                    .until(connections::size, greaterThanOrEqualTo(2));

            connections.forEach(conn -> {
                await().pollInterval(ONE_HUNDRED_MILLISECONDS).atMost(TWO_SECONDS)
                        .untilAsserted(() -> verify(conn, atLeast(2)).reAuthenticate());
            });
            latch.countDown();
            task1.get();
            task2.get();
        } finally {
            latch.countDown();
            executorService.shutdown();
        }
    }
}