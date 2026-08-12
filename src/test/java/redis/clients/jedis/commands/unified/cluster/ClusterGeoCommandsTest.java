package redis.clients.jedis.commands.unified.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.commands.unified.GeoCommandsTestBase;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.util.SafeEncoder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterGeoCommandsTest extends GeoCommandsTestBase {

  public ClusterGeoCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return  ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Test
  @Override
  public void georadiusStore() {
    // prepare datas
    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    coordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd("Sicily {ITA}", coordinateMap);

    long size = jedis.georadiusStore("Sicily {ITA}", 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("{ITA} SicilyStore"));
    assertEquals(2, size);
    List<String> expected = new ArrayList<>();
    expected.add("Palermo");
    expected.add("Catania");
    assertEquals(expected, jedis.zrange("{ITA} SicilyStore", 0, -1));
  }

  @Disabled
  @Override
  public void georadiusStoreBinary() {
  }

  @Test
  @Override
  public void georadiusByMemberStore() {
    jedis.geoadd("Sicily {ITA}", 13.583333, 37.316667, "Agrigento");
    jedis.geoadd("Sicily {ITA}", 13.361389, 38.115556, "Palermo");
    jedis.geoadd("Sicily {ITA}", 15.087269, 37.502669, "Catania");

    long size = jedis.georadiusByMemberStore("Sicily {ITA}", "Agrigento", 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("{ITA} SicilyStore"));
    assertEquals(2, size);
    List<String> expected = new ArrayList<>();
    expected.add("Agrigento");
    expected.add("Palermo");
    assertEquals(expected, jedis.zrange("{ITA} SicilyStore", 0, -1));
  }

  @Disabled
  @Override
  public void georadiusByMemberStoreBinary() {
  }

  @Disabled
  @Override
  public void geosearchstore() {
  }

  @Disabled
  @Override
  public void geosearchstoreWithdist() {
  }

  @Test
  public void geosearchStoreCrossSlotKeys() {
    // "tel-aviv" (slot 8900) and "barcelona" (slot 3776); rejected client-side because the source
    // key is registered for slot computation, not only the destination
    GeoCoordinate coord = new GeoCoordinate(2.191, 41.433);
    GeoSearchParam params = new GeoSearchParam().fromMember("place3").byRadius(100, GeoUnit.KM);

    assertCrossSlotRejected(() -> jedis.geosearchStore("tel-aviv", "barcelona", "place3", 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore("tel-aviv", "barcelona", coord, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore("tel-aviv", "barcelona", "place3", 100, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore("tel-aviv", "barcelona", coord, 100, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore("tel-aviv", "barcelona", params));
    assertCrossSlotRejected(() -> jedis.geosearchStoreStoreDist("tel-aviv", "barcelona", params));

    byte[] bdest = SafeEncoder.encode("tel-aviv");
    byte[] bsrc = SafeEncoder.encode("barcelona");
    byte[] bmember = SafeEncoder.encode("place3");

    assertCrossSlotRejected(() -> jedis.geosearchStore(bdest, bsrc, bmember, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore(bdest, bsrc, coord, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore(bdest, bsrc, bmember, 100, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore(bdest, bsrc, coord, 100, 100, GeoUnit.KM));
    assertCrossSlotRejected(() -> jedis.geosearchStore(bdest, bsrc, params));
    assertCrossSlotRejected(() -> jedis.geosearchStoreStoreDist(bdest, bsrc, params));
  }

  private static void assertCrossSlotRejected(Executable command) {
    JedisClusterOperationException e = assertThrows(JedisClusterOperationException.class, command);
    assertThat(e.getMessage(), containsString("multiple hash slots"));
  }
}
