package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Pins the `rladmin status` parse offline. This is the one piece of the Redis Enterprise Sentinel
 * scenario suite that can regress silently - a column-order change would otherwise only show up as
 * a confusing failure against a live cluster.
 * <p>
 * Named {@code *UnitTests} rather than {@code *Test} because surefire excludes
 * {@code **}{@code /scenario/*Test.java}.
 */
@Tag("unit")
public class RedisEnterpriseClusterStatusUnitTests {

  /**
   * Shaped after a real {@code rladmin status}: three databases, one of which loads modules. Addresses are from the
   * documentation ranges reserved by RFC 5737 and RFC 1918, so nothing here points at a real cluster.
   */
  private static final String STATUS = "CLUSTER NODES:\n"
      + "NODE:ID ROLE   ADDRESS     EXTERNAL_ADDRESS HOSTNAME       SHARDS CORES VERSION   STATUS\n"
      + "*node:1 master 10.0.0.1    192.0.2.1        ip-10-0-0-1    3/100  2     8.0.22-44 OK\n"
      + "node:2  slave  10.0.0.2    192.0.2.2        ip-10-0-0-2    3/100  2     8.0.22-44 OK\n"
      + "node:3  slave  10.0.0.3    192.0.2.3        ip-10-0-0-3    1/100  2     8.0.22-44 OK\n"
      + "\n" + "DATABASES:\n" + "DB:ID NAME                        TYPE  MODULE STATUS ENDPOINT\n"
      + "db:1  re-standalone               redis search active redis-12000.example:12000\n"
      + "db:2  m-standard                  redis        active redis-12003.example:12003\n"
      + "db:3  re-single-shard-oss-cluster redis        active redis-12001.example:12001\n" + "\n"
      + "ENDPOINTS:\n"
      + "DB:ID NAME                        ID           NODE   ROLE              SSL\n"
      + "db:1  re-standalone               endpoint:1:1 node:2 all-master-shards No\n"
      + "db:2  m-standard                  endpoint:2:1 node:3 single            No\n"
      + "db:3  re-single-shard-oss-cluster endpoint:3:1 node:3 all-master-shards No\n" + "\n"
      + "SHARDS:\n" + "DB:ID NAME       ID      NODE   ROLE   SLOTS     USED_MEMORY STATUS\n"
      + "db:2  m-standard redis:2 node:3 master 0-8191    2.5MB       OK\n"
      + "db:2  m-standard redis:3 node:1 slave  0-8191    2.5MB       OK\n";

  @Test
  public void parsesTheNameEndpointAndBoundNodeOfTheRequestedDatabase() {
    RedisEnterpriseClusterStatus status = RedisEnterpriseClusterStatus.parse(STATUS, "2");

    assertEquals("m-standard", status.getDbName());
    assertEquals("2:1", status.getEndpointUid());
    assertEquals("3", status.getBoundNodeUid());
  }

  @Test
  public void picksTheRequestedDatabaseRatherThanTheFirstRow() {
    RedisEnterpriseClusterStatus first = RedisEnterpriseClusterStatus.parse(STATUS, "1");
    assertEquals("re-standalone", first.getDbName());
    assertEquals("1:1", first.getEndpointUid());
    assertEquals("2", first.getBoundNodeUid());

    RedisEnterpriseClusterStatus third = RedisEnterpriseClusterStatus.parse(STATUS, "3");
    assertEquals("re-single-shard-oss-cluster", third.getDbName());
    assertEquals("3:1", third.getEndpointUid());
  }

  @Test
  public void readsTheNameFromDatabasesAndNotFromShardsOrEndpoints() {
    // A db:<id> row appears in DATABASES, ENDPOINTS and SHARDS alike. Only sectioning keeps the
    // name from being read off a SHARDS row, where column 2 happens to be the name as well but
    // column 3 is a shard id rather than an endpoint id.
    RedisEnterpriseClusterStatus status = RedisEnterpriseClusterStatus.parse(STATUS, "2");
    assertEquals("m-standard", status.getDbName());
    assertTrue(status.getRawStatus().contains("SHARDS:"));
  }

  @Test
  public void failsLoudlyWhenTheDatabaseIsAbsent() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> RedisEnterpriseClusterStatus.parse(STATUS, "99"));

    // The raw output has to travel with the failure, otherwise a format drift is undiagnosable.
    assertTrue(e.getMessage().contains("bdb 99"));
    assertTrue(e.getMessage().contains("ENDPOINTS:"));
  }

  @Test
  public void failsLoudlyOnEmptyOutput() {
    assertThrows(IllegalStateException.class, () -> RedisEnterpriseClusterStatus.parse("", "2"));
    assertThrows(IllegalStateException.class, () -> RedisEnterpriseClusterStatus.parse(null, "2"));
  }
}
