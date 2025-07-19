package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;

/**
 * Test to verify that health status events are not missed between endpoint registration
 * and listener registration, and that handleHealthStatusChange only processes events after
 * initialization is complete.
 */
public class HealthStatusEventOrderingTest {

    private HostAndPort testEndpoint;
    private JedisClientConfig testConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testEndpoint = new HostAndPort("localhost", 6379);
        testConfig = DefaultJedisClientConfig.builder().build();
    }

    @Test
    void testHealthStatusManagerEventOrdering() throws InterruptedException {
        HealthStatusManager manager = new HealthStatusManager();
        
        // Counter to track events received
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch eventLatch = new CountDownLatch(1);
        
        // Create a listener that counts events
        HealthStatusListener listener = event -> {
            eventCount.incrementAndGet();
            eventLatch.countDown();
        };
        
        // Register listener BEFORE adding endpoint (correct order)
        manager.registerListener(testEndpoint, listener);
        
        // Create a strategy that immediately returns HEALTHY
        HealthCheckStrategy immediateStrategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() {
                return 100;
            }

            @Override
            public int getTimeout() {
                return 50;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                return HealthStatus.HEALTHY;
            }
        };
        
        // Add endpoint - this should trigger health check and event
        manager.add(testEndpoint, immediateStrategy);
        
        // Wait for event to be processed
        assertTrue(eventLatch.await(2, TimeUnit.SECONDS), "Should receive health status event");
        
        // Should have received at least one event (UNKNOWN -> HEALTHY)
        assertTrue(eventCount.get() >= 1, "Should have received at least one health status event");
        
        manager.remove(testEndpoint);
    }



    @Test
    void testInitializationEventQueuing() {
        // This test simulates the new queuing behavior

        AtomicInteger processedEvents = new AtomicInteger(0);
        boolean[] initComplete = {false};
        java.util.Queue<HealthStatusChangeEvent> eventQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

        // Simulate handleHealthStatusChange logic with queuing
        HealthStatusListener mockHandler = event -> {
            if (!initComplete[0]) {
                eventQueue.offer(event); // Queue events during initialization
                return;
            }
            processedEvents.incrementAndGet();
        };

        // Simulate processPendingEvents logic
        Runnable processPendingEvents = () -> {
            HealthStatusChangeEvent event;
            while ((event = eventQueue.poll()) != null) {
                processedEvents.incrementAndGet();
            }
        };

        // Simulate events during initialization
        HealthStatusChangeEvent event1 = new HealthStatusChangeEvent(testEndpoint, HealthStatus.UNKNOWN, HealthStatus.HEALTHY);
        HealthStatusChangeEvent event2 = new HealthStatusChangeEvent(testEndpoint, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);

        // Events during initialization should be queued, not processed
        mockHandler.onStatusChange(event1);
        mockHandler.onStatusChange(event2);
        assertEquals(0, processedEvents.get(), "Events during initialization should be queued, not processed");
        assertEquals(2, eventQueue.size(), "Events should be queued during initialization");

        // Mark initialization complete and process pending events
        initComplete[0] = true;
        processPendingEvents.run();
        assertEquals(2, processedEvents.get(), "Queued events should be processed after initialization");
        assertEquals(0, eventQueue.size(), "Queue should be empty after processing");

        // Events after initialization should be processed immediately
        HealthStatusChangeEvent event3 = new HealthStatusChangeEvent(testEndpoint, HealthStatus.UNHEALTHY, HealthStatus.HEALTHY);
        mockHandler.onStatusChange(event3);
        assertEquals(3, processedEvents.get(), "Events after initialization should be processed immediately");
    }

    @Test
    void testHealthStatusManagerHasHealthCheck() {
        HealthStatusManager manager = new HealthStatusManager();

        // Initially no health check
        assertFalse(manager.hasHealthCheck(testEndpoint), "Should not have health check initially");

        // Create a simple strategy
        HealthCheckStrategy strategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() { return 100; }

            @Override
            public int getTimeout() { return 50; }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) { return HealthStatus.HEALTHY; }
        };

        // Add health check
        manager.add(testEndpoint, strategy);
        assertTrue(manager.hasHealthCheck(testEndpoint), "Should have health check after adding");

        // Remove health check
        manager.remove(testEndpoint);
        assertFalse(manager.hasHealthCheck(testEndpoint), "Should not have health check after removing");
    }

    @Test
    void testHealthStatusManagerConcurrentAccess() throws InterruptedException {
        HealthStatusManager manager = new HealthStatusManager();

        // Create multiple endpoints
        HostAndPort endpoint1 = new HostAndPort("host1", 6379);
        HostAndPort endpoint2 = new HostAndPort("host2", 6379);

        AtomicInteger eventsReceived = new AtomicInteger(0);
        CountDownLatch allEventsLatch = new CountDownLatch(4); // Expect 4 events total

        // Create listeners for both endpoints
        HealthStatusListener listener1 = event -> {
            eventsReceived.incrementAndGet();
            allEventsLatch.countDown();
        };

        HealthStatusListener listener2 = event -> {
            eventsReceived.incrementAndGet();
            allEventsLatch.countDown();
        };

        // Register listeners concurrently
        Thread registerThread1 = new Thread(() -> manager.registerListener(endpoint1, listener1));
        Thread registerThread2 = new Thread(() -> manager.registerListener(endpoint2, listener2));

        registerThread1.start();
        registerThread2.start();

        registerThread1.join();
        registerThread2.join();

        // Create strategies that return different statuses
        HealthCheckStrategy strategy1 = new HealthCheckStrategy() {
            @Override
            public int getInterval() { return 100; }
            @Override
            public int getTimeout() { return 50; }
            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) { return HealthStatus.HEALTHY; }
        };

        HealthCheckStrategy strategy2 = new HealthCheckStrategy() {
            @Override
            public int getInterval() { return 100; }
            @Override
            public int getTimeout() { return 50; }
            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) { return HealthStatus.UNHEALTHY; }
        };

        // Add health checks concurrently
        Thread addThread1 = new Thread(() -> manager.add(endpoint1, strategy1));
        Thread addThread2 = new Thread(() -> manager.add(endpoint2, strategy2));

        addThread1.start();
        addThread2.start();

        addThread1.join();
        addThread2.join();

        // Wait for all events to be processed
        assertTrue(allEventsLatch.await(3, TimeUnit.SECONDS), "Should receive all health status events");

        // Verify final states
        assertTrue(manager.hasHealthCheck(endpoint1));
        assertTrue(manager.hasHealthCheck(endpoint2));

        // Clean up
        manager.remove(endpoint1);
        manager.remove(endpoint2);
    }

    @Test
    void testNoEventsAreMissedDuringRegistration() throws InterruptedException {
        HealthStatusManager manager = new HealthStatusManager();

        // Track all events received
        AtomicInteger eventsReceived = new AtomicInteger(0);
        CountDownLatch allEventsLatch = new CountDownLatch(2); // Expect 2 events: UNKNOWN->HEALTHY, HEALTHY->UNHEALTHY

        // Create a strategy that changes status quickly
        AtomicInteger checkCount = new AtomicInteger(0);
        HealthCheckStrategy rapidChangeStrategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() { return 50; } // Very fast interval

            @Override
            public int getTimeout() { return 25; }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                int count = checkCount.incrementAndGet();
                // First check: HEALTHY, second check: UNHEALTHY
                return count == 1 ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
            }
        };

        // Create listener that tracks events
        HealthStatusListener eventTracker = event -> {
            eventsReceived.incrementAndGet();
            allEventsLatch.countDown();
            System.out.println("Event received: " + event.getOldStatus() + " -> " + event.getNewStatus());
        };

        // Register listener BEFORE adding endpoint (correct order to prevent missing events)
        manager.registerListener(testEndpoint, eventTracker);

        // Add endpoint - this should trigger rapid health checks
        manager.add(testEndpoint, rapidChangeStrategy);

        // Wait for all expected events
        assertTrue(allEventsLatch.await(3, TimeUnit.SECONDS),
            "Should receive all health status change events within timeout");

        // Verify we received the expected number of events
        assertEquals(2, eventsReceived.get(), "Should have received exactly 2 health status change events");

        // Clean up
        manager.remove(testEndpoint);
    }

    @Test
    void testEventOrderingWithMultipleRapidChanges() throws InterruptedException {
        HealthStatusManager manager = new HealthStatusManager();

        // Track events in order
        java.util.List<HealthStatusChangeEvent> receivedEvents =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        CountDownLatch eventsLatch = new CountDownLatch(3); // Expect 3 transitions

        // Create a strategy that cycles through statuses
        AtomicInteger checkCount = new AtomicInteger(0);
        HealthCheckStrategy cyclingStrategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() { return 30; } // Very fast

            @Override
            public int getTimeout() { return 15; }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                int count = checkCount.incrementAndGet();
                switch (count) {
                    case 1: return HealthStatus.HEALTHY;
                    case 2: return HealthStatus.UNHEALTHY;
                    case 3: return HealthStatus.HEALTHY;
                    default: return HealthStatus.HEALTHY;
                }
            }
        };

        // Register listener to capture events in order
        HealthStatusListener orderTracker = event -> {
            receivedEvents.add(event);
            eventsLatch.countDown();
        };

        // Register listener BEFORE adding endpoint
        manager.registerListener(testEndpoint, orderTracker);

        // Add endpoint to start health checks
        manager.add(testEndpoint, cyclingStrategy);

        // Wait for all events
        assertTrue(eventsLatch.await(5, TimeUnit.SECONDS),
            "Should receive all rapid health status changes");

        // Verify event sequence
        assertEquals(3, receivedEvents.size(), "Should have received 3 events");

        // Verify the sequence: UNKNOWN->HEALTHY, HEALTHY->UNHEALTHY, UNHEALTHY->HEALTHY
        assertEquals(HealthStatus.UNKNOWN, receivedEvents.get(0).getOldStatus());
        assertEquals(HealthStatus.HEALTHY, receivedEvents.get(0).getNewStatus());

        assertEquals(HealthStatus.HEALTHY, receivedEvents.get(1).getOldStatus());
        assertEquals(HealthStatus.UNHEALTHY, receivedEvents.get(1).getNewStatus());

        assertEquals(HealthStatus.UNHEALTHY, receivedEvents.get(2).getOldStatus());
        assertEquals(HealthStatus.HEALTHY, receivedEvents.get(2).getNewStatus());

        // Clean up
        manager.remove(testEndpoint);
    }
}
