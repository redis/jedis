package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;


import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ObjectCommandsTest extends JedisCommandsTestBase {

  private final String key = "mylist";
  private final byte[] binaryKey = SafeEncoder.encode(key);
  private static EndpointConfig lfuEndpoint;
  private Jedis lfuJedis;

  @BeforeAll
  public static void prepareEndpoints() {
    lfuEndpoint = Endpoints.getRedisEndpoint("standalone7-with-lfu-policy");
  }

  public ObjectCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    lfuJedis = new Jedis(lfuEndpoint.getHostAndPort(),
        lfuEndpoint.getClientConfigBuilder().build());
    lfuJedis.connect();
    lfuJedis.flushAll();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    lfuJedis.disconnect();
    super.tearDown();
  }

  @Test
  public void objectRefcount() {
    jedis.lpush(key, "hello world");
    Long refcount = jedis.objectRefcount(key);
    assertEquals(Long.valueOf(1), refcount);

    // Binary
    refcount = jedis.objectRefcount(binaryKey);
    assertEquals(Long.valueOf(1), refcount);

  }

  @Test
  public void objectEncodingString() {
    jedis.set(key, "hello world");
    assertThat(jedis.objectEncoding(key), containsString("str"));

    // Binary
    assertThat(SafeEncoder.encode(jedis.objectEncoding(binaryKey)), containsString("str"));
  }

  @Test
  public void objectEncodingList() {
    jedis.lpush(key, "hello world");
    assertThat(jedis.objectEncoding(key), containsString("list"));

    // Binary
    assertThat(SafeEncoder.encode(jedis.objectEncoding(binaryKey)), containsString("list"));
  }

  @Test
  public void objectIdletime() throws InterruptedException {
    jedis.lpush(key, "hello world");

    Long time = jedis.objectIdletime(key);
    assertEquals(Long.valueOf(0), time);

    // Binary
    time = jedis.objectIdletime(binaryKey);
    assertEquals(Long.valueOf(0), time);
  }

  @Test
  public void objectHelp() {
    // String
    List<String> helpTexts = jedis.objectHelp();
    assertNotNull(helpTexts);

    // Binary
    List<byte[]> helpBinaryTexts = jedis.objectHelpBinary();
    assertNotNull(helpBinaryTexts);
  }

  @Test
  public void objectFreq() {
    lfuJedis.set(key, "test1");
    lfuJedis.get(key);
    // String
    assertThat(lfuJedis.objectFreq(key), greaterThanOrEqualTo(1L));
    // Binary
    assertThat(lfuJedis.objectFreq(binaryKey), greaterThanOrEqualTo(1L));

    assertNull(lfuJedis.objectFreq("no_such_key"));

    jedis.set(key, "test2");
    assertThrows(JedisDataException.class, () -> jedis.objectFreq(key), "Freq is only allowed with LFU policy");
  }
}
