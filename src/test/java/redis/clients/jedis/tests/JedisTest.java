package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.util.SafeEncoder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JedisTest extends JedisCommandTestBase {

  public static final String PASSWORD = "foobared";
  public static final String LOCALHOST = "localhost";

  @Test
  public void useWithoutConnecting() {
    Jedis jedis = new Jedis(ClientOptions.builder().withPassword(PASSWORD).build());
    jedis.dbSize();
  }

  @Test
  public void checkBinaryData() {
    byte[] bigdata = new byte[1777];
    for (int b = 0; b < bigdata.length; b++) {
      bigdata[b] = (byte) ((byte) b % 255);
    }
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("data", SafeEncoder.encode(bigdata));

    String status = jedis.hmset("foo", hash);
    assertEquals("OK", status);
    assertEquals(hash, jedis.hgetAll("foo"));
  }

  @Test
  public void connectWithShardInfo() {
    JedisShardInfo shardInfo = new JedisShardInfo("localhost", Protocol.DEFAULT_PORT);
    shardInfo.setPassword("foobared");
    Jedis jedis = new Jedis(shardInfo);
    jedis.get("foo");
  }

  @Test(expected = JedisConnectionException.class)
  public void timeoutConnection() throws Exception {
    ClientOptions clientOptions = ClientOptions.builder().withHost("localhost").withPort(6379).withTimeout(15000).build();
    jedis = new Jedis(clientOptions);
    jedis.auth("foobared");
    jedis.configSet("timeout", "1");
    Thread.sleep(2000);
    jedis.hmget("foobar", "foo");
  }

  @Test(expected = JedisConnectionException.class)
  public void timeoutConnectionWithURI() throws Exception {
    ClientOptions clientOptions = ClientOptions.builder().withURI("redis://:foobared@localhost:6380/2").withTimeout(15000).build();
    jedis = new Jedis(clientOptions);
    jedis.configSet("timeout", "1");
    Thread.sleep(2000);
    jedis.hmget("foobar", "foo");
  }

  @Test(expected = JedisDataException.class)
  public void failWhenSendingNullValues() {
    jedis.set("foo", null);
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    Jedis j = new Jedis(ClientOptions.builder().withURI("localhost:6380").build());
    j.ping();
  }

  @Test
  public void shouldReconnectToSameDB() throws IOException {
    jedis.select(1);
    jedis.set("foo", "bar");
    jedis.getClient().getSocket().shutdownInput();
    jedis.getClient().getSocket().shutdownOutput();
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void startWithUrlString() {
    Jedis j = new Jedis(ClientOptions.builder().withPort(6379).build());
    j.auth("foobared");
    j.select(2);
    j.set("foo", "bar");
    Jedis jedis = new Jedis(ClientOptions.builder().withURI("redis://:foobared@localhost:6379/2").build());
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void shouldNotUpdateDbIndexIfSelectFails() throws URISyntaxException {
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
    Jedis jedis = new Jedis(ClientOptions.builder().withURI("redis://localhost:6380").build());
    jedis.auth("foobared");
    assertEquals(jedis.getClient().getClientOptions().getHost(), "localhost");
    assertEquals(jedis.getClient().getClientOptions().getPort(), 6380);
    assertEquals(jedis.getDB(), 0);

    jedis = new Jedis(ClientOptions.builder().withURI("redis://localhost:6380/").build());
    jedis.auth("foobared");
    assertEquals(jedis.getClient().getClientOptions().getHost(), "localhost");
    assertEquals(jedis.getClient().getClientOptions().getPort(), 6380);
    assertEquals(jedis.getDB(), 0);
  }

  @Test
  public void checkCloseable() {
    jedis.close();
    BinaryJedis bj = new BinaryJedis(ClientOptions.builder().build());
    bj.connect();
    bj.close();
  }

  @Test
  public void checkDisconnectOnQuit() {
    jedis.quit();
    assertFalse(jedis.getClient().isConnected());
  }

}