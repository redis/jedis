package redis.clients.jedis.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

public class GeoRadiusResponseIT {

  @ParameterizedTest
  @EnumSource(RedisProtocol.class)
  public void compareRepeatedSearchesWithoutCoordinates(RedisProtocol protocol) {
    EndpointConfig endpoint = Endpoints.getRedisEndpoint("redis2");
    String key = "geo-response-equality:" + UUID.randomUUID();
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(protocol).build())) {
      try {
        jedis.geoadd(key, 13.361389, 38.115556, "Palermo");
        GeoSearchParam params = new GeoSearchParam().fromMember("Palermo").byRadius(1, GeoUnit.KM)
            .withDist().withHash();
        List<GeoRadiusResponse> first = jedis.geosearch(key, params);
        List<GeoRadiusResponse> second = jedis.geosearch(key, params);

        assertEquals(1, first.size());
        assertNull(first.get(0).getCoordinate());
        assertEquals(first, second);
        assertEquals(first.get(0).hashCode(), second.get(0).hashCode());
        assertTrue(new HashSet<>(first).contains(second.get(0)));
      } finally {
        jedis.del(key);
      }
    }
  }
}
