package redis.clients.jedis.examples;

import org.junit.Assert;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.schemafields.GeoShapeField;

/**
 * As of RediSearch 2.8.4, advanced GEO querying with GEOSHAPE fields is supported.
 * <p>
 * Notes:
 * <ul>
 *   <li>As of RediSearch 2.8.4, only POLYGON and POINT objects are supported.</li>
 *   <li>As of RediSearch 2.8.4, only WITHIN and CONTAINS conditions are supported.</li>
 *   <li>As of RedisStack 7.4.0, support for INTERSECTS and DISJOINT conditions are added.</li>
 * </ul>
 *
 * Any object/library producing a <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">
 * well-known text (WKT)</a> in {@code toString()} method can be used.
 *
 * This example uses the <a href="https://github.com/locationtech/jts">JTS</a> library.
 * <pre>
 * {@code
 * <dependency>
 *   <groupId>org.locationtech.jts</groupId>
 *   <artifactId>jts-core</artifactId>
 *   <version>1.19.0</version>
 * </dependency>
 * }
 * </pre>
 */
public class GeoShapeFieldsUsageInRediSearch {

  public static void main(String[] args) {

    // We'll create geometry objects with GeometryFactory
    final GeometryFactory factory = new GeometryFactory();

    final String host = "localhost";
    final int port = 6379;
    final HostAndPort address = new HostAndPort(host, port);

    UnifiedJedis client = new JedisPooled(address);
    // client.setDefaultSearchDialect(3); // we can set default search dialect for the client (UnifiedJedis) object
                                          // to avoid setting dialect in every query.

    // creating index
    client.ftCreate("geometry-index",
        GeoShapeField.of("geometry", GeoShapeField.CoordinateSystem.SPHERICAL) // 'SPHERICAL' is for geographic (lon, lat).
                                                   // 'FLAT' coordinate system also available for cartesian (X,Y).
    );

    // preparing data
    final Polygon small = factory.createPolygon(
        new Coordinate[]{new Coordinate(34.9001, 29.7001),
        new Coordinate(34.9001, 29.7100), new Coordinate(34.9100, 29.7100),
        new Coordinate(34.9100, 29.7001), new Coordinate(34.9001, 29.7001)}
    );

    // client.hset("small", RediSearchUtil.toStringMap(Collections.singletonMap("geometry", small))); // setting data
    // client.hset("small", "geometry", small.toString()); // simplified setting data
    client.hsetObject("small", "geometry", small); // more simplified setting data

    final Polygon large = factory.createPolygon(
        new Coordinate[]{new Coordinate(34.9001, 29.7001),
        new Coordinate(34.9001, 29.7200), new Coordinate(34.9200, 29.7200),
        new Coordinate(34.9200, 29.7001), new Coordinate(34.9001, 29.7001)}
    );

    // client.hset("large", RediSearchUtil.toStringMap(Collections.singletonMap("geometry", large))); // setting data
    // client.hset("large", "geometry", large.toString()); // simplified setting data
    client.hsetObject("large", "geometry", large); // more simplified setting data

    // searching
    final Polygon within = factory.createPolygon(
        new Coordinate[]{new Coordinate(34.9000, 29.7000),
        new Coordinate(34.9000, 29.7150), new Coordinate(34.9150, 29.7150),
        new Coordinate(34.9150, 29.7000), new Coordinate(34.9000, 29.7000)}
    );

    SearchResult res = client.ftSearch("geometry-index",
        "@geometry:[within $poly]",     // query string
        FTSearchParams.searchParams()
            .addParam("poly", within)
            .dialect(3)                 // DIALECT '3' is required for this query
    ); 
    Assert.assertEquals(1, res.getTotalResults());
    Assert.assertEquals(1, res.getDocuments().size());

    // We can parse geometry objects with WKTReader
    try {
      final WKTReader reader = new WKTReader();
      Geometry object = reader.read(res.getDocuments().get(0).getString("geometry"));
      Assert.assertEquals(small, object);
    } catch (ParseException ex) { // WKTReader#read throws ParseException
      ex.printStackTrace(System.err);
    }
  }
}
