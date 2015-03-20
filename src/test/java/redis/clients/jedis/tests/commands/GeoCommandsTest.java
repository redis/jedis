package redis.clients.jedis.tests.commands;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START_BINARY;

public class GeoCommandsTest extends JedisCommandTestBase {

    @Before
   public void setUp() throws Exception {
        jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
        jedis.connect();
        jedis.configSet("timeout", "300");
        jedis.flushAll();
   }

  @Test
  public void geoadd() {
     long n = jedis.geoadd("geoadd", 40.747533, -73.9454966, "member1") ;
     assertEquals(1, n);
  }

  @Test
  public void multigeoadd() {
      GeoParams.GeoAddParams params = new GeoParams.GeoAddParams( 40.7648057, -73.9733487, "member2") ;
      params.add(40.7362513, -73.9903085, "member3") ;
      params.add(40.7126674, -74.0131604, "member4");
      params.add(40.6428986, -73.7858139, "member5") ;
      params.add(40.7498929, -73.9375699, "member6") ;
      params.add( 40.7480973, -73.9564142, "member7") ;

      long n = jedis.geoadd("multigeoadd", params ) ;
      assertEquals(6, n);
  }

  @Test
  public void georadius() {
      GeoParams.GeoAddParams params = new GeoParams.GeoAddParams( 40.7648057, -73.9733487, "member2") ;
      params.add(40.7362513, -73.9903085, "member3") ;
      params.add(40.7126674, -74.0131604, "member4");
      params.add(40.6428986, -73.7858139, "member5") ;
      params.add(40.7498929, -73.9375699, "member6") ;
      params.add( 40.7480973, -73.9564142, "member7") ;
      long n = jedis.geoadd("georadius", params ) ;

    GeoParams.GeoRadiusParams byRadiusParams = (new GeoParams.GeoRadiusParams(40.7598464, -73.9798091, 3, Protocol.Unit.KM)).withdistance().ascending();
    List<Object> s2 = jedis.georadius("georadius", byRadiusParams ) ;
    assertEquals(3, s2.size() );
  }

  @Test
  public void georadiusbymember() {
      GeoParams.GeoAddParams params = new GeoParams.GeoAddParams( 40.7648057, -73.9733487, "member2") ;
      params.add(40.7362513, -73.9903085, "member3") ;
      params.add(40.7126674, -74.0131604, "member4");
      params.add(40.6428986, -73.7858139, "member5") ;
      params.add(40.7498929, -73.9375699, "member6") ;
      params.add( 40.7480973, -73.9564142, "member7") ;
      long n = jedis.geoadd("georadiusbymember", params ) ;

    GeoParams.GeoRadiusByMemeberParams byMemeberParams = (new GeoParams.GeoRadiusByMemeberParams("member2", 3, Protocol.Unit.KM)).withdistance().ascending();
    List<Object> s2 = jedis.georadiusbymember("georadiusbymember", byMemeberParams ) ;
    assertEquals(2, s2.size() );
  }
}