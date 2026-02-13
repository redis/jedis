package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.mcf.InitializationPolicy.Decision;

/**
 * Unit tests for {@link ConnectionInitializationContext}.
 */
@ExtendWith(MockitoExtension.class)
public class ConnectionInitializationContextTest {

  @Mock
  private HealthStatusManager healthStatusManager;

  @Mock
  private MultiDbConnectionProvider.Database database1;

  @Mock
  private MultiDbConnectionProvider.Database database2;

  @Mock
  private MultiDbConnectionProvider.Database database3;

  private Endpoint endpoint1;
  private Endpoint endpoint2;
  private Endpoint endpoint3;
  private Map<Endpoint, MultiDbConnectionProvider.Database> databases;

  @BeforeEach
  void setUp() {
    endpoint1 = new HostAndPort("fake", 6379);
    endpoint2 = new HostAndPort("fake", 6380);
    endpoint3 = new HostAndPort("fake", 6381);
    databases = new HashMap<>();
  }

  @Nested
  @DisplayName("Context Construction Tests")
  class ContextConstructionTests {

    @Test
    @DisplayName("Should count healthy endpoints as available")
    void shouldCountHealthyAsAvailable() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.HEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint2)).thenReturn(HealthStatus.HEALTHY);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(2, ctx.getAvailableConnections());
      assertEquals(0, ctx.getFailedConnections());
      assertEquals(0, ctx.getPendingConnections());
    }

    @Test
    @DisplayName("Should count unhealthy endpoints as failed")
    void shouldCountUnhealthyAsFailed() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.UNHEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint2)).thenReturn(HealthStatus.UNHEALTHY);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(0, ctx.getAvailableConnections());
      assertEquals(2, ctx.getFailedConnections());
      assertEquals(0, ctx.getPendingConnections());
    }

    @Test
    @DisplayName("Should count unknown status as pending")
    void shouldCountUnknownAsPending() {
      databases.put(endpoint1, database1);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.UNKNOWN);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(0, ctx.getAvailableConnections());
      assertEquals(0, ctx.getFailedConnections());
      assertEquals(1, ctx.getPendingConnections());
    }

    @Test
    @DisplayName("Should count endpoints without health check as available")
    void shouldCountNoHealthCheckAsAvailable() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(false);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(false);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(2, ctx.getAvailableConnections());
      assertEquals(0, ctx.getFailedConnections());
      assertEquals(0, ctx.getPendingConnections());
    }

    @Test
    @DisplayName("Should handle mixed health check states")
    void shouldHandleMixedStates() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);
      databases.put(endpoint3, database3);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.HEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint2)).thenReturn(HealthStatus.UNHEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint3)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint3)).thenReturn(HealthStatus.UNKNOWN);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(1, ctx.getAvailableConnections());
      assertEquals(1, ctx.getFailedConnections());
      assertEquals(1, ctx.getPendingConnections());
    }

    @Test
    @DisplayName("Should handle empty database map")
    void shouldHandleEmptyDatabaseMap() {
      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(0, ctx.getAvailableConnections());
      assertEquals(0, ctx.getFailedConnections());
      assertEquals(0, ctx.getPendingConnections());
    }

    @Test
    @DisplayName("Should handle mix of health check enabled and disabled endpoints")
    void shouldHandleMixedHealthCheckConfiguration() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);
      databases.put(endpoint3, database3);

      // endpoint1: health check enabled, healthy
      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.HEALTHY);

      // endpoint2: health check disabled - should be counted as available
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(false);

      // endpoint3: health check enabled, pending
      when(healthStatusManager.hasHealthCheck(endpoint3)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint3)).thenReturn(HealthStatus.UNKNOWN);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(2, ctx.getAvailableConnections()); // endpoint1 + endpoint2
      assertEquals(0, ctx.getFailedConnections());
      assertEquals(1, ctx.getPendingConnections()); // endpoint3
    }
  }

  @Nested
  @DisplayName("ConformsTo Policy Evaluation Tests")
  class ConformsToPolicyTests {

    @Test
    @DisplayName("Should delegate to policy evaluate method")
    void shouldDelegateToPolicy() {
      databases.put(endpoint1, database1);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.HEALTHY);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(Decision.SUCCESS, ctx.conformsTo(InitializationPolicy.BuiltIn.ONE_AVAILABLE));
      assertEquals(Decision.SUCCESS, ctx.conformsTo(InitializationPolicy.BuiltIn.ALL_AVAILABLE));
      assertEquals(Decision.SUCCESS,
        ctx.conformsTo(InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE));
    }

    @Test
    @DisplayName("Should return CONTINUE for ONE_AVAILABLE when all pending")
    void shouldReturnContinueWhenAllPending() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.UNKNOWN);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint2)).thenReturn(HealthStatus.UNKNOWN);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(Decision.CONTINUE, ctx.conformsTo(InitializationPolicy.BuiltIn.ONE_AVAILABLE));
    }

    @Test
    @DisplayName("Should return FAIL for ALL_AVAILABLE when any failed")
    void shouldReturnFailWhenAnyFailed() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.HEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint2)).thenReturn(HealthStatus.UNHEALTHY);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      assertEquals(Decision.FAIL, ctx.conformsTo(InitializationPolicy.BuiltIn.ALL_AVAILABLE));
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should include counts in toString")
    void shouldIncludeCountsInToString() {
      databases.put(endpoint1, database1);
      databases.put(endpoint2, database2);
      databases.put(endpoint3, database3);

      when(healthStatusManager.hasHealthCheck(endpoint1)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint1)).thenReturn(HealthStatus.HEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint2)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint2)).thenReturn(HealthStatus.UNHEALTHY);
      when(healthStatusManager.hasHealthCheck(endpoint3)).thenReturn(true);
      when(healthStatusManager.getHealthStatus(endpoint3)).thenReturn(HealthStatus.UNKNOWN);

      ConnectionInitializationContext ctx = new ConnectionInitializationContext(databases,
          healthStatusManager);

      String result = ctx.toString();
      assertTrue(result.contains("available=1"));
      assertTrue(result.contains("failed=1"));
      assertTrue(result.contains("pending=1"));
    }
  }
}
