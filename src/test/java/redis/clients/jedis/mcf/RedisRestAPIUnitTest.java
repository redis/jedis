package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.RedisCredentials;

public class RedisRestAPIUnitTest {

    static class TestEndpoint implements Endpoint {
        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public int getPort() {
            return 8443;
        }
    }

    @Test
    void getBdbs_parsesArrayOfObjects() throws Exception {
        RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), creds(), 1000));
        HttpURLConnection conn = mock(HttpURLConnection.class);
        doReturn(conn).when(api).createConnection(any(), any(), any());

        when(conn.getResponseCode()).thenReturn(200);
        String body = "[ {\"uid\":\"1\", \"endpoints\":[]}, {\"uid\":\"2\", \"endpoints\":[]} ]";
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(body.getBytes()));

        List<RedisRestAPI.BdbInfo> result = api.getBdbs();
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getUid());
        assertEquals("2", result.get(1).getUid());
        verify(conn, times(1)).disconnect();
    }

    @Test
    void availability_logsAndReturnsFalseForNon200() throws Exception {
        RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), creds(), 1000));
        HttpURLConnection conn = mock(HttpURLConnection.class);
        doReturn(conn).when(api).createConnection(any(), any(), any());

        when(conn.getResponseCode()).thenReturn(503);
        String body = "{\"error_code\":\"bdb_unavailable\",\"description\":\"Database is not available\"}";
        when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream(body.getBytes()));

        assertFalse(api.checkBdbAvailability("2", false));
    }

    private static Supplier<RedisCredentials> creds() {
        return () -> new DefaultRedisCredentials("testUser", "testPwd");
    }

    @Test
    void availability_200_and_503_paths_cover_lagAware_toggle() throws Exception {
        RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), creds(), 1000));
        HttpURLConnection conn = mock(HttpURLConnection.class);
        doReturn(conn).when(api).createConnection(any(), any(), any());

        // Healthy path (200)
        when(conn.getResponseCode()).thenReturn(200);
        assertTrue(api.checkBdbAvailability("123", true));

        // Unhealthy path (503) with error body
        reset(conn);
        doReturn(conn).when(api).createConnection(any(), any(), any());
        when(conn.getResponseCode()).thenReturn(503);
        when(conn.getErrorStream())
            .thenReturn(new ByteArrayInputStream("{\"error_code\":\"bdb_unavailable\"}".getBytes()));
        assertFalse(api.checkBdbAvailability("123", false));
    }

    @Test
    void testCheckBdbAvailabilityWithExtendedCheck() throws Exception {
        RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), () -> new DefaultRedisCredentials("user", "pass")));
        HttpURLConnection conn = mock(HttpURLConnection.class);

        doReturn(conn).when(api).createConnection(any(), any(), any());
        when(conn.getResponseCode()).thenReturn(200);
        assertTrue(api.checkBdbAvailability("123", true, 100L));

        // Verify the correct URL was constructed with extended check parameters
        verify(api).createConnection(eq(
            "https://localhost:8443/v1/local/bdbs/123/availability?extend_check=lag&availability_lag_tolerance_ms=100"),
            eq("GET"), any());
    }

    @Test
    void testCheckBdbAvailabilityWithExtendedCheckNoTolerance() throws Exception {
        RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), () -> new DefaultRedisCredentials("user", "pass")));
        HttpURLConnection conn = mock(HttpURLConnection.class);

        doReturn(conn).when(api).createConnection(any(), any(), any());
        when(conn.getResponseCode()).thenReturn(200);
        assertTrue(api.checkBdbAvailability("123", true, null));

        // Verify the correct URL was constructed with extended check but no tolerance parameter
        verify(api).createConnection(eq("https://localhost:8443/v1/local/bdbs/123/availability?extend_check=lag"),
            eq("GET"), any());
    }

    @Test
    void testCheckBdbAvailabilityWithStandardCheck() throws Exception {
        RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), () -> new DefaultRedisCredentials("user", "pass")));
        HttpURLConnection conn = mock(HttpURLConnection.class);

        doReturn(conn).when(api).createConnection(any(), any(), any());
        when(conn.getResponseCode()).thenReturn(200);
        assertTrue(api.checkBdbAvailability("123", false, null));

        // Verify the correct URL was constructed for standard check (no query parameters)
        verify(api).createConnection(eq("https://localhost:8443/v1/local/bdbs/123/availability"), eq("GET"), any());
    }

    // ========== Parsing and BDB Matching Tests ==========

    @Test
    void parseBdbInfoFromResponse_parses_correctly() {
        String responseBody = "[\n" + "    {\n" + "        \"uid\": \"1\",\n" + "        \"endpoints\": [\n"
            + "            {\n" + "                \"dns_name\": \"redis-db1.example.com\",\n"
            + "                \"addr\": [\"10.0.1.100\"],\n" + "                \"port\": 6379,\n"
            + "                \"uid\": \"1:1\"\n" + "            }\n" + "        ]\n" + "    },\n" + "    {\n"
            + "        \"uid\": \"2\",\n" + "        \"endpoints\": [\n" + "            {\n"
            + "                \"dns_name\": \"redis-db2.example.com\",\n"
            + "                \"addr\": [\"10.0.1.101\"],\n" + "                \"port\": 6380,\n"
            + "                \"uid\": \"2:1\"\n" + "            }\n" + "        ]\n" + "    }\n" + "]";

        List<RedisRestAPI.BdbInfo> result = RedisRestAPI.parseBdbInfoFromResponse(responseBody);
        assertEquals(2, result.size());

        RedisRestAPI.BdbInfo bdb1 = result.get(0);
        assertEquals("1", bdb1.getUid());
        assertEquals(1, bdb1.getEndpoints().size());

        RedisRestAPI.EndpointInfo endpoint1 = bdb1.getEndpoints().get(0);
        assertEquals("redis-db1.example.com", endpoint1.getDnsName());
        assertEquals(Arrays.asList("10.0.1.100"), endpoint1.getAddr());
        assertEquals(Integer.valueOf(6379), endpoint1.getPort());
        assertEquals("1:1", endpoint1.getUid());
    }

    @Test
    void findMatchingBdb_matches_dns_name() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis-db1.example.com", 6379, "1:1")));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.101"), "redis-db2.example.com", 6380, "2:1")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "redis-db2.example.com");

        assertNotNull(result);
        assertEquals("2", result.getUid());
    }

    @Test
    void findMatchingBdb_matches_ip_address() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100", "192.168.1.100"),
                "redis-db1.example.com", 6379, "1:1")));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.101"), "redis-db2.example.com", 6380, "2:1")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "192.168.1.100");

        assertNotNull(result);
        assertEquals("1", result.getUid());
    }

    @Test
    void findMatchingBdb_returns_null_when_no_match() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis-db1.example.com", 6379, "1:1")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "nonexistent.example.com");

        assertNull(result);
    }

    @Test
    void findMatchingBdb_handles_multiple_endpoints_per_bdb() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis-db1-primary.example.com", 6379, "1:1"),
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.101"), "redis-db1-replica.example.com", 6380, "1:2")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "redis-db1-replica.example.com");

        assertNotNull(result);
        assertEquals("1", result.getUid());
    }

    @Test
    void parseBdbInfoFromResponse_handles_missing_fields_gracefully() {
        String responseBody = "[\n" + "    {\n" + "        \"uid\": \"1\"\n" + "    },\n" + "    {\n"
            + "        \"endpoints\": [\n" + "            {\n"
            + "                \"dns_name\": \"redis-db2.example.com\"\n" + "            }\n" + "        ]\n"
            + "    },\n" + "    {\n" + "        \"uid\": \"3\",\n" + "        \"endpoints\": [\n" + "            {\n"
            + "                \"dns_name\": \"redis-db3.example.com\",\n"
            + "                \"addr\": [\"10.0.1.103\"],\n" + "                \"port\": 6379\n" + "            }\n"
            + "        ]\n" + "    }\n" + "]";

        List<RedisRestAPI.BdbInfo> result = RedisRestAPI.parseBdbInfoFromResponse(responseBody);
        assertEquals(2, result.size()); // Only BDBs with uid are included

        RedisRestAPI.BdbInfo bdb1 = result.get(0);
        assertEquals("1", bdb1.getUid());
        assertEquals(0, bdb1.getEndpoints().size()); // No endpoints

        RedisRestAPI.BdbInfo bdb3 = result.get(1);
        assertEquals("3", bdb3.getUid());
        assertEquals(1, bdb3.getEndpoints().size());

        RedisRestAPI.EndpointInfo endpoint = bdb3.getEndpoints().get(0);
        assertEquals("redis-db3.example.com", endpoint.getDnsName());
        assertEquals(Arrays.asList("10.0.1.103"), endpoint.getAddr());
        assertEquals(Integer.valueOf(6379), endpoint.getPort());
        assertNull(endpoint.getUid()); // Missing uid field
    }

    @Test
    void parseBdbInfoFromResponse_handles_empty_response() {
        String responseBody = "[]";

        List<RedisRestAPI.BdbInfo> result = RedisRestAPI.parseBdbInfoFromResponse(responseBody);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatchingBdb_prefers_dns_name_over_addr() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "target-host.example.com", 6379, "1:1")));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2",
            Arrays.asList(new RedisRestAPI.EndpointInfo(Arrays.asList("target-host.example.com"),
                "other-host.example.com", 6380, "2:1")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);

        // Should match BDB 1 by dns_name, not BDB 2 by addr
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "target-host.example.com");
        assertNotNull(result);
        assertEquals("1", result.getUid());
    }

    @Test
    void findMatchingBdb_matches_correct_bdb_with_same_host_different_ports() {
        // Two BDBs with same DNS name but different ports
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis.example.com", 6379, "1:1")));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis.example.com", 6380, "2:1")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);

        // Should match first BDB found with the DNS name (current implementation matches by host only)
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "redis.example.com");
        assertNotNull(result);
        assertEquals("1", result.getUid()); // First match wins
    }

    @Test
    void findMatchingBdb_matches_correct_bdb_with_same_ip_different_ports() {
        // Two BDBs with same IP but different ports and DNS names
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis1.example.com", 6379, "1:1")));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays
            .asList(new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis2.example.com", 6380, "2:1")));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);

        // Should match first BDB found with the IP address (current implementation matches by host only)
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "10.0.1.100");
        assertNotNull(result);
        assertEquals("1", result.getUid()); // First match wins
    }
}
