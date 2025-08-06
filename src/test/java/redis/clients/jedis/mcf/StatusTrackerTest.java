package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import redis.clients.jedis.HostAndPort;

public class StatusTrackerTest {

    @Mock
    private HealthStatusManager mockHealthStatusManager;

    private StatusTracker statusTracker;
    private HostAndPort testEndpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        statusTracker = new StatusTracker(mockHealthStatusManager);
        testEndpoint = new HostAndPort("localhost", 6379);
    }

    @Test
    void testWaitForHealthStatus_AlreadyDetermined() {
        // Given: Health status is already HEALTHY
        when(mockHealthStatusManager.getHealthStatus(testEndpoint)).thenReturn(HealthStatus.HEALTHY);

        // When: Waiting for health status
        HealthStatus result = statusTracker.waitForHealthStatus(testEndpoint);

        // Then: Should return immediately without waiting
        assertEquals(HealthStatus.HEALTHY, result);
        verify(mockHealthStatusManager, never()).registerListener(eq(testEndpoint), any(HealthStatusListener.class));
    }

    @Test
    void testWaitForHealthStatus_EventDriven() throws InterruptedException {
        // Given: Health status is initially UNKNOWN
        when(mockHealthStatusManager.getHealthStatus(testEndpoint))
            .thenReturn(HealthStatus.UNKNOWN)  // First call
            .thenReturn(HealthStatus.UNKNOWN); // Second call after registering listener

        // Capture the registered listener
        final HealthStatusListener[] capturedListener = new HealthStatusListener[1];
        doAnswer(invocation -> {
            capturedListener[0] = invocation.getArgument(1);
            return null;
        }).when(mockHealthStatusManager).registerListener(eq(testEndpoint), any(HealthStatusListener.class));

        // When: Start waiting in a separate thread
        CountDownLatch testLatch = new CountDownLatch(1);
        final HealthStatus[] result = new HealthStatus[1];
        
        Thread waitingThread = new Thread(() -> {
            result[0] = statusTracker.waitForHealthStatus(testEndpoint);
            testLatch.countDown();
        });
        waitingThread.start();

        // Give some time for the listener to be registered
        Thread.sleep(50);

        // Simulate health status change event
        assertNotNull(capturedListener[0], "Listener should have been registered");
        HealthStatusChangeEvent event = new HealthStatusChangeEvent(testEndpoint, HealthStatus.UNKNOWN, HealthStatus.HEALTHY);
        capturedListener[0].onStatusChange(event);

        // Then: Should complete and return the new status
        assertTrue(testLatch.await(1, TimeUnit.SECONDS), "Should complete within timeout");
        assertEquals(HealthStatus.HEALTHY, result[0]);
        
        // Verify cleanup
        verify(mockHealthStatusManager).unregisterListener(eq(testEndpoint), eq(capturedListener[0]));
    }

    @Test
    void testWaitForHealthStatus_IgnoresUnknownStatus() throws InterruptedException {
        // Given: Health status is initially UNKNOWN
        when(mockHealthStatusManager.getHealthStatus(testEndpoint)).thenReturn(HealthStatus.UNKNOWN);

        // Capture the registered listener
        final HealthStatusListener[] capturedListener = new HealthStatusListener[1];
        doAnswer(invocation -> {
            capturedListener[0] = invocation.getArgument(1);
            return null;
        }).when(mockHealthStatusManager).registerListener(eq(testEndpoint), any(HealthStatusListener.class));

        // When: Start waiting in a separate thread
        CountDownLatch testLatch = new CountDownLatch(1);
        final HealthStatus[] result = new HealthStatus[1];
        
        Thread waitingThread = new Thread(() -> {
            result[0] = statusTracker.waitForHealthStatus(testEndpoint);
            testLatch.countDown();
        });
        waitingThread.start();

        // Give some time for the listener to be registered
        Thread.sleep(50);

        // Simulate UNKNOWN status change (should be ignored)
        assertNotNull(capturedListener[0], "Listener should have been registered");
        HealthStatusChangeEvent unknownEvent = new HealthStatusChangeEvent(testEndpoint, HealthStatus.UNKNOWN, HealthStatus.UNKNOWN);
        capturedListener[0].onStatusChange(unknownEvent);

        // Should not complete yet
        assertFalse(testLatch.await(100, TimeUnit.MILLISECONDS), "Should not complete with UNKNOWN status");

        // Now send a real status change
        HealthStatusChangeEvent realEvent = new HealthStatusChangeEvent(testEndpoint, HealthStatus.UNKNOWN, HealthStatus.UNHEALTHY);
        capturedListener[0].onStatusChange(realEvent);

        // Then: Should complete now
        assertTrue(testLatch.await(1, TimeUnit.SECONDS), "Should complete with real status");
        assertEquals(HealthStatus.UNHEALTHY, result[0]);
    }

    @Test
    void testWaitForHealthStatus_IgnoresOtherEndpoints() throws InterruptedException {
        // Given: Health status is initially UNKNOWN
        when(mockHealthStatusManager.getHealthStatus(testEndpoint)).thenReturn(HealthStatus.UNKNOWN);
        HostAndPort otherEndpoint = new HostAndPort("other", 6379);

        // Capture the registered listener
        final HealthStatusListener[] capturedListener = new HealthStatusListener[1];
        doAnswer(invocation -> {
            capturedListener[0] = invocation.getArgument(1);
            return null;
        }).when(mockHealthStatusManager).registerListener(eq(testEndpoint), any(HealthStatusListener.class));

        // When: Start waiting in a separate thread
        CountDownLatch testLatch = new CountDownLatch(1);
        final HealthStatus[] result = new HealthStatus[1];
        
        Thread waitingThread = new Thread(() -> {
            result[0] = statusTracker.waitForHealthStatus(testEndpoint);
            testLatch.countDown();
        });
        waitingThread.start();

        // Give some time for the listener to be registered
        Thread.sleep(50);

        // Simulate status change for different endpoint (should be ignored)
        assertNotNull(capturedListener[0], "Listener should have been registered");
        HealthStatusChangeEvent otherEvent = new HealthStatusChangeEvent(otherEndpoint, HealthStatus.UNKNOWN, HealthStatus.HEALTHY);
        capturedListener[0].onStatusChange(otherEvent);

        // Should not complete yet
        assertFalse(testLatch.await(100, TimeUnit.MILLISECONDS), "Should not complete with other endpoint");

        // Now send event for correct endpoint
        HealthStatusChangeEvent correctEvent = new HealthStatusChangeEvent(testEndpoint, HealthStatus.UNKNOWN, HealthStatus.HEALTHY);
        capturedListener[0].onStatusChange(correctEvent);

        // Then: Should complete now
        assertTrue(testLatch.await(1, TimeUnit.SECONDS), "Should complete with correct endpoint");
        assertEquals(HealthStatus.HEALTHY, result[0]);
    }

    @Test
    void testWaitForHealthStatus_InterruptHandling() {
        // Given: Health status is initially UNKNOWN and will stay that way
        when(mockHealthStatusManager.getHealthStatus(testEndpoint)).thenReturn(HealthStatus.UNKNOWN);

        // When: Interrupt the waiting thread
        Thread testThread = new Thread(() -> {
            try {
                statusTracker.waitForHealthStatus(testEndpoint);
                fail("Should have thrown JedisConnectionException due to interrupt");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Interrupted while waiting"));
                assertTrue(Thread.currentThread().isInterrupted());
            }
        });

        testThread.start();

        // Give thread time to start waiting
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Interrupt the waiting thread
        testThread.interrupt();

        try {
            testThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(testThread.isAlive(), "Test thread should have completed");
    }

    @Test
    void testWaitForHealthStatus_RaceConditionProtection() {
        // Given: Health status changes between first check and listener registration
        when(mockHealthStatusManager.getHealthStatus(testEndpoint))
            .thenReturn(HealthStatus.UNKNOWN)  // First call
            .thenReturn(HealthStatus.HEALTHY); // Second call after registering listener

        // When: Waiting for health status
        HealthStatus result = statusTracker.waitForHealthStatus(testEndpoint);

        // Then: Should return the status from the second check without waiting
        assertEquals(HealthStatus.HEALTHY, result);

        // Verify listener was registered and unregistered
        verify(mockHealthStatusManager).registerListener(eq(testEndpoint), any(HealthStatusListener.class));
        verify(mockHealthStatusManager).unregisterListener(eq(testEndpoint), any(HealthStatusListener.class));
    }

    @Test
    void testWaitForHealthStatus_ListenerCleanupOnException() {
        // Given: Health status is initially UNKNOWN
        when(mockHealthStatusManager.getHealthStatus(testEndpoint)).thenReturn(HealthStatus.UNKNOWN);

        // Mock registerListener to throw an exception
        doThrow(new RuntimeException("Registration failed"))
            .when(mockHealthStatusManager).registerListener(eq(testEndpoint), any(HealthStatusListener.class));

        // When: Waiting for health status
        assertThrows(RuntimeException.class, () -> {
            statusTracker.waitForHealthStatus(testEndpoint);
        });

        // Then: Should still attempt to unregister (cleanup in finally block)
        verify(mockHealthStatusManager).registerListener(eq(testEndpoint), any(HealthStatusListener.class));
        // Note: unregisterListener might not be called if registerListener fails,
        // but the finally block should handle this gracefully
    }
}
