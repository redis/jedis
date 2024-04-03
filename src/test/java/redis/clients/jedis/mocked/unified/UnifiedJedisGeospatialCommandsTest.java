package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

public class UnifiedJedisGeospatialCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testGeoadd() {
    String key = "cities";
    double longitude = 13.361389;
    double latitude = 38.115556;
    String member = "Palermo";
    long expectedAdded = 1L;

    when(commandObjects.geoadd(key, longitude, latitude, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.geoadd(key, longitude, latitude, member);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geoadd(key, longitude, latitude, member);
  }

  @Test
  public void testGeoaddBinary() {
    byte[] key = "cities".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    byte[] member = "Palermo".getBytes();
    long expectedAdded = 1L;

    when(commandObjects.geoadd(key, longitude, latitude, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.geoadd(key, longitude, latitude, member);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geoadd(key, longitude, latitude, member);
  }

  @Test
  public void testGeoaddMap() {
    String key = "cities";
    Map<String, GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    memberCoordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
    long expectedAdded = 2L;

    when(commandObjects.geoadd(key, memberCoordinateMap)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.geoadd(key, memberCoordinateMap);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geoadd(key, memberCoordinateMap);
  }

  @Test
  public void testGeoaddMapBinary() {
    byte[] key = "cities".getBytes();
    Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));
    memberCoordinateMap.put("Catania".getBytes(), new GeoCoordinate(15.087269, 37.502669));
    long expectedAdded = 2L;

    when(commandObjects.geoadd(key, memberCoordinateMap)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.geoadd(key, memberCoordinateMap);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geoadd(key, memberCoordinateMap);
  }

  @Test
  public void testGeoaddMapWithParams() {
    String key = "cities";
    GeoAddParams params = GeoAddParams.geoAddParams().nx();
    Map<String, GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    long expectedAdded = 1L;

    when(commandObjects.geoadd(key, params, memberCoordinateMap)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.geoadd(key, params, memberCoordinateMap);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geoadd(key, params, memberCoordinateMap);
  }

  @Test
  public void testGeoaddMapWithParamsBinary() {
    byte[] key = "cities".getBytes();
    GeoAddParams params = GeoAddParams.geoAddParams().nx();
    Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));
    long expectedAdded = 1L;

    when(commandObjects.geoadd(key, params, memberCoordinateMap)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.geoadd(key, params, memberCoordinateMap);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geoadd(key, params, memberCoordinateMap);
  }

  @Test
  public void testGeodist() {
    String key = "cities";
    String member1 = "Palermo";
    String member2 = "Catania";
    Double expectedDistance = 166274.15156960033;

    when(commandObjects.geodist(key, member1, member2)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedDistance);

    Double result = jedis.geodist(key, member1, member2);

    assertThat(result, equalTo(expectedDistance));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).geodist(key, member1, member2);
  }

  @Test
  public void testGeodistBinary() {
    byte[] key = "cities".getBytes();
    byte[] member1 = "Palermo".getBytes();
    byte[] member2 = "Catania".getBytes();
    Double expectedDistance = 166274.15156960033;

    when(commandObjects.geodist(key, member1, member2)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedDistance);

    Double result = jedis.geodist(key, member1, member2);

    assertThat(result, equalTo(expectedDistance));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).geodist(key, member1, member2);
  }

  @Test
  public void testGeodistWithUnit() {
    String key = "cities";
    String member1 = "Palermo";
    String member2 = "Catania";
    GeoUnit unit = GeoUnit.KM;
    Double expectedDistance = 166.274;

    when(commandObjects.geodist(key, member1, member2, unit)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedDistance);

    Double result = jedis.geodist(key, member1, member2, unit);

    assertThat(result, equalTo(expectedDistance));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).geodist(key, member1, member2, unit);
  }

