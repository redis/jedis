package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class RedisRestAPITest {

    @Test
    void parseBdbInfoFromResponse_parses_correctly() {
        String responseBody = "[\n" +
            "    {\n" +
            "        \"uid\": \"1\",\n" +
            "        \"endpoints\": [\n" +
            "            {\n" +
            "                \"dns_name\": \"redis-db1.example.com\",\n" +
            "                \"addr\": [\"10.0.1.100\"],\n" +
            "                \"port\": 6379,\n" +
            "                \"uid\": \"1:1\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"uid\": \"2\",\n" +
            "        \"endpoints\": [\n" +
            "            {\n" +
            "                \"dns_name\": \"redis-db2.example.com\",\n" +
            "                \"addr\": [\"10.0.1.101\"],\n" +
            "                \"port\": 6380,\n" +
            "                \"uid\": \"2:1\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "]";

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
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis-db1.example.com", 6379, "1:1")
        ));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.101"), "redis-db2.example.com", 6380, "2:1")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "redis-db2.example.com");

        assertNotNull(result);
        assertEquals("2", result.getUid());
    }

    @Test
    void findMatchingBdb_matches_ip_address() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100", "192.168.1.100"), "redis-db1.example.com", 6379, "1:1")
        ));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.101"), "redis-db2.example.com", 6380, "2:1")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "192.168.1.100");

        assertNotNull(result);
        assertEquals("1", result.getUid());
    }

    @Test
    void findMatchingBdb_returns_null_when_no_match() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis-db1.example.com", 6379, "1:1")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "nonexistent.example.com");

        assertNull(result);
    }

    @Test
    void findMatchingBdb_handles_multiple_endpoints_per_bdb() {
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis-db1-primary.example.com", 6379, "1:1"),
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.101"), "redis-db1-replica.example.com", 6380, "1:2")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1);
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "redis-db1-replica.example.com");

        assertNotNull(result);
        assertEquals("1", result.getUid());
    }

    @Test
    void parseBdbInfoFromResponse_handles_missing_fields_gracefully() {
        String responseBody = "[\n" +
            "    {\n" +
            "        \"uid\": \"1\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"endpoints\": [\n" +
            "            {\n" +
            "                \"dns_name\": \"redis-db2.example.com\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"uid\": \"3\",\n" +
            "        \"endpoints\": [\n" +
            "            {\n" +
            "                \"dns_name\": \"redis-db3.example.com\",\n" +
            "                \"addr\": [\"10.0.1.103\"],\n" +
            "                \"port\": 6379\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "]";

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
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "target-host.example.com", 6379, "1:1")
        ));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("target-host.example.com"), "other-host.example.com", 6380, "2:1")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);

        // Should match BDB 1 by dns_name, not BDB 2 by addr
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "target-host.example.com");
        assertNotNull(result);
        assertEquals("1", result.getUid());
    }

    @Test
    void findMatchingBdb_matches_correct_bdb_with_same_host_different_ports() {
        // Two BDBs with same DNS name but different ports
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis.example.com", 6379, "1:1")
        ));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis.example.com", 6380, "2:1")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);

        // Should match first BDB found with the DNS name (current implementation matches by host only)
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "redis.example.com");
        assertNotNull(result);
        assertEquals("1", result.getUid()); // First match wins
    }

    @Test
    void findMatchingBdb_matches_correct_bdb_with_same_ip_different_ports() {
        // Two BDBs with same IP but different ports and DNS names
        RedisRestAPI.BdbInfo bdb1 = new RedisRestAPI.BdbInfo("1", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis1.example.com", 6379, "1:1")
        ));
        RedisRestAPI.BdbInfo bdb2 = new RedisRestAPI.BdbInfo("2", Arrays.asList(
            new RedisRestAPI.EndpointInfo(Arrays.asList("10.0.1.100"), "redis2.example.com", 6380, "2:1")
        ));

        List<RedisRestAPI.BdbInfo> bdbs = Arrays.asList(bdb1, bdb2);

        // Should match first BDB found with the IP address (current implementation matches by host only)
        RedisRestAPI.BdbInfo result = RedisRestAPI.BdbInfo.findMatchingBdb(bdbs, "10.0.1.100");
        assertNotNull(result);
        assertEquals("1", result.getUid()); // First match wins
    }
}
