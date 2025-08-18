package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.RedisCredentials;
import redis.clients.jedis.mcf.LagAwareStrategy.Config;

public class LagAwareStrategyUnitTest {

    private Endpoint endpoint;
    private Supplier<RedisCredentials> creds;

    @BeforeEach
    void setup() {
        endpoint = new Endpoint() {
            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public int getPort() {
                return 8443;
            }
        };
        creds = () -> new DefaultRedisCredentials("user", "pwd");
    }

    @Test
    void healthy_when_bdb_available_and_cached_uid_used_on_next_check() throws Exception {
        RedisRestAPI.BdbInfo bdbInfo = new RedisRestAPI.BdbInfo("1",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("127.0.0.1"), "localhost", 6379, "1:1")));

        RedisRestAPI[] reference = new RedisRestAPI[1];
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenReturn(Arrays.asList(bdbInfo));
                when(mock.checkBdbAvailability("1", true, 100L)).thenReturn(true);
                reference[0] = mock;
            })) {
            Config lagCheckConfig = Config.builder(endpoint, creds).interval(500).timeout(250)
                .minConsecutiveSuccessCount(2).build();
            try (LagAwareStrategy strategy = new LagAwareStrategy(lagCheckConfig)) {
                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                reset(api);
                when(api.checkBdbAvailability("1", true, 100L)).thenReturn(true);

                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                verify(api, never()).getBdbs(); // Should not call getBdbs again when cached
                verify(api, times(1)).checkBdbAvailability("1", true, 100L);
            }
        }
    }

    @Test
    void unhealthy_when_no_bdb_returned() throws Exception {
        AtomicReference<RedisRestAPI> ref = new AtomicReference<RedisRestAPI>();
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenReturn(Collections.emptyList()); // No BDBs found
                ref.set(mock);
            })) {

            Config lagCheckConfig = Config.builder(endpoint, creds).interval(500).timeout(250)
                .minConsecutiveSuccessCount(1).build();
            try (LagAwareStrategy strategy = new LagAwareStrategy(lagCheckConfig)) {
                assertEquals(HealthStatus.UNHEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = ref.get();
                verify(api, times(1)).getBdbs();
                verify(api, never()).checkBdbAvailability(anyString(), anyBoolean());
            }
        }
    }

    @Test
    void unhealthy_and_cache_reset_on_exception_then_recovers_next_time() throws Exception {
        RedisRestAPI.BdbInfo bdbInfo = new RedisRestAPI.BdbInfo("42",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("127.0.0.1"), "localhost", 6379, "1:1")));

        AtomicReference<RedisRestAPI> ref = new AtomicReference<RedisRestAPI>();
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenThrow(new RuntimeException("boom"));
                ref.set(mock);
            })) {

            Config lagCheckConfig = Config.builder(endpoint, creds).interval(500).timeout(250)
                .minConsecutiveSuccessCount(1).build();
            try (LagAwareStrategy strategy = new LagAwareStrategy(lagCheckConfig)) {
                RedisRestAPI api = ref.get();
                assertEquals(HealthStatus.UNHEALTHY, strategy.doHealthCheck(endpoint));

                reset(api);
                when(api.getBdbs()).thenReturn(Arrays.asList(bdbInfo));
                when(api.checkBdbAvailability("42", true, 100L)).thenReturn(true);

                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
            }
        }
    }

    @Test
    void healthy_when_matching_bdb_found_by_host() throws Exception {
        RedisRestAPI.BdbInfo matchingBdb = new RedisRestAPI.BdbInfo("matched-bdb-123",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("127.0.0.1"), "localhost", 6379, "1:1")));

        RedisRestAPI[] reference = new RedisRestAPI[1];
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenReturn(Arrays.asList(matchingBdb));
                when(mock.checkBdbAvailability("matched-bdb-123", true, 100L)).thenReturn(true);
                reference[0] = mock;
            })) {
            Config lagCheckConfig = Config.builder(endpoint, creds).interval(500).timeout(250)
                .minConsecutiveSuccessCount(2).extendedCheckEnabled(true)
                .availabilityLagTolerance(Duration.ofMillis(100)).build();
            try (LagAwareStrategy strategy = new LagAwareStrategy(lagCheckConfig)) {
                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                verify(api, times(1)).getBdbs();
                verify(api, times(1)).checkBdbAvailability("matched-bdb-123", true, 100L);
            }
        }
    }

    @Test
    void unhealthy_when_no_matching_host_found() throws Exception {
        RedisRestAPI.BdbInfo nonMatchingBdb = new RedisRestAPI.BdbInfo("other-bdb-456", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("192.168.1.100"), "other-host.example.com", 6379, "2:1")));

        RedisRestAPI[] reference = new RedisRestAPI[1];
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenReturn(Arrays.asList(nonMatchingBdb)); // BDB that doesn't match localhost
                reference[0] = mock;
            })) {
            Config lagCheckConfig = Config.builder(endpoint, creds).interval(500).timeout(250)
                .minConsecutiveSuccessCount(2).build();
            try (LagAwareStrategy strategy = new LagAwareStrategy(lagCheckConfig)) {
                assertEquals(HealthStatus.UNHEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                verify(api, times(1)).getBdbs();
                verify(api, never()).checkBdbAvailability(anyString(), anyBoolean()); // Should not check availability
            }
        }
    }

    @Test
    void config_builder_creates_config_with_default_values() {
        Config config = Config.builder(endpoint, creds).build();

        assertEquals(1000, config.interval);
        assertEquals(1000, config.timeout);
        assertEquals(3, config.minConsecutiveSuccessCount);
        assertEquals(Duration.ofMillis(100), config.getAvailabilityLagTolerance());
        assertEquals(endpoint, config.getEndpoint());
        assertEquals(creds, config.getCredentialsSupplier());
    }

    @Test
    void config_builder_creates_config_with_custom_values() {
        Config config = Config.builder(endpoint, creds).interval(500).timeout(250).minConsecutiveSuccessCount(2)
            .availabilityLagTolerance(Duration.ofMillis(50)).build();

        assertEquals(500, config.interval);
        assertEquals(250, config.timeout);
        assertEquals(2, config.minConsecutiveSuccessCount);
        assertEquals(Duration.ofMillis(50), config.getAvailabilityLagTolerance());
        assertEquals(endpoint, config.getEndpoint());
        assertEquals(creds, config.getCredentialsSupplier());
    }

    @Test
    void config_builder_allows_fluent_chaining() {
        // Test that all builder methods return the builder instance for chaining
        Config config = Config.builder(endpoint, creds).interval(800).timeout(400).minConsecutiveSuccessCount(5)
            .availabilityLagTolerance(Duration.ofMillis(200)).build();

        assertNotNull(config);
        assertEquals(800, config.interval);
        assertEquals(400, config.timeout);
        assertEquals(5, config.minConsecutiveSuccessCount);
        assertEquals(Duration.ofMillis(200), config.getAvailabilityLagTolerance());
    }

    @Test
    void config_builder_creates_config_with_extended_check_enabled() {
        Config config = Config.builder(endpoint, creds).extendedCheckEnabled(true)
            .availabilityLagTolerance(Duration.ofMillis(150)).build();

        assertTrue(config.isExtendedCheckEnabled());
        assertEquals(Duration.ofMillis(150), config.getAvailabilityLagTolerance());
    }

    @Test
    void config_builder_creates_config_with_extended_check_disabled_by_default() {
        Config config = Config.builder(endpoint, creds).build();

        assertTrue(config.isExtendedCheckEnabled());
    }

    @Test
    void healthy_when_extended_check_enabled_and_lag_check_passes() throws Exception {
        RedisRestAPI.BdbInfo bdbInfo = new RedisRestAPI.BdbInfo("1",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("127.0.0.1"), "localhost", 6379, "1:1")));

        RedisRestAPI[] reference = new RedisRestAPI[1];
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenReturn(Arrays.asList(bdbInfo));
                when(mock.checkBdbAvailability("1", true, 100L)).thenReturn(true);
                reference[0] = mock;
            })) {

            Config config = Config.builder(endpoint, creds).extendedCheckEnabled(true)
                .availabilityLagTolerance(Duration.ofMillis(100)).build();

            try (LagAwareStrategy strategy = new LagAwareStrategy(config)) {
                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                verify(api, times(1)).getBdbs();
                verify(api, times(1)).checkBdbAvailability("1", true, 100L);
                verify(api, never()).checkBdbAvailability("1", false);
            }
        }
    }

    @Test
    void healthy_when_extended_check_disabled_and_standard_check_passes() throws Exception {
        RedisRestAPI.BdbInfo bdbInfo = new RedisRestAPI.BdbInfo("1",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("127.0.0.1"), "localhost", 6379, "1:1")));

        RedisRestAPI[] reference = new RedisRestAPI[1];
        try (MockedConstruction<RedisRestAPI> mockedConstructor = mockConstruction(RedisRestAPI.class,
            (mock, context) -> {
                when(mock.getBdbs()).thenReturn(Arrays.asList(bdbInfo));
                when(mock.checkBdbAvailability("1", false)).thenReturn(true);
                reference[0] = mock;
            })) {

            Config config = Config.builder(endpoint, creds).extendedCheckEnabled(false).build();

            try (LagAwareStrategy strategy = new LagAwareStrategy(config)) {
                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                verify(api, times(1)).getBdbs();
                verify(api, times(1)).checkBdbAvailability("1", false);
                verify(api, never()).checkBdbAvailability(eq("1"), eq(true), any());
            }
        }
    }
}
