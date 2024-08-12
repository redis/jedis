package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

public class PipeliningBaseGeospatialCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testGeoadd() {
    when(commandObjects.geoadd("key", 13.361389, 38.115556, "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd("key", 13.361389, 38.115556, "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddBinary() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    byte[] member = "Sicily".getBytes();

    when(commandObjects.geoadd(key, longitude, latitude, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd(key, longitude, latitude, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMap() {
    Map<String, GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("member", new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd("key", memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd("key", memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMapBinary() {
    byte[] key = "location".getBytes();

    Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd(key, memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd(key, memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMapWithParams() {
    GeoAddParams params = new GeoAddParams();

    Map<String, GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("member", new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd("key", params, memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd("key", params, memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMapWithParamsBinary() {
    byte[] key = "location".getBytes();
    GeoAddParams params = GeoAddParams.geoAddParams();

    Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd(key, params, memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd(key, params, memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodist() {
    when(commandObjects.geodist("key", "member1", "member2")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist("key", "member1", "member2");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodistBinary() {
    byte[] key = "location".getBytes();
    byte[] member1 = "Palermo".getBytes();
    byte[] member2 = "Catania".getBytes();

    when(commandObjects.geodist(key, member1, member2)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist(key, member1, member2);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodistWithUnit() {
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geodist("key", "member1", "member2", unit)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist("key", "member1", "member2", unit);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodistWithUnitBinary() {
    byte[] key = "location".getBytes();
    byte[] member1 = "Palermo".getBytes();
    byte[] member2 = "Catania".getBytes();
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geodist(key, member1, member2, unit)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist(key, member1, member2, unit);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeohash() {
    when(commandObjects.geohash("key", "member1", "member2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.geohash("key", "member1", "member2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeohashBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();

    when(commandObjects.geohash(key, member)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.geohash(key, member);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeopos() {
    when(commandObjects.geopos("key", "member1", "member2")).thenReturn(listGeoCoordinateCommandObject);

    Response<List<GeoCoordinate>> response = pipeliningBase.geopos("key", "member1", "member2");

    assertThat(commands, contains(listGeoCoordinateCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoposBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();

    when(commandObjects.geopos(key, member)).thenReturn(listGeoCoordinateCommandObject);

    Response<List<GeoCoordinate>> response = pipeliningBase.geopos(key, member);

    assertThat(commands, contains(listGeoCoordinateCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradius() {
    when(commandObjects.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusBinary() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadius(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadius(key, longitude, latitude, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonly() {
    when(commandObjects.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonlyBinary() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusReadonly(key, longitude, latitude, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusWithParamBinary() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadius(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadius(key, longitude, latitude, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonlyWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonlyWithParamBinary() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusReadonly(key, longitude, latitude, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMember() {
    when(commandObjects.georadiusByMember("key", "member", 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusByMember("key", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadiusByMember(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMember(key, member, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonly() {
    when(commandObjects.georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonlyBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMemberReadonly(key, member, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMember("key", "member", 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .georadiusByMember("key", "member", 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberWithParamBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMember(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMember(key, member, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonlyWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonlyWithParamBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMemberReadonly(key, member, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusStore() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusStore("key", 15.0, 37.0, 100.0, GeoUnit.KM, param, storeParam))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .georadiusStore("key", 15.0, 37.0, 100.0, GeoUnit.KM, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusStoreBinary() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberStore() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusByMemberStore("key", "member", 100.0, GeoUnit.KM, param, storeParam))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .georadiusByMemberStore("key", "member", 100.0, GeoUnit.KM, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberStoreBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.georadiusByMemberStore(key, member, radius, unit, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberRadius() {
    when(commandObjects.geosearch("key", "member", 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .geosearch("key", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberRadiusBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, member, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordRadius() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearch("key", coord, 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch("key", coord, 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordRadiusBinary() {
    byte[] key = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, coord, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, coord, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberBox() {
    when(commandObjects.geosearch("key", "member", 50.0, 50.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .geosearch("key", "member", 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberBoxBinary() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, member, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, member, width, height, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordBox() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearch("key", coord, 50.0, 50.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .geosearch("key", coord, 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordBoxBinary() {
    byte[] key = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, coord, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, coord, width, height, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchWithParams() {
    GeoSearchParam params = new GeoSearchParam();

    when(commandObjects.geosearch("key", params)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch("key", params);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchWithParamsBinary() {
    byte[] key = "location".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM);

    when(commandObjects.geosearch(key, params)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, params);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberRadius() {
    when(commandObjects.geosearchStore("dest", "src", "member", 100.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .geosearchStore("dest", "src", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberRadiusBinary() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, member, radius, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, member, radius, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordRadius() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearchStore("dest", "src", coord, 100.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore("dest", "src", coord, 100.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordRadiusBinary() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, coord, radius, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, coord, radius, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberBox() {
    when(commandObjects.geosearchStore("dest", "src", "member", 50.0, 50.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .geosearchStore("dest", "src", "member", 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberBoxBinary() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, member, width, height, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, member, width, height, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordBox() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearchStore("dest", "src", coord, 50.0, 50.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .geosearchStore("dest", "src", coord, 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordBoxBinary() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, coord, width, height, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, coord, width, height, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreWithParams() {
    GeoSearchParam params = new GeoSearchParam();

    when(commandObjects.geosearchStore("dest", "src", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore("dest", "src", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreWithParamsBinary() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM);

    when(commandObjects.geosearchStore(dest, src, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreStoreDist() {
    GeoSearchParam params = new GeoSearchParam();

    when(commandObjects.geosearchStoreStoreDist("dest", "src", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStoreStoreDist("dest", "src", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreStoreDistBinary() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM);

    when(commandObjects.geosearchStoreStoreDist(dest, src, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStoreStoreDist(dest, src, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
