package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
                when(mock.checkBdbAvailability("1", true)).thenReturn(true);
                reference[0] = mock;
            })) {

            try (LagAwareStrategy strategy = new LagAwareStrategy(new Config(endpoint, creds, 500, 250, 2))) {
                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                reset(api);
                when(api.checkBdbAvailability("1", true)).thenReturn(true);

                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                verify(api, never()).getBdbs(); // Should not call getBdbs again when cached
                verify(api, times(1)).checkBdbAvailability("1", true);
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

            try (LagAwareStrategy strategy = new LagAwareStrategy(new Config(endpoint, creds, 500, 250, 1))) {
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

            try (LagAwareStrategy strategy = new LagAwareStrategy(new Config(endpoint, creds, 500, 250, 1))) {
                RedisRestAPI api = ref.get();
                assertEquals(HealthStatus.UNHEALTHY, strategy.doHealthCheck(endpoint));

                reset(api);
                when(api.getBdbs()).thenReturn(Arrays.asList(bdbInfo));
                when(api.checkBdbAvailability("42", true)).thenReturn(true);

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
                when(mock.checkBdbAvailability("matched-bdb-123", true)).thenReturn(true);
                reference[0] = mock;
            })) {

            try (LagAwareStrategy strategy = new LagAwareStrategy(new Config(endpoint, creds, 500, 250, 2))) {
                assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                verify(api, times(1)).getBdbs();
                verify(api, times(1)).checkBdbAvailability("matched-bdb-123", true);
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

            try (LagAwareStrategy strategy = new LagAwareStrategy(new Config(endpoint, creds, 500, 250, 2))) {
                assertEquals(HealthStatus.UNHEALTHY, strategy.doHealthCheck(endpoint));
                RedisRestAPI api = reference[0];
                verify(api, times(1)).getBdbs();
                verify(api, never()).checkBdbAvailability(anyString(), anyBoolean()); // Should not check availability
            }
        }
    }
}