  @Test
  public void testGeodistWithUnitBinary() {
    byte[] key = "cities".getBytes();
    byte[] member1 = "Palermo".getBytes();
    byte[] member2 = "Catania".getBytes();
    GeoUnit unit = GeoUnit.KM;
    Double expectedDistance = 166.274;

    when(commandObjects.geodist(key, member1, member2, unit)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedDistance);

    Double result = jedis.geodist(key, member1, member2, unit);

    assertThat(result, equalTo(expectedDistance));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).geodist(key, member1, member2, unit);
  }

  @Test
  public void testGeohash() {
    String key = "cities";
    String[] members = { "Palermo", "Catania" };
    List<String> expectedHashes = Arrays.asList("sqc8b49rny0", "sqdtr74hyu0");

    when(commandObjects.geohash(key, members)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedHashes);

    List<String> result = jedis.geohash(key, members);

    assertThat(result, equalTo(expectedHashes));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).geohash(key, members);
  }

  @Test
  public void testGeohashBinary() {
    byte[] key = "cities".getBytes();
    byte[][] members = { "Palermo".getBytes(), "Catania".getBytes() };
    List<byte[]> expectedHashes = Arrays.asList("sqc8b49rny0".getBytes(), "sqdtr74hyu0".getBytes());

    when(commandObjects.geohash(key, members)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedHashes);

    List<byte[]> result = jedis.geohash(key, members);

    assertThat(result, equalTo(expectedHashes));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).geohash(key, members);
  }

  @Test
  public void testGeopos() {
    String key = "cities";
    String[] members = { "Palermo", "Catania" };
    List<GeoCoordinate> expectedPositions = Arrays.asList(
        new GeoCoordinate(13.361389, 38.115556),
        new GeoCoordinate(15.087269, 37.502669)
    );

    when(commandObjects.geopos(key, members)).thenReturn(listGeoCoordinateCommandObject);
    when(commandExecutor.executeCommand(listGeoCoordinateCommandObject)).thenReturn(expectedPositions);

    List<GeoCoordinate> result = jedis.geopos(key, members);

    assertThat(result, equalTo(expectedPositions));

    verify(commandExecutor).executeCommand(listGeoCoordinateCommandObject);
    verify(commandObjects).geopos(key, members);
  }

  @Test
  public void testGeoposBinary() {
    byte[] key = "cities".getBytes();
    byte[][] members = { "Palermo".getBytes(), "Catania".getBytes() };
    List<GeoCoordinate> expectedPositions = Arrays.asList(
        new GeoCoordinate(13.361389, 38.115556),
        new GeoCoordinate(15.087269, 37.502669)
    );

    when(commandObjects.geopos(key, members)).thenReturn(listGeoCoordinateCommandObject);
    when(commandExecutor.executeCommand(listGeoCoordinateCommandObject)).thenReturn(expectedPositions);

    List<GeoCoordinate> result = jedis.geopos(key, members);

    assertThat(result, equalTo(expectedPositions));

    verify(commandExecutor).executeCommand(listGeoCoordinateCommandObject);
    verify(commandObjects).geopos(key, members);
  }

  @Test
  public void testGeoradius() {
    String key = "cities";
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadius(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadius(key, longitude, latitude, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadius(key, longitude, latitude, radius, unit);
  }

  @Test
  public void testGeoradiusBinary() {
    byte[] key = "cities".getBytes();
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadius(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadius(key, longitude, latitude, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadius(key, longitude, latitude, radius, unit);
  }

  @Test
  public void testGeoradiusReadonly() {
    String key = "cities";
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusReadonly(key, longitude, latitude, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusReadonly(key, longitude, latitude, radius, unit);
  }

  @Test
  public void testGeoradiusReadonlyBinary() {
    byte[] key = "cities".getBytes();
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusReadonly(key, longitude, latitude, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusReadonly(key, longitude, latitude, radius, unit);
  }

  @Test
  public void testGeoradiusWithParam() {
    String key = "cities";
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadius(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadius(key, longitude, latitude, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadius(key, longitude, latitude, radius, unit, param);
  }

  @Test
  public void testGeoradiusWithParamBinary() {
    byte[] key = "cities".getBytes();
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadius(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadius(key, longitude, latitude, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadius(key, longitude, latitude, radius, unit, param);
  }

  @Test
  public void testGeoradiusReadonlyWithParam() {
    String key = "cities";
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusReadonly(key, longitude, latitude, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusReadonly(key, longitude, latitude, radius, unit, param);
  }

  @Test
  public void testGeoradiusReadonlyWithParamBinary() {
    byte[] key = "cities".getBytes();
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusReadonly(key, longitude, latitude, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusReadonly(key, longitude, latitude, radius, unit, param);
  }

  @Test
  public void testGeoradiusByMember() {
    String key = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMember(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMember(key, member, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMember(key, member, radius, unit);
  }

  @Test
  public void testGeoradiusByMemberBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMember(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMember(key, member, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMember(key, member, radius, unit);
  }

  @Test
  public void testGeoradiusByMemberReadonly() {
    String key = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMemberReadonly(key, member, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMemberReadonly(key, member, radius, unit);
  }

  @Test
  public void testGeoradiusByMemberReadonlyBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMemberReadonly(key, member, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMemberReadonly(key, member, radius, unit);
  }

  @Test
  public void testGeoradiusByMemberWithParam() {
    String key = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMember(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMember(key, member, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMember(key, member, radius, unit, param);
  }

  @Test
  public void testGeoradiusByMemberWithParamBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMember(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMember(key, member, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMember(key, member, radius, unit, param);
  }

  @Test
  public void testGeoradiusByMemberReadonlyWithParam() {
    String key = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMemberReadonly(key, member, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMemberReadonly(key, member, radius, unit, param);
  }

  @Test
  public void testGeoradiusByMemberReadonlyWithParamBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.georadiusByMemberReadonly(key, member, radius, unit, param);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).georadiusByMemberReadonly(key, member, radius, unit, param);
  }

  @Test
  public void testGeoradiusStore() {
    String key = "cities";
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam();
    long expectedStored = 2L;

    when(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).georadiusStore(key, longitude, latitude, radius, unit, param, storeParam);
  }

  @Test
  public void testGeoradiusStoreBinary() {
    byte[] key = "cities".getBytes();
    double longitude = 15.087269;
    double latitude = 37.502669;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam();
    long expectedStored = 2L;

    when(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).georadiusStore(key, longitude, latitude, radius, unit, param, storeParam);
  }

  @Test
  public void testGeoradiusByMemberStore() {
    String key = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam();
    long expectedStored = 2L;

    when(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.georadiusByMemberStore(key, member, radius, unit, param, storeParam);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).georadiusByMemberStore(key, member, radius, unit, param, storeParam);
  }

  @Test
  public void testGeoradiusByMemberStoreBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withDist();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam();
    long expectedStored = 2L;

    when(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.georadiusByMemberStore(key, member, radius, unit, param, storeParam);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).georadiusByMemberStore(key, member, radius, unit, param, storeParam);
  }

  @Test
  public void testGeosearchByMemberRadius() {
    String key = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, member, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, member, radius, unit);
  }

  @Test
  public void testGeosearchByMemberRadiusBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, member, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, member, radius, unit);
  }

  @Test
  public void testGeosearchKeyCoordRadius() {
    String key = "cities";
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, coord, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, coord, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, coord, radius, unit);
  }

  @Test
  public void testGeosearchKeyCoordRadiusBinary() {
    byte[] key = "cities".getBytes();
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, coord, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, coord, radius, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, coord, radius, unit);
  }

  @Test
  public void testGeosearchByMemberBox() {
    String key = "cities";
    String member = "Catania";
    double width = 100;
    double height = 200;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, member, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, member, width, height, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, member, width, height, unit);
  }

  @Test
  public void testGeosearchByMemberBoxBinary() {
    byte[] key = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double width = 150;
    double height = 75;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, member, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, member, width, height, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, member, width, height, unit);
  }

  @Test
  public void testGeosearchByCoordBox() {
    String key = "cities";
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double width = 100;
    double height = 200;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, coord, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, coord, width, height, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, coord, width, height, unit);
  }

  @Test
  public void testGeosearchByCoordBoxBinary() {
    byte[] key = "cities".getBytes();
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double width = 150;
    double height = 75;
    GeoUnit unit = GeoUnit.KM;
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, coord, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, coord, width, height, unit);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, coord, width, height, unit);
  }

  @Test
  public void testGeosearchWithParams() {
    String key = "cities";
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM).withCoord().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, params)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, params);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, params);
  }

  @Test
  public void testGeosearchWithParamsBinary() {
    byte[] key = "cities".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM).withCoord().withDist();
    List<GeoRadiusResponse> expectedResponses = new ArrayList<>();
    expectedResponses.add(new GeoRadiusResponse("Palermo".getBytes()));

    when(commandObjects.geosearch(key, params)).thenReturn(listGeoRadiusResponseCommandObject);
    when(commandExecutor.executeCommand(listGeoRadiusResponseCommandObject)).thenReturn(expectedResponses);

    List<GeoRadiusResponse> result = jedis.geosearch(key, params);

    assertThat(result, equalTo(expectedResponses));

    verify(commandExecutor).executeCommand(listGeoRadiusResponseCommandObject);
    verify(commandObjects).geosearch(key, params);
  }

  @Test
  public void testGeosearchStoreByMemberRadius() {
    String dest = "cities_store";
    String src = "cities";
    String member = "Catania";
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 2L;

    when(commandObjects.geosearchStore(dest, src, member, radius, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, member, radius, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, member, radius, unit);
  }

  @Test
  public void testGeosearchStoreByMemberRadiusBinary() {
    byte[] dest = "cities_store".getBytes();
    byte[] src = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, member, radius, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, member, radius, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, member, radius, unit);
  }

  @Test
  public void testGeosearchStoreByCoordRadius() {
    String dest = "cities_store";
    String src = "cities";
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 2L;

    when(commandObjects.geosearchStore(dest, src, coord, radius, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, coord, radius, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, coord, radius, unit);
  }

  @Test
  public void testGeosearchStoreByCoordRadiusBinary() {
    byte[] dest = "cities_store".getBytes();
    byte[] src = "cities".getBytes();
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, coord, radius, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, coord, radius, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, coord, radius, unit);
  }

  @Test
  public void testGeosearchStoreByMemberBox() {
    String dest = "cities_store";
    String src = "cities";
    String member = "Catania";
    double width = 150;
    double height = 75;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, member, width, height, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, member, width, height, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, member, width, height, unit);
  }

  @Test
  public void testGeosearchStoreByMemberBoxBinary() {
    byte[] dest = "cities_store".getBytes();
    byte[] src = "cities".getBytes();
    byte[] member = "Catania".getBytes();
    double width = 150;
    double height = 75;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, member, width, height, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, member, width, height, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, member, width, height, unit);
  }

  @Test
  public void testGeosearchStoreByCoordBox() {
    String dest = "cities_store";
    String src = "cities";
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double width = 150;
    double height = 75;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, coord, width, height, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, coord, width, height, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, coord, width, height, unit);
  }

  @Test
  public void testGeosearchStoreByCoordBoxBinary() {
    byte[] dest = "cities_store".getBytes();
    byte[] src = "cities".getBytes();
    GeoCoordinate coord = new GeoCoordinate(15.087269, 37.502669);
    double width = 150;
    double height = 75;
    GeoUnit unit = GeoUnit.KM;
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, coord, width, height, unit)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, coord, width, height, unit);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, coord, width, height, unit);
  }

  @Test
  public void testGeosearchStoreWithParams() {
    String dest = "cities_store";
    String src = "cities";
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM).withCoord().withDist();
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, params);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, params);
  }

  @Test
  public void testGeosearchStoreWithParamsBinary() {
    byte[] dest = "cities_store".getBytes();
    byte[] src = "cities".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM).withCoord().withDist();
    long expectedStored = 3L;

    when(commandObjects.geosearchStore(dest, src, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.geosearchStore(dest, src, params);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStore(dest, src, params);
  }

  @Test
  public void testGeosearchStoreStoreDist() {
    String dest = "cities_store";
    String src = "cities";
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM).withCoord().withDist();
    long expectedStoredDist = 3L;

    when(commandObjects.geosearchStoreStoreDist(dest, src, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredDist);

    long result = jedis.geosearchStoreStoreDist(dest, src, params);

    assertThat(result, equalTo(expectedStoredDist));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStoreStoreDist(dest, src, params);
  }

  @Test
  public void testGeosearchStoreStoreDistBinary() {
    byte[] dest = "cities_store".getBytes();
    byte[] src = "cities".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM).withCoord().withDist();
    long expectedStoredDist = 3L;

    when(commandObjects.geosearchStoreStoreDist(dest, src, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredDist);

    long result = jedis.geosearchStoreStoreDist(dest, src, params);

    assertThat(result, equalTo(expectedStoredDist));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).geosearchStoreStoreDist(dest, src, params);
  }

}
