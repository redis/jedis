//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import redis.clients.jedis.GeoCoordinate;
//import redis.clients.jedis.args.GeoUnit;
//import redis.clients.jedis.params.GeoRadiusParam;
//import redis.clients.jedis.params.GeoRadiusStoreParam;
//import redis.clients.jedis.commands.unified.GeoCommandsTestBase;
//
//public class ClusterGeoCommandsTest extends GeoCommandsTestBase {
//
//  @BeforeClass
//  public static void prepare() throws InterruptedException {
//    jedis = ClusterCommandsTestHelper.initAndGetCluster();
//  }
//
//  @AfterClass
//  public static void closeCluster() {
//    jedis.close();
//  }
//
//  @AfterClass
//  public static void resetCluster() {
//    ClusterCommandsTestHelper.tearClusterDown();
//  }
//
//  @Before
//  public void setUp() {
//    ClusterCommandsTestHelper.clearClusterData();
//  }
//
//  @Test
//  @Override
//  public void georadiusStore() {
//    // prepare datas
//    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
//    coordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
//    coordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
//    jedis.geoadd("Sicily {ITA}", coordinateMap);
//
//    long size = jedis.georadiusStore("Sicily {ITA}", 15, 37, 200, GeoUnit.KM,
//      GeoRadiusParam.geoRadiusParam(),
//      GeoRadiusStoreParam.geoRadiusStoreParam().store("{ITA} SicilyStore"));
//    assertEquals(2, size);
//    List<String> expected = new ArrayList<>();
//    expected.add("Palermo");
//    expected.add("Catania");
//    assertEquals(expected, jedis.zrange("{ITA} SicilyStore", 0, -1));
//  }
//
//  @Ignore
//  @Override
//  public void georadiusStoreBinary() {
//  }
//
//  @Test
//  @Override
//  public void georadiusByMemberStore() {
//    jedis.geoadd("Sicily {ITA}", 13.583333, 37.316667, "Agrigento");
//    jedis.geoadd("Sicily {ITA}", 13.361389, 38.115556, "Palermo");
//    jedis.geoadd("Sicily {ITA}", 15.087269, 37.502669, "Catania");
//
//    long size = jedis.georadiusByMemberStore("Sicily {ITA}", "Agrigento", 100, GeoUnit.KM,
//      GeoRadiusParam.geoRadiusParam(),
//      GeoRadiusStoreParam.geoRadiusStoreParam().store("{ITA} SicilyStore"));
//    assertEquals(2, size);
//    List<String> expected = new ArrayList<>();
//    expected.add("Agrigento");
//    expected.add("Palermo");
//    assertEquals(expected, jedis.zrange("{ITA} SicilyStore", 0, -1));
//  }
//
//  @Ignore
//  @Override
//  public void georadiusByMemberStoreBinary() {
//  }
//}
