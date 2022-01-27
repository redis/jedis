package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

public interface GeoCommands {

  /**
   * Adds the specified geospatial item (longitude, latitude, member) to the specified key.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the sorted set.
   * @param key
   * @param longitude
   * @param latitude
   * @param member
   * @return The number of elements added
   */
  long geoadd(String key, double longitude, double latitude, String member);

  /**
   * Adds the specified geospatial items (in memberCoordinateMap) to the specified key.
   * <p>
   * Time complexity: O(log(N)) for each item added, where N is the number of elements in
   * the sorted set.
   * @param key
   * @param memberCoordinateMap Members names with their geo coordinates
   * @return The number of elements added
   */
  long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

  /**
   * Adds the specified geospatial items (in memberCoordinateMap) to the specified key.
   * Can be used with the following options:
   * XX- Only update elements that already exist. Never add elements.
   * NX- Don't update already existing elements. Always add new elements.
   * CH- Modify the return value from the number of new elements added, to the total number of elements changed
   * <p>
   * Time complexity: O(log(N)) for each item added
   * @param key
   * @param params Additional options
   * @param memberCoordinateMap Members names with their geo coordinates
   * @return The number of elements added
   */
  long geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap);

  /**
   * Return the distance between two members in the geospatial index represented by the sorted set.
   * <p>
   * Time complexity: O(log(N))
   * @param key
   * @param member1
   * @param member2
   * @return The distance as a double
   */
  Double geodist(String key, String member1, String member2);

  /**
   * Return the distance between two members in the geospatial index represented by the sorted set.
   * <p>
   * Time complexity: O(log(N))
   * @param key
   * @param member1
   * @param member2
   * @param unit can be M, KM, MI or FT can  M, KM, MI or FT
   * @return The distance as a double
   */
  Double geodist(String key, String member1, String member2, GeoUnit unit);

  /**
   * Return valid Geohash strings representing the position of the given members.
   * <p>
   * Time complexity: O(log(N)) for each member requested
   * @param key
   * @param members
   * @return A list of Geohash strings corresponding to each member name passed as
   * argument to the command.
   */
  List<String> geohash(String key, String... members);

  /**
   * Return the positions (longitude,latitude) of all the specified members.
   * <p>
   * Time complexity: O(N) where N is the number of members requested.
   * @param key
   * @param members
   * @return A list of GeoCoordinate representing longitude and latitude (x,y)
   * of each member name passed as argument to the command.
   */
  List<GeoCoordinate> geopos(String key, String... members);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified with the center location and the radius.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param longitude of the center point
   * @param latitude of the center point
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
      GeoUnit unit);

  /**
   * Readonly version of {@link GeoCommands#georadius(String, double, double, double, GeoUnit) GEORADIUS},
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @see GeoCommands#georadius(String, double, double, double, GeoUnit)
   * @param key
   * @param longitude of the center point
   * @param latitude of the center point
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified with the center location and the radius.
   * Additional information can be reached using {@link GeoRadiusParam}:
   * WITHDIST: Also return the distance of the returned items from the specified center.
   * The distance is returned in the same unit as the unit specified as the radius argument of the command.
   * WITHCOORD: Also return the longitude,latitude coordinates of the matching items.
   * WITHHASH: Also return the raw geohash-encoded sorted set score of the item, in the form of a 52
   * bit unsigned integer. This is only useful for low level hacks or debugging and is otherwise of
   * little interest for the general user.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param longitude of the center point
   * @param latitude of the center point
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @param param {@link GeoRadiusParam}
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param);

  /**
   * Readonly version of {@link GeoCommands#georadius(String, double, double, double, GeoUnit, GeoRadiusParam) GEORADIUS},
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @see GeoCommands#georadius(String, double, double, double, GeoUnit, GeoRadiusParam)
   * @param key
   * @param longitude of the center point
   * @param latitude of the center point
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @param param {@link GeoRadiusParam}
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param);

  /**
   * This command is exactly like {@link GeoCommands#georadius(String, double, double, double, GeoUnit) GEORADIUS}
   * with the sole difference that instead of taking, as the center of the area to query, a longitude
   * and latitude value, it takes the name of a member already existing inside the geospatial index
   * represented by the sorted set.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param member represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit);

  /**
   * Readonly version of {@link GeoCommands#georadiusByMember(String, String, double, GeoUnit) GEORADIUSBYMEMBER}
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param member represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit);

  /**
   * This command is exactly like {@link GeoCommands#georadius(String, double, double, double, GeoUnit, GeoRadiusParam) GEORADIUS}
   * with the sole difference that instead of taking, as the center of the area to query, a longitude
   * and latitude value, it takes the name of a member already existing inside the geospatial index
   * represented by the sorted set.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param member represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @param param {@link GeoRadiusParam}
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param);

  /**
   * Readonly version of {@link GeoCommands#georadiusByMember(String, String, double, GeoUnit, GeoRadiusParam) GEORADIUSBYMEMBER}
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param member represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @param param {@link GeoRadiusParam}
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param);

  /**
   * This command is exactly like {@link GeoCommands#georadius(String, double, double, double, GeoUnit, GeoRadiusParam) GEORADIUS}
   * but storing the results at the destination key (provided with {@link GeoRadiusStoreParam storeParam}).
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param longitude of the center point
   * @param latitude of the center point
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @param param {@link GeoRadiusParam}
   * @param storeParam {@link GeoRadiusStoreParam}
   * @return The number of results being stored
   */
  long georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  /**
   * This command is exactly like {@link GeoCommands#georadiusByMember(String, String, double, GeoUnit, GeoRadiusParam) GEORADIUSBYMEMBER}
   * but storing the results at the destination key (provided with {@link GeoRadiusStoreParam storeParam}).
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements inside the bounding box of
   * the circular area delimited by center and radius and M is the number of items inside the index.
   * @param key
   * @param member represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @param param {@link GeoRadiusParam}
   * @param storeParam {@link GeoRadiusStoreParam}
   * @return The number of results being stored
   */
  long georadiusByMemberStore(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified by a given shape.
   * <p>
   * This command can be used in place of the {@link GeoCommands#georadiusByMember(String, String, double, GeoUnit) GEORADIUSBYMEMBER} command.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param key
   * @param member represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> geosearch(String key, String member, double radius, GeoUnit unit);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified by a given shape.
   * <p>
   * This command can be used in place of the {@link GeoCommands#georadius(String, double, double, double, GeoUnit) GEORADIUS} command.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param key
   * @param coord represents the center of the area
   * @param radius of the area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> geosearch(String key, GeoCoordinate coord, double radius, GeoUnit unit);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified by a given shape. This command extends
   * the GEORADIUS command, so in addition to searching within circular areas, it supports
   * searching within rectangular areas.
   * <p>
   * The axis-aligned rectangle, determined by height and width, when the center point is
   * determined by the position of the given member.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param key
   * @param member represents the center of the area
   * @param width of the rectangular area
   * @param height of the rectangular area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> geosearch(String key, String member, double width, double height, GeoUnit unit);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified by a given shape. This command extends
   * the GEORADIUS command, so in addition to searching within circular areas, it supports
   * searching within rectangular areas.
   * <p>
   * The axis-aligned rectangle, determined by height and width, when the center point is
   * determined by the given coordinate.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param key
   * @param coord represents the center point
   * @param width of the rectangular area
   * @param height of the rectangular area
   * @param unit can be M, KM, MI or FT
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> geosearch(String key, GeoCoordinate coord, double width, double height, GeoUnit unit);

  /**
   * Return the members of a sorted set populated with geospatial information using GEOADD,
   * which are within the borders of the area specified by a given shape. This command extends
   * the GEORADIUS command, so in addition to searching within circular areas, it supports
   * searching within rectangular areas.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param key
   * @param params {@link GeoSearchParam}
   * @return List of GeoRadiusResponse
   */
  List<GeoRadiusResponse> geosearch(String key, GeoSearchParam params);

  /**
   * This command is exactly like {@link GeoCommands#geosearch(String, String, double, GeoUnit) GEOSEARCH}
   * but storing the results at dest.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param dest
   * @param src the sorted set (key)
   * @param member represents the center of the area
   * @param radius of the circular area
   * @param unit can be M, KM, MI or FT
   * @return The number of results being stored
   */
  long geosearchStore(String dest, String src, String member, double radius, GeoUnit unit);

  /**
   * This command is exactly like {@link GeoCommands#geosearch(String, GeoCoordinate, double, GeoUnit) GEOSEARCH}
   * but storing the results at dest.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param dest
   * @param src
   * @param coord represents the center point
   * @param radius of the circular area
   * @param unit can be M, KM, MI or FT
   * @return The number of results being stored
   */
  long geosearchStore(String dest, String src, GeoCoordinate coord, double radius, GeoUnit unit);

  /**
   * This command is exactly like {@link GeoCommands#geosearch(String, String, double, double, GeoUnit) GEOSEARCH}
   * but storing the results at dest.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param dest
   * @param src
   * @param member represents the center of the area
   * @param width of the rectangular area
   * @param height of the rectangular area
   * @param unit can be M, KM, MI or FT
   * @return The number of results being stored
   */
  long geosearchStore(String dest, String src, String member, double width, double height, GeoUnit unit);

  /**
   * This command is exactly like {@link GeoCommands#geosearch(String, GeoCoordinate, double, double, GeoUnit) GEOSEARCH}
   * but storing the results at dest.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param dest
   * @param src
   * @param coord represents the center point
   * @param width of the rectangular area
   * @param height of the rectangular area
   * @param unit can be M, KM, MI or FT
   * @return The number of results being stored
   */
  long geosearchStore(String dest, String src, GeoCoordinate coord, double width, double height, GeoUnit unit);

  /**
   * This command is exactly like {@link GeoCommands#geosearch(String, GeoSearchParam) GEOSEARCH}
   * but storing the results at dest.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param dest
   * @param src
   * @param params {@link GeoSearchParam}
   * @return The number of results being stored
   */
  long geosearchStore(String dest, String src, GeoSearchParam params);

  /**
   * This command is exactly like {@link GeoCommands#geosearchStore(String, String, GeoSearchParam) GEOSEARCHSTORE}
   * but storing the results with their destinations from the center point.
   * <p>
   * Time complexity: O(N+log(M)) where N is the number of elements in the grid-aligned
   * bounding box area around the shape provided as the filter and M is the number of items
   * inside the shape
   * @param dest
   * @param src
   * @param params {@link GeoSearchParam}
   * @return The number of results being stored
   */
  long geosearchStoreStoreDist(String dest, String src, GeoSearchParam params);
}
