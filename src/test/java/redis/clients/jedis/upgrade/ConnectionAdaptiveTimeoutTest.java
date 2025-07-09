package redis.clients.jedis.upgrade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceEventHandler;
import redis.clients.jedis.MaintenanceEventHandlerImpl;
import redis.clients.jedis.MaintenanceEventListener;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.TimeoutOptions;
import redis.clients.jedis.util.ReflectionTestUtils;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Test that connection adaptive timeout works as expected.
 * Usses a mock TCP server to send Maintenance push messages to the client in controllable manner.
 */
@Tag("upgrade")
public class ConnectionAdaptiveTimeoutTest {

    private TcpMockServer mockServer;
    private Connection connection;
    private final int originalTimeoutMs = 2000;
    private final Duration relaxedTimeout = Duration.ofSeconds(10);

    @BeforeEach
    public void setUp() throws IOException {
        // Start the mock TCP server
        mockServer = new TcpMockServer();
        mockServer.start();
        
        // Create client configuration with relaxed timeout and maintenance event handler
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
            .proactiveTimeoutsRelaxing(relaxedTimeout)
            .build();
            
        MaintenanceEventHandler maintenanceEventHandler = new MaintenanceEventHandlerImpl();

        MaintenanceEventListener testListener = new MaintenanceEventListener() {
            @Override
            public void onMigrating() {
                System.out.println("MIGRATING");
            }
        };
        maintenanceEventHandler.addListener(testListener);

        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .socketTimeoutMillis(originalTimeoutMs)
            .timeoutOptions(timeoutOptions)
            .maintenanceEventHandler(maintenanceEventHandler)
            .protocol(RedisProtocol.RESP3)
            .build();

        // Create connection to the mock server
        HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
        connection = new Connection(hostAndPort, clientConfig);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (connection != null && connection.isConnected()) {
            connection.close();
        }
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    public void testMigratingPushMessage() throws SocketException {
        Socket socket = ReflectionTestUtils.getField(connection, "socket");

        assertTrue(connection.isConnected());
        assertEquals(originalTimeoutMs, connection.getSoTimeout());
        assertEquals(originalTimeoutMs, socket.getSoTimeout());

        // First send MIGRATING to activate relaxed timeout
        mockServer.sendMigratingPushToAll();
        assertTrue(connection.ping());
        assertTrue(connection.isRelaxedTimeoutActive());
        assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

        mockServer.sendMigratedPushToAll();
        assertTrue(connection.ping());
        assertFalse(connection.isRelaxedTimeoutActive());
        assertEquals(originalTimeoutMs, socket.getSoTimeout());
    }

    @Test
    public void testFailoverPushMessage() throws SocketException {
        Socket socket = ReflectionTestUtils.getField(connection, "socket");

        assertTrue(connection.isConnected());
        assertEquals(originalTimeoutMs, connection.getSoTimeout());
        assertEquals(originalTimeoutMs, socket.getSoTimeout());

        // First send MIGRATING to activate relaxed timeout
        mockServer.sendFailingOverPushToAll();
        assertTrue(connection.ping());
        assertTrue(connection.isRelaxedTimeoutActive());
        assertEquals(relaxedTimeout.toMillis(), socket.getSoTimeout());

        mockServer.sendFailedOverPushToAll();
        assertTrue(connection.ping());
        assertFalse(connection.isRelaxedTimeoutActive());
        assertEquals(originalTimeoutMs, socket.getSoTimeout());
    }

    @Test
    public void testDisabledTimeoutRelaxationDoesNotApplyRelaxedTimeout() throws Exception {
        // Create a connection with disabled timeout relaxation
        Connection disabledConnection = createConnectionWithDisabledTimeoutRelaxation();
        Socket disabledSocket = ReflectionTestUtils.getField(disabledConnection, "socket");

        try {
            assertTrue(disabledConnection.isConnected());
            assertEquals(originalTimeoutMs, disabledConnection.getSoTimeout());
            assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

            // Verify that relaxed timeout is disabled
            assertFalse(disabledConnection.isRelaxedTimeoutActive());

            // Send MIGRATING push message - should NOT activate relaxed timeout
            mockServer.sendMigratingPushToAll();

            assertTrue(disabledConnection.ping());

            // Verify that relaxed timeout was NOT activated
            assertFalse(disabledConnection.isRelaxedTimeoutActive());
            assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

            // Send FAILING_OVER push message - should also NOT activate relaxed timeout
            mockServer.sendFailingOverPushToAll();

            assertTrue(disabledConnection.ping());

            // Verify that relaxed timeout is still NOT activated
            assertFalse(disabledConnection.isRelaxedTimeoutActive());
            assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

            // Send MIGRATED and FAILED_OVER messages - timeout should remain unchanged
            mockServer.sendMigratedPushToAll();
            mockServer.sendFailedOverPushToAll();

            assertTrue(disabledConnection.ping());
            assertFalse(disabledConnection.isRelaxedTimeoutActive());
            assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

        } finally {
            if (disabledConnection.isConnected()) {
                disabledConnection.close();
            }
        }
    }

    @Test
    public void testManualRelaxTimeoutsCallWithDisabledTimeoutRelaxation() throws Exception {
        // Create a connection with disabled timeout relaxation
        Connection disabledConnection = createConnectionWithDisabledTimeoutRelaxation();
        Socket disabledSocket = ReflectionTestUtils.getField(disabledConnection, "socket");

        try {
            assertTrue(disabledConnection.isConnected());
            assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

            // Manually call relaxTimeouts() - should have no effect when disabled
            disabledConnection.relaxTimeouts();

            // Verify that timeout was not changed
            assertFalse(disabledConnection.isRelaxedTimeoutActive());
            assertEquals(originalTimeoutMs, disabledSocket.getSoTimeout());

            // Verify connection still works
            assertTrue(disabledConnection.ping());

        } finally {
            if (disabledConnection.isConnected()) {
                disabledConnection.close();
            }
        }
    }

    @Test
    public void testNullTimeoutOptionsDisablesRelaxedTimeout() throws Exception {
        // Create a connection with null timeout options
        Connection nullTimeoutConnection = createConnectionWithNullTimeoutOptions();
        Socket nullTimeoutSocket = ReflectionTestUtils.getField(nullTimeoutConnection, "socket");

        try {
            assertTrue(nullTimeoutConnection.isConnected());
            assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

            // Verify that relaxed timeout is disabled
            assertFalse(nullTimeoutConnection.isRelaxedTimeoutActive());

            // Send maintenance push messages - should NOT activate relaxed timeout
            mockServer.sendMigratingPushToAll();

            assertTrue(nullTimeoutConnection.ping());
            assertFalse(nullTimeoutConnection.isRelaxedTimeoutActive());
            assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

            // Manual call should also have no effect
            nullTimeoutConnection.relaxTimeouts();
            assertFalse(nullTimeoutConnection.isRelaxedTimeoutActive());
            assertEquals(originalTimeoutMs, nullTimeoutSocket.getSoTimeout());

        } finally {
            if (nullTimeoutConnection.isConnected()) {
                nullTimeoutConnection.close();
            }
        }
    }

    /**
     * Helper method to create a connection with disabled timeout relaxation.
     */
    private Connection createConnectionWithDisabledTimeoutRelaxation() throws IOException {
        // Create configuration with disabled timeout relaxation
        TimeoutOptions disabledTimeoutOptions = TimeoutOptions.create(); // Uses default disabled timeout

        MaintenanceEventHandler maintenanceEventHandler = new MaintenanceEventHandlerImpl();

        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .socketTimeoutMillis(originalTimeoutMs)
            .timeoutOptions(disabledTimeoutOptions)
            .maintenanceEventHandler(maintenanceEventHandler)
            .protocol(RedisProtocol.RESP3)
            .build();

        // Create connection to the mock server
        HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
        Connection disabledConnection = new Connection(hostAndPort, clientConfig);
        disabledConnection.connect();

        return disabledConnection;
    }

    /**
     * Helper method to create a connection with null timeout options.
     */
    private Connection createConnectionWithNullTimeoutOptions() throws IOException {
        MaintenanceEventHandler maintenanceEventHandler = new MaintenanceEventHandlerImpl();

        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .socketTimeoutMillis(originalTimeoutMs)
            // Note: not setting timeoutOptions, so it will be null
            .maintenanceEventHandler(maintenanceEventHandler)
            .protocol(RedisProtocol.RESP3)
            .build();

        // Create connection to the mock server
        HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
        Connection nullTimeoutConnection = new Connection(hostAndPort, clientConfig);
        nullTimeoutConnection.connect();

        return nullTimeoutConnection;
    }
}
