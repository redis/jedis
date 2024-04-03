package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

/**
 * Tests related to <a href="https://redis.io/commands/?group=geo">Geospatial</a> commands.
 */
public class CommandObjectsGeospatialCommandsTest extends CommandObjectsStandaloneTestBase {

  // Some coordinates for testing
  public static final String CATANIA = "Catania";
  public static final double CATANIA_LATITUDE = 37.502669;
  public static final double CATANIA_LONGITUDE = 15.087269;

  public static final String PALERMO = "Palermo";
  public static final double PALERMO_LONGITUDE = 13.361389;
  public static final double PALERMO_LATITUDE = 38.115556;

  public static final String SYRACUSE = "Syracuse";
  public static final double SYRACUSE_LONGITUDE = 15.293331;
  public static final double SYRACUSE_LATITUDE = 37.075474;

  public static final String AGRIGENTO = "Agrigento";
  public static final double AGRIGENTO_LONGITUDE = 13.583333;
  public static final double AGRIGENTO_LATITUDE = 37.316667;

  public CommandObjectsGeospatialCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testGeoAddAndRadius() {
    String key = "locations";

    Map<String, GeoCoordinate> cataniaCoordinates = new HashMap<>();
    cataniaCoordinates.put(CATANIA, new GeoCoordinate(CATANIA_LONGITUDE, CATANIA_LATITUDE));

    Map<String, GeoCoordinate> syracuseCoordinates = new HashMap<>();
    syracuseCoordinates.put(SYRACUSE, new GeoCoordinate(SYRACUSE_LONGITUDE, SYRACUSE_LATITUDE));

    Long addPalermo = exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    assertThat(addPalermo, equalTo(1L));

    List<GeoRadiusResponse> radiusFromPalermo = exec(commandObjects.georadius(
        key, PALERMO_LONGITUDE, PALERMO_LATITUDE, 100, GeoUnit.KM));
    assertThat(radiusFromPalermo.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains(equalTo(PALERMO)));

    Long addCatania = exec(commandObjects.geoadd(key, cataniaCoordinates));
    assertThat(addCatania, equalTo(1L));

    List<GeoRadiusResponse> radiusFromCatania = exec(commandObjects.georadius(
        key, CATANIA_LONGITUDE, CATANIA_LATITUDE, 100, GeoUnit.KM));
    assertThat(radiusFromCatania.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        contains(equalTo(CATANIA)));

    Long addSyracuse = exec(commandObjects.geoadd(key, GeoAddParams.geoAddParams().nx(), syracuseCoordinates));
    assertThat(addSyracuse, equalTo(1L));

    List<GeoRadiusResponse> radiusEverything = exec(commandObjects.georadius(
        key, 15, 37, 200, GeoUnit.KM));
    assertThat(radiusEverything.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(equalTo(CATANIA), equalTo(SYRACUSE), equalTo(PALERMO)));
  }

