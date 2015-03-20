package redis.clients.jedis;

import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static redis.clients.jedis.Protocol.read;
import static redis.clients.jedis.Protocol.toByteArray;

public class GeoParams {
  private final static String WITH_DISTANCE = "withdistance" ;
  private final static String WITH_COORDINATE = "withcoordinates" ;
  private final static String WITH_HASH = "withhash" ;
  private final static String WITH_GEOJSON  = "withgeojson" ;
  private final static String WITH_GEOJSON_COLLECTION = "withgeojsoncollection" ;
  private final static String NO_PROPERTIES = "noproperties" ;
  private final static String ASC = "asc" ;
  private final static String DESC= "desc" ;

  protected List<byte[]> params = new ArrayList<byte[]>();

  public Collection<byte[]> getParams() {
      return Collections.unmodifiableCollection(params);
  }

  // geoadd nyc 40.747533 -73.9454966 "lic market"
  // 1 if member is new or 0 if member is updated.
  // Multi add
  // Return value: count of members submitted.
  // geoadd nyc 40.7648057 -73.9733487 "central park" 40.7362513 -73.9903085 "union square"
  public static class GeoAddParams extends GeoParams {
      public GeoAddParams( double latitude, double longitude, String member ) {
          add(latitude, longitude, member) ;
      }

      public GeoAddParams add( double latitude, double longitude, String member ) {
          params.add(toByteArray(latitude));
          params.add(toByteArray(longitude));
          params.add(SafeEncoder.encode(member));
          return this ;
      }
  }

  // georadius geoset latitude longitude radius units [withdistance] [withcoordinates] [withhash] [withgeojson] [withgeojsoncollection] [noproperties] [asc|desc]
  public static class GeoRadiusParams extends GeoParams {
      protected GeoRadiusParams() {}

      public GeoRadiusParams(double latitude, double longitude, int distance, Protocol.Unit unit) {
          params.add(toByteArray(latitude));
          params.add(toByteArray(longitude));
          params.add(toByteArray(distance));
          params.add(SafeEncoder.encode(unit.name().toLowerCase()));
      }

      public GeoRadiusParams withdistance() {
          params.add(SafeEncoder.encode(WITH_DISTANCE));
          return this;
      }

      public GeoRadiusParams withhash() {
          params.add(SafeEncoder.encode(WITH_HASH));
          return this;
      }

      public GeoRadiusParams withgeojson() {
          params.add(SafeEncoder.encode(WITH_GEOJSON)) ;
          return this ;
      }

      public GeoRadiusParams withgeojsoncollection() {
          params.add(SafeEncoder.encode(WITH_GEOJSON_COLLECTION)) ;
          return this ;
      }

      public GeoRadiusParams withcoordinates() {
          params.add(SafeEncoder.encode(WITH_COORDINATE)) ;
          return this ;
      }

      public GeoRadiusParams sort(Protocol.Sort sort) {
          params.add(SafeEncoder.encode(sort.name().toLowerCase()));
          return this;
      }

      public GeoRadiusParams noproperties() {
          params.add(SafeEncoder.encode(NO_PROPERTIES)) ;
          return this ;
      }

      public GeoRadiusParams ascending() {
          params.add(SafeEncoder.encode(ASC));
          return this;
      }

      public GeoRadiusParams descending() {
          params.add(SafeEncoder.encode(DESC));
          return this;
      }
  }

   // georadiusbymember geoset member radius units [withdistance] [withcoordinates] [withhash] [withgeojson] [withgeojsoncollection] [noproperties] [asc|desc]
   //
   public static class GeoRadiusByMemeberParams extends GeoRadiusParams {
       public GeoRadiusByMemeberParams(String member, int distance, Protocol.Unit unit ) {
           params.add(SafeEncoder.encode(member));
           params.add(toByteArray(distance));
           params.add(SafeEncoder.encode(unit.name().toLowerCase()));
       }

       @Override
       public GeoRadiusByMemeberParams withdistance() {
           super.withdistance();
           return this;
       }

       @Override
       public GeoRadiusByMemeberParams withgeojson() {
           super.withgeojson() ;
           return this ;
       }

       @Override
       public GeoRadiusByMemeberParams withgeojsoncollection() {
           super.withgeojsoncollection() ;
           return this ;
       }

       @Override
       public GeoRadiusByMemeberParams withcoordinates() {
           super.withcoordinates() ;
           return this ;
       }

       @Override
       public GeoRadiusByMemeberParams noproperties() {
           super.noproperties() ;
           return this ;
       }

       @Override
       public GeoRadiusByMemeberParams sort(Protocol.Sort sort) {
           super.sort(sort );
           return this;
       }

       @Override
       public GeoRadiusByMemeberParams ascending() {
           super.ascending();
           return this;
       }

       @Override
       public GeoRadiusByMemeberParams descending() {
           super.descending() ;
           return this;
       }
  }
}
