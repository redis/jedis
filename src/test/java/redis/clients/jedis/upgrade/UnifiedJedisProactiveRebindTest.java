package redis.clients.jedis.upgrade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Test that UnifiedJedis proactively rebinds to new target when receiving MOVING notifications.
 * Uses mock TCP servers to simulate Redis cluster slot migration scenarios.
 */
@Tag("upgrade")
public class UnifiedJedisProactiveRebindTest {

    private TcpMockServer mockServer1;
    private TcpMockServer mockServer2;
    private JedisPooled unifiedJedis;
    private final int socketTimeoutMs = 5000;

    @BeforeEach
    public void setUp() throws IOException {
        // Start tcpmockedserver1
        mockServer1 = new TcpMockServer();
        mockServer1.start();
        
        // Start tcpmockedserver2
        mockServer2 = new TcpMockServer();
        mockServer2.start();

        System.out.println("MockServer1 started on port: " + mockServer1.getPort());
        System.out.println("MockServer2 started on port: " + mockServer2.getPort());
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (unifiedJedis != null) {
            unifiedJedis.close();
        }
        if (mockServer1 != null) {
            mockServer1.stop();
        }
        if (mockServer2 != null) {
            mockServer2.stop();
        }
    }

    @Test
    public void testProactiveRebindOnMovingNotification() throws Exception {
        // 1. Create UnifiedJedis client and connect it to mockedserver1
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .socketTimeoutMillis(socketTimeoutMs)
            .protocol(RedisProtocol.RESP3)
            .proactiveRebindEnabled(true) // Enable proactive rebinding
            .build();

        HostAndPort server1Address = new HostAndPort("localhost", mockServer1.getPort());
        ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
        connectionPoolConfig.setMaxTotal(1);
        connectionPoolConfig.setMinIdle(1);
        unifiedJedis = new JedisPooled(connectionPoolConfig, server1Address, clientConfig);

        // 1. Perform a PING command to initiate a connection
        String response1 = unifiedJedis.ping();
        assertEquals("PONG", response1);

        // Verify initial connection to server1
        assertEquals(1, mockServer1.getConnectedClientCount());
        assertEquals(0, mockServer2.getConnectedClientCount());

        // 2. Send MOVING notification to mockedserver2
        // MOVING format: ['MOVING', slot, 'host:port']
        String server2Address = "localhost:" + mockServer2.getPort();
        mockServer1.sendPushMessageToAll("MOVING", "30", server2Address);

        // 3. Perform PING command
        // This should trigger read of the MOVING notification and rebind to server2
        // the ping command itself should be executed against server1
        // the used connection should be closed after the ping command is executed
        String response2 = unifiedJedis.ping();
        assertEquals("PONG", response2);

        // drop connection to server1
        mockServer1.stop();

        // Verify initial connection to server1
        assertEquals(0, mockServer1.getConnectedClientCount());
        assertEquals(0, mockServer2.getConnectedClientCount());

        // 4. Perform PING command
        // Folowup ping command should be executed against server2

        String response3 = unifiedJedis.ping();
        assertEquals("PONG", response3);

        // Verify that connection has moved to server2
        assertEquals(0, mockServer1.getConnectedClientCount());
        assertEquals(1, mockServer2.getConnectedClientCount());
    }

}
