package redis.clients.jedis.commands.unified.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.commands.unified.GeoCommandsTestBase;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