  @Test
  public void testGeoAddAndRadiusBinary() {
    byte[] key = "locations".getBytes();

    Map<byte[], GeoCoordinate> cataniaCoordinates = new HashMap<>();
    cataniaCoordinates.put(CATANIA.getBytes(), new GeoCoordinate(CATANIA_LONGITUDE, CATANIA_LATITUDE));

    Map<byte[], GeoCoordinate> syracuseCoordinates = new HashMap<>();
    syracuseCoordinates.put(SYRACUSE.getBytes(), new GeoCoordinate(SYRACUSE_LONGITUDE, SYRACUSE_LATITUDE));

    Long addPalermo = exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO.getBytes()));
    assertThat(addPalermo, equalTo(1L));

    List<GeoRadiusResponse> radiusFromPalermo = exec(commandObjects.georadius(
        key, PALERMO_LONGITUDE, PALERMO_LATITUDE, 100, GeoUnit.KM));
    assertThat(radiusFromPalermo.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(equalTo(PALERMO.getBytes())));

    Long addCatania = exec(commandObjects.geoadd(key, cataniaCoordinates));
    assertThat(addCatania, equalTo(1L));

    List<GeoRadiusResponse> radiusFromCatania = exec(commandObjects.georadius(
        key, CATANIA_LONGITUDE, CATANIA_LATITUDE, 100, GeoUnit.KM));
    assertThat(radiusFromCatania.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        contains(equalTo(CATANIA.getBytes())));

    Long addSyracuse = exec(commandObjects.geoadd(key, GeoAddParams.geoAddParams().nx(), syracuseCoordinates));
    assertThat(addSyracuse, equalTo(1L));

    List<GeoRadiusResponse> radiusEverything = exec(commandObjects.georadius(
        key, 15, 37, 200, GeoUnit.KM));
    assertThat(radiusEverything.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(equalTo(CATANIA.getBytes()), equalTo(PALERMO.getBytes()), equalTo(SYRACUSE.getBytes())));
  }

  @Test
  public void testGeoDist() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    // Add locations to calculate distance
    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));

    Double distance = exec(commandObjects.geodist(key, CATANIA, PALERMO));
    // This is in meters, we don't try to accurately assert it. We refer to it later.
    assertThat(distance, notNullValue());

    Double distanceWithUnit = exec(commandObjects.geodist(key, CATANIA, PALERMO, GeoUnit.KM));
    assertThat(distanceWithUnit, closeTo(distance / 1000, 0.001));

    Double binaryDistance = exec(commandObjects.geodist(binaryKey, CATANIA.getBytes(), PALERMO.getBytes()));
    assertThat(binaryDistance, closeTo(distance, 0.001));

    Double binaryDistanceWithUnit = exec(commandObjects.geodist(binaryKey, CATANIA.getBytes(), PALERMO.getBytes(), GeoUnit.KM));
    assertThat(binaryDistanceWithUnit, closeTo(distance / 1000, 0.001));
  }

  @Test
  public void testGeoHash() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));

    List<String> hashes = exec(commandObjects.geohash(key, CATANIA, PALERMO));
    assertThat(hashes, contains(notNullValue(), notNullValue()));

    List<byte[]> binaryHashes = exec(commandObjects.geohash(binaryKey, CATANIA.getBytes(), PALERMO.getBytes()));
    assertThat(binaryHashes, contains(hashes.get(0).getBytes(), hashes.get(1).getBytes()));
  }

  @Test
  public void testGeoPos() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));

    List<GeoCoordinate> positions = exec(commandObjects.geopos(key, CATANIA, PALERMO));
    assertThat(positions.size(), equalTo(2));

    assertThat(positions.get(0), notNullValue());
    assertThat(positions.get(0).getLongitude(), closeTo(CATANIA_LONGITUDE, 0.001));
    assertThat(positions.get(0).getLatitude(), closeTo(CATANIA_LATITUDE, 0.001));

    assertThat(positions.get(1), notNullValue());
    assertThat(positions.get(1).getLongitude(), closeTo(PALERMO_LONGITUDE, 0.001));
    assertThat(positions.get(1).getLatitude(), closeTo(PALERMO_LATITUDE, 0.001));

    List<GeoCoordinate> binaryPositions = exec(commandObjects.geopos(binaryKey, CATANIA.getBytes(), PALERMO.getBytes()));
    assertThat(binaryPositions.size(), equalTo(2));

    assertThat(binaryPositions.get(0), notNullValue());
    assertThat(binaryPositions.get(0).getLongitude(), closeTo(CATANIA_LONGITUDE, 0.001));
    assertThat(binaryPositions.get(0).getLatitude(), closeTo(CATANIA_LATITUDE, 0.001));

    assertThat(binaryPositions.get(1), notNullValue());
    assertThat(binaryPositions.get(1).getLongitude(), closeTo(PALERMO_LONGITUDE, 0.001));
    assertThat(binaryPositions.get(1).getLatitude(), closeTo(PALERMO_LATITUDE, 0.001));
  }

  @Test
  public void testGeoRadius() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withCoord().withDist();

    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));

    List<GeoRadiusResponse> responses = exec(commandObjects.georadius(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM));

    // we got distances, but no coordinates
    assertThat(responses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA, PALERMO));
    assertThat(responses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> responsesWithParam = exec(commandObjects.georadius(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM, param));

    // we got distances, and coordinates
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA, PALERMO));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(CATANIA_LATITUDE, 0.001)));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(CATANIA_LONGITUDE, 0.001)));

    List<GeoRadiusResponse> binaryResponses = exec(commandObjects.georadius(binaryKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM));

    // distances, but no coordinates
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA, PALERMO));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> binaryResponsesWithParam = exec(commandObjects.georadius(binaryKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM, param));

    // distances, and coordinates
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA.getBytes(), PALERMO.getBytes()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(CATANIA_LATITUDE, 0.001)));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(CATANIA_LONGITUDE, 0.001)));
  }

  @Test
  public void testGeoRadiusReadonly() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withCoord().withDist();

    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));

    List<GeoRadiusResponse> responses = exec(commandObjects.georadiusReadonly(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM));

    // we got distances, but no coordinates
    assertThat(responses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA, PALERMO));
    assertThat(responses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> responsesWithParam = exec(commandObjects.georadiusReadonly(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM, param));

    // we got distances, and coordinates
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA, PALERMO));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(CATANIA_LATITUDE, 0.001)));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(CATANIA_LONGITUDE, 0.001)));

    List<GeoRadiusResponse> binaryResponses = exec(commandObjects.georadiusReadonly(binaryKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM));

    // distances, but no coordinates
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA, PALERMO));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> binaryResponsesWithParam = exec(commandObjects.georadiusReadonly(binaryKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, 200, GeoUnit.KM, param));

    // distances, and coordinates
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(CATANIA.getBytes(), PALERMO.getBytes()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(CATANIA_LATITUDE, 0.001)));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(CATANIA_LONGITUDE, 0.001)));
  }

  @Test
  public void testGeoRadiusByMember() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withCoord().withDist();

    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(key, AGRIGENTO_LONGITUDE, AGRIGENTO_LATITUDE, AGRIGENTO));

    List<GeoRadiusResponse> responses = exec(commandObjects.georadiusByMember(key, AGRIGENTO, 100, GeoUnit.KM));

    assertThat(responses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO, PALERMO));
    assertThat(responses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> responsesWithParam = exec(commandObjects.georadiusByMember(key, AGRIGENTO, 100, GeoUnit.KM, param));

    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO, PALERMO));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(AGRIGENTO_LATITUDE, 0.001)));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(AGRIGENTO_LONGITUDE, 0.001)));

    List<GeoRadiusResponse> binaryResponses = exec(commandObjects.georadiusByMember(binaryKey, AGRIGENTO.getBytes(), 100, GeoUnit.KM));

    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO, PALERMO));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> binaryResponsesWithParam = exec(commandObjects.georadiusByMember(binaryKey, AGRIGENTO.getBytes(), 100, GeoUnit.KM, param));

    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO.getBytes(), PALERMO.getBytes()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(AGRIGENTO_LATITUDE, 0.001)));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(AGRIGENTO_LONGITUDE, 0.001)));
  }

  @Test
  public void testGeoRadiusByMemberReadonly() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withCoord().withDist();

    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(key, AGRIGENTO_LONGITUDE, AGRIGENTO_LATITUDE, AGRIGENTO));

    List<GeoRadiusResponse> responses = exec(commandObjects.georadiusByMemberReadonly(key, AGRIGENTO, 100, GeoUnit.KM));

    assertThat(responses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO, PALERMO));
    assertThat(responses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> responsesWithParam = exec(commandObjects.georadiusByMemberReadonly(key, AGRIGENTO, 100, GeoUnit.KM, param));

    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO, PALERMO));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(AGRIGENTO_LATITUDE, 0.001)));
    assertThat(responsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(AGRIGENTO_LONGITUDE, 0.001)));

    List<GeoRadiusResponse> binaryResponses = exec(commandObjects.georadiusByMemberReadonly(binaryKey, AGRIGENTO.getBytes(), 100, GeoUnit.KM));

    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO, PALERMO));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponses.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(nullValue(), nullValue()));

    List<GeoRadiusResponse> binaryResponsesWithParam = exec(commandObjects.georadiusByMemberReadonly(binaryKey, AGRIGENTO.getBytes(), 100, GeoUnit.KM, param));

    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getMember).collect(Collectors.toList()),
        containsInAnyOrder(AGRIGENTO.getBytes(), PALERMO.getBytes()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getDistance).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).collect(Collectors.toList()),
        containsInAnyOrder(notNullValue(), notNullValue()));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLatitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LATITUDE, 0.001), closeTo(AGRIGENTO_LATITUDE, 0.001)));
    assertThat(binaryResponsesWithParam.stream().map(GeoRadiusResponse::getCoordinate).map(GeoCoordinate::getLongitude).collect(Collectors.toList()),
        containsInAnyOrder(closeTo(PALERMO_LONGITUDE, 0.001), closeTo(AGRIGENTO_LONGITUDE, 0.001)));
  }

  @Test
  public void testGeoradiusStore() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    String destinationKey = "result";
    String binaryDestinationKey = "resultBinary";

    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().sortAscending();

    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));

    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store(destinationKey);

    Long store = exec(commandObjects.georadiusStore(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, 200, GeoUnit.KM, param, storeParam));
    assertThat(store, equalTo(2L));

    List<String> destination = exec(commandObjects.zrange(destinationKey, 0, -1));
    assertThat(destination, containsInAnyOrder(PALERMO, CATANIA));

    GeoRadiusStoreParam storeParamForBinary = GeoRadiusStoreParam.geoRadiusStoreParam().store(binaryDestinationKey);

    Long storeBinary = exec(commandObjects.georadiusStore(binaryKey, PALERMO_LONGITUDE, PALERMO_LATITUDE, 200, GeoUnit.KM, param, storeParamForBinary));
    assertThat(storeBinary, equalTo(2L));

    destination = exec(commandObjects.zrange(binaryDestinationKey, 0, -1));
    assertThat(destination, containsInAnyOrder(PALERMO, CATANIA));
  }

  @Test
  public void testGeoradiusByMemberStore() {
    String key = "locations";
    byte[] binaryKey = key.getBytes();

    String destinationKey = "result";
    String binaryDestinationKey = "resultBinary";

    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().sortAscending();

    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));

    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store(destinationKey);

    Long store = exec(commandObjects.georadiusByMemberStore(key, PALERMO, 200, GeoUnit.KM, param, storeParam));
    assertThat(store, equalTo(2L));

    List<String> storedResults = exec(commandObjects.zrange(destinationKey, 0, -1));
    assertThat(storedResults, containsInAnyOrder(PALERMO, CATANIA));

    GeoRadiusStoreParam storeParamForBinary = GeoRadiusStoreParam.geoRadiusStoreParam().store(binaryDestinationKey);

    Long storeBinary = exec(commandObjects.georadiusByMemberStore(binaryKey, PALERMO.getBytes(), 200, GeoUnit.KM, param, storeParamForBinary));
    assertThat(storeBinary, equalTo(2L));

    storedResults = exec(commandObjects.zrange(binaryDestinationKey, 0, -1));
    assertThat(storedResults, containsInAnyOrder(PALERMO, CATANIA));
  }

  @Test
  public void testGeosearch() {
    String key = "locations";

    GeoCoordinate palermoCoord = new GeoCoordinate(PALERMO_LONGITUDE, PALERMO_LATITUDE);

    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));

    List<GeoRadiusResponse> resultsByMember = exec(commandObjects.geosearch(key, PALERMO, 200, GeoUnit.KM));

    assertThat(resultsByMember.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO, CATANIA));

    List<GeoRadiusResponse> resultsByCoord = exec(commandObjects.geosearch(key, palermoCoord, 200, GeoUnit.KM));

    assertThat(resultsByCoord.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO, CATANIA));

    List<GeoRadiusResponse> resultsByMemberBox = exec(commandObjects.geosearch(key, PALERMO, 200, 200, GeoUnit.KM));

    assertThat(resultsByMemberBox.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO));

    List<GeoRadiusResponse> resultsByCoordBox = exec(commandObjects.geosearch(key, palermoCoord, 200, 200, GeoUnit.KM));

    assertThat(resultsByCoordBox.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO));

    GeoSearchParam params = GeoSearchParam.geoSearchParam()
        .byRadius(200, GeoUnit.KM).withCoord().withDist().fromMember(PALERMO);

    List<GeoRadiusResponse> resultsWithParams = exec(commandObjects.geosearch(key, params));

    assertThat(resultsWithParams.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO, CATANIA));

    List<GeoRadiusResponse> resultsInvalidKey = exec(commandObjects.geosearch("invalidKey", PALERMO, 100, GeoUnit.KM));

    assertThat(resultsInvalidKey, empty());
  }

  @Test
  public void testGeosearchBinary() {
    byte[] key = "locations".getBytes();

    GeoCoordinate palermoCoord = new GeoCoordinate(PALERMO_LONGITUDE, PALERMO_LATITUDE);

    exec(commandObjects.geoadd(key, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO.getBytes()));
    exec(commandObjects.geoadd(key, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA.getBytes()));

    List<GeoRadiusResponse> resultsByMember = exec(commandObjects.geosearch(key, PALERMO.getBytes(), 200, GeoUnit.KM));

    assertThat(resultsByMember.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO, CATANIA));

    List<GeoRadiusResponse> resultsByCoord = exec(commandObjects.geosearch(key, palermoCoord, 200, GeoUnit.KM));

    assertThat(resultsByCoord.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO, CATANIA));

    List<GeoRadiusResponse> resultsByMemberBox = exec(commandObjects.geosearch(key, PALERMO.getBytes(), 200, 200, GeoUnit.KM));

    assertThat(resultsByMemberBox.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO));

    List<GeoRadiusResponse> resultsByCoordBox = exec(commandObjects.geosearch(key, palermoCoord, 200, 200, GeoUnit.KM));

    assertThat(resultsByCoordBox.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO));

    GeoSearchParam params = GeoSearchParam.geoSearchParam()
        .byRadius(200, GeoUnit.KM).withCoord().withDist().fromMember(PALERMO);

    List<GeoRadiusResponse> resultsWithParams = exec(commandObjects.geosearch(key, params));

    assertThat(resultsWithParams.stream().map(GeoRadiusResponse::getMemberByString).collect(Collectors.toList()),
        containsInAnyOrder(PALERMO, CATANIA));

    List<GeoRadiusResponse> resultsInvalidKey = exec(commandObjects.geosearch("invalidKey".getBytes(), PALERMO.getBytes(), 100, GeoUnit.KM));

    assertThat(resultsInvalidKey, empty());
  }

  @Test
  public void testGeosearchStore() {
    String srcKey = "locations";
    String destKey = "locationsStore";

    GeoCoordinate palermoCoord = new GeoCoordinate(PALERMO_LONGITUDE, PALERMO_LATITUDE);

    exec(commandObjects.geoadd(srcKey, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(srcKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));

    Long storeByMember = exec(commandObjects.geosearchStore(destKey, srcKey, PALERMO, 200, GeoUnit.KM));
    assertThat(storeByMember, equalTo(2L));

    List<String> storedResultsByMember = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByMember, containsInAnyOrder(PALERMO, CATANIA));

    // Reset
    exec(commandObjects.del(destKey));

    Long storeByCoord = exec(commandObjects.geosearchStore(destKey, srcKey, palermoCoord, 200, GeoUnit.KM));
    assertThat(storeByCoord, equalTo(2L));

    List<String> storedResultsByCoord = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByCoord, containsInAnyOrder(PALERMO, CATANIA));

    exec(commandObjects.del(destKey));

    Long storeByMemberBox = exec(commandObjects.geosearchStore(destKey, srcKey, PALERMO, 200, 200, GeoUnit.KM));
    assertThat(storeByMemberBox, equalTo(1L));

    List<String> storedResultsByMemberBox = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByMemberBox, containsInAnyOrder(PALERMO));

    exec(commandObjects.del(destKey));

    Long storeByCoordBox = exec(commandObjects.geosearchStore(destKey, srcKey, palermoCoord, 200, 200, GeoUnit.KM));
    assertThat(storeByCoordBox, equalTo(1L));

    List<String> storedResultsByCoordBox = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByCoordBox, containsInAnyOrder(PALERMO));

    exec(commandObjects.del(destKey));

    GeoSearchParam params = GeoSearchParam.geoSearchParam()
        .byRadius(200, GeoUnit.KM).fromMember(PALERMO);

    Long storeWithParams = exec(commandObjects.geosearchStore(destKey, srcKey, params));
    assertThat(storeWithParams, equalTo(2L));

    List<String> storedResultsWithParams = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsWithParams, containsInAnyOrder(PALERMO, CATANIA));
  }

  @Test
  public void testGeosearchStoreBinary() {
    byte[] srcKey = "locations".getBytes();
    byte[] destKey = "locationsStore".getBytes();

    GeoCoordinate palermoCoord = new GeoCoordinate(PALERMO_LONGITUDE, PALERMO_LATITUDE);

    exec(commandObjects.geoadd(srcKey, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO.getBytes()));
    exec(commandObjects.geoadd(srcKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA.getBytes()));

    Long storeByMember = exec(commandObjects.geosearchStore(destKey, srcKey, PALERMO.getBytes(), 200, GeoUnit.KM));
    assertThat(storeByMember, equalTo(2L));

    List<byte[]> storedResultsByMember = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByMember, containsInAnyOrder(PALERMO.getBytes(), CATANIA.getBytes()));

    // Reset
    exec(commandObjects.del(destKey));

    Long storeByCoord = exec(commandObjects.geosearchStore(destKey, srcKey, palermoCoord, 200, GeoUnit.KM));
    assertThat(storeByCoord, equalTo(2L));

    List<byte[]> storedResultsByCoord = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByCoord, containsInAnyOrder(PALERMO.getBytes(), CATANIA.getBytes()));

    exec(commandObjects.del(destKey));

    Long storeByMemberBox = exec(commandObjects.geosearchStore(destKey, srcKey, PALERMO.getBytes(), 200, 200, GeoUnit.KM));
    assertThat(storeByMemberBox, equalTo(1L));

    List<byte[]> storedResultsByMemberBox = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByMemberBox, containsInAnyOrder(PALERMO.getBytes()));

    exec(commandObjects.del(destKey));

    Long storeByCoordBox = exec(commandObjects.geosearchStore(destKey, srcKey, palermoCoord, 200, 200, GeoUnit.KM));
    assertThat(storeByCoordBox, equalTo(1L));

    List<byte[]> storedResultsByCoordBox = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsByCoordBox, containsInAnyOrder(PALERMO.getBytes()));

    exec(commandObjects.del(destKey));

    GeoSearchParam params = GeoSearchParam.geoSearchParam()
        .byRadius(200, GeoUnit.KM).fromMember(PALERMO);

    Long storeWithParams = exec(commandObjects.geosearchStore(destKey, srcKey, params));
    assertThat(storeWithParams, equalTo(2L));

    List<byte[]> storedResultsWithParams = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(storedResultsWithParams, containsInAnyOrder(PALERMO.getBytes(), CATANIA.getBytes()));
  }

  @Test
  public void testGeosearchStoreStoreDist() {
    String srcKey = "locations";
    byte[] srcKeyBytes = srcKey.getBytes();

    String destKey = "resultKey";
    byte[] destKeyBytes = destKey.getBytes();

    exec(commandObjects.geoadd(srcKey, PALERMO_LONGITUDE, PALERMO_LATITUDE, PALERMO));
    exec(commandObjects.geoadd(srcKey, CATANIA_LONGITUDE, CATANIA_LATITUDE, CATANIA));
    exec(commandObjects.geoadd(srcKey, SYRACUSE_LONGITUDE, SYRACUSE_LATITUDE, SYRACUSE));

    GeoSearchParam params = new GeoSearchParam()
        .byRadius(100, GeoUnit.KM).fromLonLat(15, 37);

    Long store = exec(commandObjects.geosearchStoreStoreDist(destKey, srcKey, params));
    assertThat(store, equalTo(2L));

    List<String> dstContent = exec(commandObjects.zrange(destKey, 0, -1));
    assertThat(dstContent, containsInAnyOrder(CATANIA, SYRACUSE));

    exec(commandObjects.del(destKey));

    Long storeWithBytes = exec(commandObjects.geosearchStoreStoreDist(destKeyBytes, srcKeyBytes, params));
    assertThat(storeWithBytes, equalTo(2L));

    List<byte[]> dstContentWithBytes = exec(commandObjects.zrange(destKeyBytes, 0, -1));
    assertThat(dstContentWithBytes, containsInAnyOrder(CATANIA.getBytes(), SYRACUSE.getBytes()));
  }
}
