package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class JedisTest extends JedisCommandsTestBase {

  public JedisTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void useWithoutConnecting() {
    try (Jedis j = new Jedis()) {
      j.auth(endpoint.getPassword());
      j.dbSize();
    }
  }

  @Test
  public void checkBinaryData() {
    byte[] bigdata = new byte[1777];
    for (int b = 0; b < bigdata.length; b++) {
      bigdata[b] = (byte) ((byte) b % 255);
    }
    Map<String, String> hash = new HashMap<>();
    hash.put("data", SafeEncoder.encode(bigdata));

    assertEquals("OK", jedis.hmset("foo", hash));
    assertEquals(hash, jedis.hgetAll("foo"));
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), DefaultJedisClientConfig.builder().build())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithEmptyConfigInterface() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), new JedisClientConfig() {
    })) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfigInterface() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), new JedisClientConfig() {
      @Override
      public String getPassword() {
        return endpoint.getPassword();
      }
    })) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectOnResp3Protocol() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .protocol(RedisProtocol.RESP3).build())) {
      assertEquals("PONG", jedis.ping());
      assertEquals(RedisProtocol.RESP3, jedis.getConnection().getRedisProtocol());
    }
  }

  @Test
  public void connectOnResp3ProtocolShortcut() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .resp3().build())) {
      assertEquals("PONG", jedis.ping());
      assertEquals(RedisProtocol.RESP3, jedis.getConnection().getRedisProtocol());
    }
  }

  @Test
  public void timeoutConnection() throws Exception {
    final String TIMEOUT_STR = "timeout";

    Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().timeoutMillis(15000).build());

    // read current config
    final String timeout = jedis.configGet(TIMEOUT_STR).get(TIMEOUT_STR);
    try {
      jedis.configSet("timeout", "1");
      Thread.sleep(5000);
      try {
        jedis.hmget("foobar", "foo");
        fail("Operation should throw JedisConnectionException");
      } catch (JedisConnectionException jce) {
        // expected
      }
      jedis.close();
    } finally {
      // reset config
      jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
      jedis.configSet(TIMEOUT_STR, timeout);
      jedis.close();
    }
  }

  @Test
  public void infiniteTimeout() throws Exception {
    try (Jedis timeoutJedis = new Jedis(endpoint.getHost(), endpoint.getPort(), 200, 200, 200)) {
      timeoutJedis.auth(endpoint.getPassword());
      try {
        timeoutJedis.blpop(0, "foo");
        fail("SocketTimeoutException should occur");
      } catch (JedisConnectionException jce) {
        assertSame(java.net.SocketTimeoutException.class, jce.getCause().getClass());
        assertEquals("Read timed out", jce.getCause().getMessage());
        assertTrue(timeoutJedis.isBroken());
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void failWhenSendingNullValues() {
    jedis.set("foo", null);
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    Jedis j = new Jedis(new URI("localhost:6380"));
  }
//
//  @Test
//  public void shouldReconnectToSameDB() throws IOException {
//    jedis.select(1);
//    jedis.set("foo", "bar");
//    jedis.getClient().getSocket().shutdownInput();
//    jedis.getClient().getSocket().shutdownOutput();
//    assertEquals("bar", jedis.get("foo"));
//  }

  @Test
  public void connectWithUrl() {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone1");
    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      j.auth(endpoint.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (Jedis j2 = new Jedis(
        endpoint.getURIBuilder().defaultCredentials().path("/2").build().toString())) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void connectWithUri() throws URISyntaxException {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone1");
    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      j.auth(endpoint.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (Jedis jedis = new Jedis(
        endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", jedis.ping());
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void connectWithUrlOnResp3() {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone1");

    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      j.auth(endpoint.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (Jedis j2 = new Jedis(
        endpoint.getURIBuilder().defaultCredentials().path("/2?protocol=3").build().toString())) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void connectWithUriOnResp3() throws URISyntaxException {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone1");

    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      j.auth(endpoint.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (Jedis jedis = new Jedis(
        endpoint.getURIBuilder().defaultCredentials().path("/2?protocol=3").build())) {
      assertEquals("PONG", jedis.ping());
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void shouldNotUpdateDbIndexIfSelectFails() {
    int currentDb = jedis.getDB();
    try {
      int invalidDb = -1;
      jedis.select(invalidDb);

      fail("Should throw an exception if tried to select invalid db");
    } catch (JedisException e) {
      assertEquals(currentDb, jedis.getDB());
    }
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() {
    EndpointConfig endpointStandalone1 = HostAndPorts.getRedisEndpoint("standalone1");

    try (Jedis j1 = new Jedis(endpointStandalone1.getURI().toString())) {
      j1.auth(endpointStandalone1.getPassword());
//      assertEquals("localhost", j1.getClient().getHost());
//      assertEquals(6380, j1.getClient().getPort());
      assertEquals(0, j1.getDB());
    }

    try (Jedis j2 = new Jedis(endpointStandalone1.getURI().toString())) {
      j2.auth(endpointStandalone1.getPassword());
//      assertEquals("localhost", j2.getClient().getHost());
//      assertEquals(6380, j2.getClient().getPort());
      assertEquals(0, j2.getDB());
    }
  }

  @Test
  public void uriWithDBindexShouldUseTimeout() throws URISyntaxException, IOException {
    int fakePort = 6378;
    int timeoutMillis = 3250;
    int deltaMillis = 500;
    URI uri = new URI(String.format("redis://localhost:%d/1", fakePort));
    Instant start = Instant.now();

    try (ServerSocket server = new ServerSocket(fakePort);
        Jedis jedis = new Jedis(uri, timeoutMillis)) {
      fail("Jedis should fail to connect to a fake port");
    } catch (JedisConnectionException ex) {
      assertSame(SocketTimeoutException.class, ex.getCause().getClass());
      assertEquals(timeoutMillis, Duration.between(start, Instant.now()).toMillis(), deltaMillis);
    }
  }

  @Test
  public void checkCloseable() {
    Jedis bj = new Jedis();
    bj.close();
  }

  @Test
  public void checkCloseableAfterConnect() {
    Jedis bj = new Jedis();
    bj.connect();
    bj.close();
  }

  @Test
  public void checkCloseableAfterCommand() {
    Jedis bj = new Jedis();
    bj.auth(endpoint.getPassword());
    bj.close();
  }

  @Test
  public void checkDisconnectOnQuit() {
    jedis.disconnect();
    assertFalse(jedis.isConnected());
  }

  @Test
  public void clientSetInfoDefault() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .clientSetInfoConfig(ClientSetInfoConfig.DEFAULT).build())) {
      assertEquals("PONG", jedis.ping());
      String info = jedis.clientInfo();
      assertTrue(info.contains("lib-name=" + JedisMetaInfo.getArtifactId()));
      assertTrue(info.contains("lib-ver=" + JedisMetaInfo.getVersion()));
    }
  }

  @Test
  public void clientSetInfoDisabled() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED).build())) {
      assertEquals("PONG", jedis.ping());
      String info = jedis.clientInfo();
      assertFalse(info.contains("lib-name=" + JedisMetaInfo.getArtifactId()));
      assertFalse(info.contains("lib-ver=" + JedisMetaInfo.getVersion()));
    }
  }

  @Test
  public void clientSetInfoLibNameSuffix() {
    final String libNameSuffix = "for-redis";
    ClientSetInfoConfig setInfoConfig = ClientSetInfoConfig.withLibNameSuffix(libNameSuffix);
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .clientSetInfoConfig(setInfoConfig).build())) {
      assertEquals("PONG", jedis.ping());
      String info = jedis.clientInfo();
      assertTrue(info.contains("lib-name=" + JedisMetaInfo.getArtifactId() + '(' + libNameSuffix + ')'));
      assertTrue(info.contains("lib-ver=" + JedisMetaInfo.getVersion()));
    }
  }

}
