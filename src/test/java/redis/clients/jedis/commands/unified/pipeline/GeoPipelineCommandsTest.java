package redis.clients.jedis.commands.unified.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.util.GeoCoordinateMatcher.atCoordinates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class GeoPipelineCommandsTest extends PipelineCommandsTestBase {

  protected final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  protected final byte[] bA = { 0x0A };
  protected final byte[] bB = { 0x0B };
  protected final byte[] bC = { 0x0C };
  protected final byte[] bNotexist = { 0x0F };

  private static final double EPSILON = 1e-5;

  public GeoPipelineCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void geoadd() {
    pipe.geoadd("foo", 1, 2, "a");
    pipe.geoadd("foo", 2, 3, "a");

    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("a", new GeoCoordinate(3, 4));
    coordinateMap.put("b", new GeoCoordinate(2, 3));
    coordinateMap.put("c", new GeoCoordinate(3.314, 2.3241));

    pipe.geoadd("foo", coordinateMap);

    // binary
    pipe.geoadd(bfoo, 1, 2, bA);
    pipe.geoadd(bfoo, 2, 3, bA);

    Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<>();
    bcoordinateMap.put(bA, new GeoCoordinate(3, 4));
    bcoordinateMap.put(bB, new GeoCoordinate(2, 3));
    bcoordinateMap.put(bC, new GeoCoordinate(3.314, 2.3241));

    pipe.geoadd(bfoo, bcoordinateMap);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        0L,
        2L,
        1L,
        0L,
        2L
    ));
  }

  @Test
  public void geoaddWithParams() {
    pipe.geoadd("foo", 1, 2, "a");

    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("a", new GeoCoordinate(3, 4));
    pipe.geoadd("foo", GeoAddParams.geoAddParams().nx(), coordinateMap);
    pipe.geoadd("foo", GeoAddParams.geoAddParams().xx().ch(), coordinateMap);

    coordinateMap.clear();
    coordinateMap.put("b", new GeoCoordinate(6, 7));
    // never add elements.
    pipe.geoadd("foo", GeoAddParams.geoAddParams().xx(), coordinateMap);
    pipe.geoadd("foo", GeoAddParams.geoAddParams().nx(), coordinateMap);

    // binary
    pipe.geoadd(bfoo, 1, 2, bA);

    Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<>();
    bcoordinateMap.put(bA, new GeoCoordinate(3, 4));
    pipe.geoadd(bfoo, GeoAddParams.geoAddParams().nx(), bcoordinateMap);
    pipe.geoadd(bfoo, GeoAddParams.geoAddParams().xx().ch(), bcoordinateMap);

    bcoordinateMap.clear();
    bcoordinateMap.put(bB, new GeoCoordinate(6, 7));
    // never add elements.
    pipe.geoadd(bfoo, GeoAddParams.geoAddParams().xx(), bcoordinateMap);
    pipe.geoadd(bfoo, GeoAddParams.geoAddParams().nx(), bcoordinateMap);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        0L,
        1L,
        0L,
        1L,
        1L,
        0L,
        1L,
        0L,
        1L
    ));
  }

  @Test
  public void geodist() {
    prepareGeoData();

    Response<Double> dist1 = pipe.geodist("foo", "a", "b");
    Response<Double> dist2 = pipe.geodist("foo", "a", "b", GeoUnit.KM);
    Response<Double> dist3 = pipe.geodist("foo", "a", "b", GeoUnit.MI);
    Response<Double> dist4 = pipe.geodist("foo", "a", "b", GeoUnit.FT);

    // binary
    Response<Double> dist5 = pipe.geodist(bfoo, bA, bB);
    Response<Double> dist6 = pipe.geodist(bfoo, bA, bB, GeoUnit.KM);
    Response<Double> dist7 = pipe.geodist(bfoo, bA, bB, GeoUnit.MI);
    Response<Double> dist8 = pipe.geodist(bfoo, bA, bB, GeoUnit.FT);

    pipe.sync();

    assertThat(dist1.get(), closeTo(157149.0, 1.0));
    assertThat(dist2.get(), closeTo(157.0, 1.0));
    assertThat(dist3.get(), closeTo(97.0, 1.0));
    assertThat(dist4.get(), closeTo(515583.0, 1.0));
    assertThat(dist5.get(), closeTo(157149.0, 1.0));
    assertThat(dist6.get(), closeTo(157.0, 1.0));
    assertThat(dist7.get(), closeTo(97.0, 1.0));
    assertThat(dist8.get(), closeTo(515583.0, 1.0));
  }

  @Test
  public void geohash() {
    prepareGeoData();

    Response<List<String>> hashes = pipe.geohash("foo", "a", "b", "notexist");
    Response<List<byte[]>> bhashes = pipe.geohash(bfoo, bA, bB, bNotexist);

    pipe.sync();

    assertThat(hashes.get(), contains(
        "s0dnu20t9j0",
        "s093jd0k720",
        null
    ));

    assertThat(bhashes.get(), contains(
        SafeEncoder.encode("s0dnu20t9j0"),
        SafeEncoder.encode("s093jd0k720"),
        null
    ));
  }

  @Test
  public void geopos() {
    prepareGeoData();

    Response<List<GeoCoordinate>> coordinates = pipe.geopos("foo", "a", "b", "notexist");
    Response<List<GeoCoordinate>> bcoordinates = pipe.geopos(bfoo, bA, bB, bNotexist);

    pipe.sync();

    assertThat(coordinates.get(), contains(
        atCoordinates(3.0, 4.0),
        atCoordinates(2.0, 3.0),
        null
    ));

    assertThat(bcoordinates.get(), contains(
        atCoordinates(3.0, 4.0),
        atCoordinates(2.0, 3.0),
        null
    ));
  }

  @Test
  public void georadius() {
    // prepare data
    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    coordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd("Sicily", coordinateMap);

    Response<List<GeoRadiusResponse>> members1 = pipe.georadius("Sicily", 15, 37, 200, GeoUnit.KM);

    // sort
    Response<List<GeoRadiusResponse>> members2 = pipe.georadius("Sicily", 15, 37, 200,
        GeoUnit.KM, GeoRadiusParam.geoRadiusParam().sortDescending());

    // sort, count 1
    Response<List<GeoRadiusResponse>> members3 = pipe.georadius("Sicily", 15, 37, 200,
        GeoUnit.KM, GeoRadiusParam.geoRadiusParam().sortAscending().count(1));

    // sort, count 1, withdist, withcoord
    Response<List<GeoRadiusResponse>> members4 = pipe.georadius("Sicily", 15, 37, 200,
        GeoUnit.KM, GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist().withHash());

    // sort, count 1, with hash
    Response<List<GeoRadiusResponse>> members5 = pipe.georadius("Sicily", 15, 37, 200,
        GeoUnit.KM, GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withHash());

    // sort, count 1, any
    Response<List<GeoRadiusResponse>> members6 = pipe.georadius("Sicily", 15, 37, 200,
        GeoUnit.KM, GeoRadiusParam.geoRadiusParam().sortDescending().count(1, true));

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder("Palermo", "Catania"));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Palermo", "Catania"));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Catania"));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));

    assertThat(members4.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Catania"));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(56.4413, EPSILON)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(15.087269, 37.502669)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(3479447370796909L));

    assertThat(members5.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Catania"));
    assertThat(members5.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members5.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members5.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(3479447370796909L));

    assertThat(members6.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        anyOf(contains("Catania"), contains("Palermo")));
    assertThat(members6.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members6.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members6.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusStore() {
    // prepare data
    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    coordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd("Sicily", coordinateMap);

    Response<Long> size = pipe.georadiusStore("Sicily", 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("SicilyStore"));

    Response<List<String>> items = pipe.zrange("SicilyStore", 0, -1);

    pipe.sync();

    assertThat(size.get(), equalTo(2L));
    assertThat(items.get(), contains("Palermo", "Catania"));
  }

  @Test
  public void georadiusReadonly() {
    // prepare data
    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("Palermo", new GeoCoordinate(13.361389, 38.115556));
    coordinateMap.put("Catania", new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd("Sicily", coordinateMap);

    Response<List<GeoRadiusResponse>> members1 = pipe.georadiusReadonly("Sicily", 15, 37, 200, GeoUnit.KM);

    // sort
    Response<List<GeoRadiusResponse>> members2 = pipe.georadiusReadonly("Sicily", 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    // sort, count 1
    Response<List<GeoRadiusResponse>> members3 = pipe.georadiusReadonly("Sicily", 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1));

    // sort, count 1, withdist, withcoord
    Response<List<GeoRadiusResponse>> members4 = pipe.georadiusReadonly("Sicily", 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder("Palermo", "Catania"));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Catania", "Palermo"));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Catania"));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));

    assertThat(members4.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Catania"));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(56.4413, EPSILON)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(15.087269, 37.502669)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusBinary() {
    // prepare data
    Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<>();
    bcoordinateMap.put(bA, new GeoCoordinate(13.361389, 38.115556));
    bcoordinateMap.put(bB, new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd(bfoo, bcoordinateMap);

    Response<List<GeoRadiusResponse>> members1 = pipe.georadius(bfoo, 15, 37, 200, GeoUnit.KM);

    // sort
    Response<List<GeoRadiusResponse>> members2 = pipe.georadius(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    // sort, count 1
    Response<List<GeoRadiusResponse>> members3 = pipe.georadius(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1));

    // sort, count 1, withdist, withcoord
    Response<List<GeoRadiusResponse>> members4 = pipe.georadius(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(bA, bB));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bB, bA));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bB));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));

    assertThat(members4.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bB));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(56.4413, EPSILON)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(15.087269, 37.502669)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusStoreBinary() {
    // prepare data
    Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<>();
    bcoordinateMap.put(bA, new GeoCoordinate(13.361389, 38.115556));
    bcoordinateMap.put(bB, new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd(bfoo, bcoordinateMap);

    Response<Long> size = pipe.georadiusStore(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("SicilyStore"));

    Response<List<byte[]>> items = pipe.zrange("SicilyStore".getBytes(), 0, -1);

    pipe.sync();

    assertThat(size.get(), equalTo(2L));
    assertThat(items.get(), contains(bA, bB));
  }

  @Test
  public void georadiusReadonlyBinary() {
    // prepare data
    Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<>();
    bcoordinateMap.put(bA, new GeoCoordinate(13.361389, 38.115556));
    bcoordinateMap.put(bB, new GeoCoordinate(15.087269, 37.502669));
    jedis.geoadd(bfoo, bcoordinateMap);

    Response<List<GeoRadiusResponse>> members1 = pipe.georadiusReadonly(bfoo, 15, 37, 200, GeoUnit.KM);

    // sort
    Response<List<GeoRadiusResponse>> members2 = pipe.georadiusReadonly(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    // sort, count 1
    Response<List<GeoRadiusResponse>> members3 = pipe.georadiusReadonly(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1));

    // sort, count 1, withdist, withcoord
    Response<List<GeoRadiusResponse>> members4 = pipe.georadiusReadonly(bfoo, 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(bA, bB));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bB, bA));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bB));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));

    assertThat(members4.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bB));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(56.4413, EPSILON)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(15.087269, 37.502669)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusByMember() {
    jedis.geoadd("Sicily", 13.583333, 37.316667, "Agrigento");
    jedis.geoadd("Sicily", 13.361389, 38.115556, "Palermo");
    jedis.geoadd("Sicily", 15.087269, 37.502669, "Catania");

    Response<List<GeoRadiusResponse>> members1 = pipe.georadiusByMember("Sicily", "Agrigento", 100,
        GeoUnit.KM);

    Response<List<GeoRadiusResponse>> members2 = pipe.georadiusByMember("Sicily", "Agrigento", 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    Response<List<GeoRadiusResponse>> members3 = pipe.georadiusByMember("Sicily", "Agrigento", 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder("Agrigento", "Palermo"));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Agrigento", "Palermo"));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Agrigento"));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(13.583333, 37.316667)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusByMemberStore() {
    jedis.geoadd("Sicily", 13.583333, 37.316667, "Agrigento");
    jedis.geoadd("Sicily", 13.361389, 38.115556, "Palermo");
    jedis.geoadd("Sicily", 15.087269, 37.502669, "Catania");

    Response<Long> size = pipe.georadiusByMemberStore("Sicily", "Agrigento", 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("SicilyStore"));

    Response<List<String>> items = pipe.zrange("SicilyStore", 0, -1);

    pipe.sync();

    assertThat(size.get(), equalTo(2L));
    assertThat(items.get(), contains("Agrigento", "Palermo"));
  }

  @Test
  public void georadiusByMemberReadonly() {
    jedis.geoadd("Sicily", 13.583333, 37.316667, "Agrigento");
    jedis.geoadd("Sicily", 13.361389, 38.115556, "Palermo");
    jedis.geoadd("Sicily", 15.087269, 37.502669, "Catania");

    Response<List<GeoRadiusResponse>> members1 = pipe.georadiusByMemberReadonly("Sicily", "Agrigento", 100,
        GeoUnit.KM);

    Response<List<GeoRadiusResponse>> members2 = pipe.georadiusByMemberReadonly("Sicily", "Agrigento", 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    Response<List<GeoRadiusResponse>> members3 = pipe.georadiusByMemberReadonly("Sicily", "Agrigento", 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder("Agrigento", "Palermo"));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Agrigento", "Palermo"));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("Agrigento"));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(13.583333, 37.316667)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusByMemberBinary() {
    jedis.geoadd(bfoo, 13.583333, 37.316667, bA);
    jedis.geoadd(bfoo, 13.361389, 38.115556, bB);
    jedis.geoadd(bfoo, 15.087269, 37.502669, bC);

    Response<List<GeoRadiusResponse>> members1 = pipe.georadiusByMember(bfoo, bA, 100, GeoUnit.KM);

    Response<List<GeoRadiusResponse>> members2 = pipe.georadiusByMember(bfoo, bA, 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    Response<List<GeoRadiusResponse>> members3 = pipe.georadiusByMember(bfoo, bA, 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(bA, bB));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bA, bB));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bA));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(13.583333, 37.316667)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void georadiusByMemberStoreBinary() {
    jedis.geoadd(bfoo, 13.583333, 37.316667, bA);
    jedis.geoadd(bfoo, 13.361389, 38.115556, bB);
    jedis.geoadd(bfoo, 15.087269, 37.502669, bC);

    Response<Long> size = pipe.georadiusByMemberStore(bfoo, bA, 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("SicilyStore"));

    Response<List<byte[]>> items = pipe.zrange("SicilyStore".getBytes(), 0, -1);

    pipe.sync();

    assertThat(size.get(), equalTo(2L));
    assertThat(items.get(), contains(bA, bB));
  }

  @Test
  public void georadiusByMemberReadonlyBinary() {
    jedis.geoadd(bfoo, 13.583333, 37.316667, bA);
    jedis.geoadd(bfoo, 13.361389, 38.115556, bB);
    jedis.geoadd(bfoo, 15.087269, 37.502669, bC);

    Response<List<GeoRadiusResponse>> members1 = pipe.georadiusByMemberReadonly(bfoo, bA, 100, GeoUnit.KM);

    Response<List<GeoRadiusResponse>> members2 = pipe.georadiusByMemberReadonly(bfoo, bA, 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending());

    Response<List<GeoRadiusResponse>> members3 = pipe.georadiusByMemberReadonly(bfoo, bA, 100, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam().sortAscending().count(1).withCoord().withDist());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(bA, bB));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bA, bB));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(0.0, EPSILON)));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue(), nullValue()));
    assertThat(members2.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L, 0L));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(bA));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(13.583333, 37.316667)));
    assertThat(members3.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test
  public void geosearch() {
    jedis.geoadd("barcelona", 2.1909389952632d, 41.433791470673d, "place1");
    jedis.geoadd("barcelona", 2.1873744593677d, 41.406342043777d, "place2");
    jedis.geoadd("barcelona", 2.583333d, 41.316667d, "place3");

    // FROMLONLAT and BYRADIUS
    Response<List<GeoRadiusResponse>> members1 = pipe.geosearch("barcelona",
        new GeoCoordinate(2.191d, 41.433d), 1000, GeoUnit.M);

    // using Params
    Response<List<GeoRadiusResponse>> members2 = pipe.geosearch("barcelona", new GeoSearchParam().byRadius(3000, GeoUnit.M)
        .fromLonLat(2.191d, 41.433d).desc());

    // FROMMEMBER and BYRADIUS
    Response<List<GeoRadiusResponse>> members3 = pipe.geosearch("barcelona", "place3", 100, GeoUnit.KM);

    // using Params
    Response<List<GeoRadiusResponse>> members4 = pipe.geosearch("barcelona", new GeoSearchParam().fromMember("place1")
        .byRadius(100, GeoUnit.KM).withDist().withCoord().withHash().count(2));

    // FROMMEMBER and BYBOX
    Response<List<GeoRadiusResponse>> members5 = pipe.geosearch("barcelona", "place3", 100, 100, GeoUnit.KM);

    // using Params
    Response<List<GeoRadiusResponse>> members6 = pipe.geosearch("barcelona", new GeoSearchParam().fromMember("place3")
        .byBox(100, 100, GeoUnit.KM).asc().count(1, true));

    // FROMLONLAT and BYBOX
    Response<List<GeoRadiusResponse>> members7 = pipe.geosearch("barcelona", new GeoCoordinate(2.191, 41.433),
        1, 1, GeoUnit.KM);

    // using Params
    Response<List<GeoRadiusResponse>> members8 = pipe.geosearch("barcelona", new GeoSearchParam().byBox(1, 1, GeoUnit.KM)
        .fromLonLat(2.191, 41.433).withDist().withCoord());

    pipe.sync();

    assertThat(members1.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place1"));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON)));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(nullValue()));
    assertThat(members1.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));

    assertThat(members2.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place2", "place1"));

    assertThat(members3.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place2", "place1", "place3"));

    assertThat(members4.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place1", "place2"));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0, EPSILON), closeTo(3.0674, EPSILON)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(2.1909389952632d, 41.433791470673d), atCoordinates(2.1873744593677d, 41.406342043777d)));
    assertThat(members4.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(3471609698139488L, 3471609625421029L));

    assertThat(members5.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place2", "place1", "place3"));

    assertThat(members6.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place2"));

    assertThat(members7.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place1"));

    assertThat(members8.get().stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains("place1"));
    assertThat(members8.get().stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        contains(closeTo(0.0881, EPSILON)));
    assertThat(members8.get().stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        contains(atCoordinates(2.1909389952632d, 41.433791470673d)));
    assertThat(members8.get().stream().map(GeoRadiusResponse::getRawScore).collect(Collectors.toList()),
        contains(0L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void geosearchSearchParamCombineFromMemberAndFromLonLat() {
    pipe.geosearch("barcelona", new GeoSearchParam().fromMember("foobar").fromLonLat(10, 10));
  }

  @Test(expected = IllegalArgumentException.class)
  public void geosearchSearchParamWithoutFromMemberAndFromLonLat() {
    pipe.geosearch("barcelona", new GeoSearchParam().byRadius(10, GeoUnit.MI));
  }

  @Test(expected = IllegalArgumentException.class)
  public void geosearchSearchParamCombineByRadiousAndByBox() {
    pipe.geosearch("barcelona", new GeoSearchParam().byRadius(3000, GeoUnit.M).byBox(300, 300, GeoUnit.M));
  }

  @Test(expected = IllegalArgumentException.class)
  public void geosearchSearchParamWithoutByRadiousAndByBox() {
    pipe.geosearch("barcelona", new GeoSearchParam().fromMember("foobar"));
  }

  @Test
  public void geosearchstore() {
    jedis.geoadd("barcelona", 2.1909389952632d, 41.433791470673d, "place1");
    jedis.geoadd("barcelona", 2.1873744593677d, 41.406342043777d, "place2");
    jedis.geoadd("barcelona", 2.583333d, 41.316667d, "place3");

    // FROMLONLAT and BYRADIUS
    Response<Long> membersCount1 = pipe.geosearchStore("tel-aviv", "barcelona",
        new GeoCoordinate(2.191d, 41.433d), 1000, GeoUnit.M);

    Response<List<String>> members1 = pipe.zrange("tel-aviv", 0, -1);

    Response<Long> membersCount2 = pipe.geosearchStore("tel-aviv", "barcelona", new GeoSearchParam()
        .byRadius(3000, GeoUnit.M)
        .fromLonLat(new GeoCoordinate(2.191d, 41.433d)));

    // FROMMEMBER and BYRADIUS
    Response<Long> membersCount3 = pipe.geosearchStore("tel-aviv", "barcelona", "place3", 100, GeoUnit.KM);

    // FROMMEMBER and BYBOX
    Response<Long> membersCount4 = pipe.geosearchStore("tel-aviv", "barcelona", "place3", 100, 100, GeoUnit.KM);

    // FROMLONLAT and BYBOX
    Response<Long> membersCount5 = pipe.geosearchStore("tel-aviv", "barcelona",
        new GeoCoordinate(2.191, 41.433), 1, 1, GeoUnit.KM);

    pipe.sync();

    assertThat(membersCount1.get(), equalTo(1L));
    assertThat(members1.get(), contains("place1"));
    assertThat(membersCount2.get(), equalTo(2L));
    assertThat(membersCount3.get(), equalTo(3L));
    assertThat(membersCount4.get(), equalTo(3L));
    assertThat(membersCount5.get(), equalTo(1L));
  }

  @Test
  public void geosearchstoreWithdist() {
    jedis.geoadd("barcelona", 2.1909389952632d, 41.433791470673d, "place1");
    jedis.geoadd("barcelona", 2.1873744593677d, 41.406342043777d, "place2");

    Response<Long> members = pipe.geosearchStoreStoreDist("tel-aviv", "barcelona",
        new GeoSearchParam().byRadius(3000, GeoUnit.M).fromLonLat(2.191d, 41.433d));

    Response<Double> score = pipe.zscore("tel-aviv", "place1");

    pipe.sync();

    assertThat(members.get(), equalTo(2L));
    assertThat(score.get(), closeTo(88.05060698409301, 5));
  }

  private void prepareGeoData() {
    Map<String, GeoCoordinate> coordinateMap = new HashMap<>();
    coordinateMap.put("a", new GeoCoordinate(3, 4));
    coordinateMap.put("b", new GeoCoordinate(2, 3));
    coordinateMap.put("c", new GeoCoordinate(3.314, 2.3241));

    assertEquals(3, jedis.geoadd("foo", coordinateMap));

    Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<>();
    bcoordinateMap.put(bA, new GeoCoordinate(3, 4));
    bcoordinateMap.put(bB, new GeoCoordinate(2, 3));
    bcoordinateMap.put(bC, new GeoCoordinate(3.314, 2.3241));

    assertEquals(3, jedis.geoadd(bfoo, bcoordinateMap));
  }
}
