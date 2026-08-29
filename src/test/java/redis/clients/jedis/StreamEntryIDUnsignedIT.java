package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

/**
 * Server round-trip tests for stream IDs in the upper half of the unsigned 64-bit range. The reply
 * builders funnel every echoed ID through {@link StreamEntryID#StreamEntryID(String)}, which used
 * to throw {@link NumberFormatException} for values above {@code 2^63-1}.
 */
public class StreamEntryIDUnsignedIT {

  private static final String KEY = "stream-entry-id-unsigned";

  private static EndpointConfig endpoint;
  private RedisClient client;

  @BeforeAll
  public static void setUpClass() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  @BeforeEach
  public void setUp() {
    client = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().build()).build();
    client.del(KEY);
  }

  @AfterEach
  public void tearDown() {
    client.del(KEY);
    client.close();
  }

  @Test
  public void upperHalfIdRoundTripsThroughReplyBuilders() {
    StreamEntryID id = new StreamEntryID("0-18446744073709551615"); // sequence 2^64-1
    Map<String, String> hash = Collections.singletonMap("field", "value");

    StreamEntryID echoed = client.xadd(KEY, XAddParams.xAddParams().id(id), hash);
    assertEquals(id, echoed);

    List<StreamEntry> range = client.xrange(KEY, "-", "+");
    assertEquals(1, range.size());
    assertEquals("0-18446744073709551615", range.get(0).getID().toString());
    assertEquals(hash, range.get(0).getFields());

    List<Map.Entry<String, List<StreamEntry>>> read = client.xread(
      XReadParams.xReadParams().count(1),
      Collections.singletonMap(KEY, new StreamEntryID("0-9223372036854775807")));
    assertEquals(1, read.size());
    assertEquals(id, read.get(0).getValue().get(0).getID());
  }

  @Test
  public void xAddParamsLongComponentsFormatUnsigned() {
    long sequence = Long.parseUnsignedLong("9223372036854775808"); // 2^63, negative as signed
    Map<String, String> hash = Collections.singletonMap("field", "value");

    StreamEntryID echoed = client.xadd(KEY, XAddParams.xAddParams().id(0L, sequence), hash);
    assertEquals("0-9223372036854775808", echoed.toString());

    List<StreamEntry> range = client.xrange(KEY, "-", "+");
    assertEquals(1, range.size());
    assertEquals(echoed, range.get(0).getID());
  }
}
