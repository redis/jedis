package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.params.ClientKillParams.SkipMe;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.resps.TrackingInfo;

@RunWith(Parameterized.class)
public class ClientCommandsTest extends JedisCommandsTestBase {

  private final String clientName = "fancy_jedis_name";
  private final Pattern pattern = Pattern.compile("\\bname=" + clientName + "\\b");

  private Jedis client;

  public ClientCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    client = new Jedis(endpoint.getHost(), endpoint.getPort(), 500);
    client.auth(endpoint.getPassword());
    client.clientSetname(clientName);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    client.close();
    super.tearDown();
  }

  @Test
  public void nameString() {
    String name = "string";
    client.clientSetname(name);
    assertEquals(name, client.clientGetname());
  }

  @Test
  public void nameBinary() {
    byte[] name = "binary".getBytes();
    client.clientSetname(name);
    assertArrayEquals(name, client.clientGetnameBinary());
  }

  @Test
  public void clientSetInfoCommand() {
    String libName = "Jedis::A-Redis-Java-library";
    String libVersion = "999.999.999";
    assertEquals("OK", client.clientSetInfo(ClientAttributeOption.LIB_NAME, libName));
    assertEquals("OK", client.clientSetInfo(ClientAttributeOption.LIB_VER, libVersion));
    String info = client.clientInfo();
    assertTrue(info.contains("lib-name=" + libName));
    assertTrue(info.contains("lib-ver=" + libVersion));
  }

  @Test
  public void clientId() {
    long clientId = client.clientId();

    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bid=(\\d+)\\b").matcher(info);
    matcher.find();

    assertEquals(clientId, Long.parseLong(matcher.group(1)));
  }

  @Test
  public void clientIdmultipleConnection() {
    try (Jedis client2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500)) {
      client2.auth(endpoint.getPassword());
      client2.clientSetname("fancy_jedis_another_name");

      // client-id is monotonically increasing
      assertTrue(client.clientId() < client2.clientId());
    }
  }

  @Test
  public void clientIdReconnect() {
    long clientIdInitial = client.clientId();
    client.disconnect();
    client.connect();
    client.auth(endpoint.getPassword());
    long clientIdAfterReconnect = client.clientId();

    assertTrue(clientIdInitial < clientIdAfterReconnect);
  }

  @Test
  public void clientUnblock() throws InterruptedException, TimeoutException {
    long clientId = client.clientId();
    assertEquals(0, jedis.clientUnblock(clientId, UnblockType.ERROR));
    Future<?> future = Executors.newSingleThreadExecutor().submit(() -> client.brpop(100000, "foo"));

    try {
      // to make true command already executed
      TimeUnit.MILLISECONDS.sleep(500);
      assertEquals(1, jedis.clientUnblock(clientId, UnblockType.ERROR));
      future.get(1, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      assertEquals("redis.clients.jedis.exceptions.JedisDataException: UNBLOCKED client unblocked via CLIENT UNBLOCK", e.getMessage());
    }
  }

  @Test
  public void killIdString() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bid=(\\d+)\\b").matcher(info);
    matcher.find();
    String id = matcher.group(1);

    assertEquals(1, jedis.clientKill(new ClientKillParams().id(id)));

    assertDisconnected(client);
  }

  @Test
  public void killIdBinary() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bid=(\\d+)\\b").matcher(info);
    matcher.find();
    byte[] id = matcher.group(1).getBytes();

    assertEquals(1, jedis.clientKill(new ClientKillParams().id(id)));

    assertDisconnected(client);
  }

  @Test
  public void killTypeNormal() {
    long clients = jedis.clientKill(new ClientKillParams().type(ClientType.NORMAL));
    assertTrue(clients > 0);
    assertDisconnected(client);
  }

  @Test
  public void killSkipmeNo() {
    jedis.clientKill(new ClientKillParams().type(ClientType.NORMAL).skipMe(SkipMe.NO));
    assertDisconnected(client);
    assertDisconnected(jedis);
  }

  @Test
  public void killSkipmeYesNo() {
    jedis.clientKill(new ClientKillParams().type(ClientType.NORMAL).skipMe(SkipMe.YES));
    assertDisconnected(client);
    assertEquals(1, jedis.clientKill(new ClientKillParams().type(ClientType.NORMAL).skipMe(SkipMe.NO)));
    assertDisconnected(jedis);
  }

  @Test
  public void killAddrString() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\baddr=(\\S+)\\b").matcher(info);
    matcher.find();
    String addr = matcher.group(1);

    assertEquals(1, jedis.clientKill(new ClientKillParams().addr(addr)));

    assertDisconnected(client);
  }

  @Test
  public void killAddrBinary() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\baddr=(\\S+)\\b").matcher(info);
    matcher.find();
    String addr = matcher.group(1);

    assertEquals(1, jedis.clientKill(new ClientKillParams().addr(addr)));

    assertDisconnected(client);
  }

  @Test
  public void killLAddr() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bladdr=(\\S+)\\b").matcher(info);
    matcher.find();
    String laddr = matcher.group(1);

    long clients = jedis.clientKill(new ClientKillParams().laddr(laddr));
    assertTrue(clients >= 1);

    assertDisconnected(client);
  }

  @Test
  public void killAddrIpPort() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\baddr=(\\S+)\\b").matcher(info);
    matcher.find();
    String addr = matcher.group(1);
    int lastColon = addr.lastIndexOf(":");
    String[] hp = new String[]{addr.substring(0, lastColon), addr.substring(lastColon + 1)};

    assertEquals(1, jedis.clientKill(new ClientKillParams().addr(hp[0], Integer.parseInt(hp[1]))));

    assertDisconnected(client);
  }

  @Test
  public void killUser() {
    client.aclSetUser("test_kill", "on", "+acl", ">password1");
    try (Jedis client2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500)) {
      client2.auth("test_kill", "password1");

      assertEquals(1, jedis.clientKill(new ClientKillParams().user("test_kill")));
      assertDisconnected(client2);
    } finally {
      jedis.aclDelUser("test_kill");
    }
  }

  @Test
  public void killMaxAge() throws InterruptedException {
    long maxAge = 2;

    // sleep twice the maxAge, to be sure
    Thread.sleep(maxAge * 2 * 1000);

    try (Jedis client2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500)) {
      client2.auth(endpoint.getPassword());

      long killedClients = jedis.clientKill(new ClientKillParams().maxAge(maxAge));

      // The reality is that some tests leak clients, so we can't assert
      // on the exact number of killed clients.
      assertTrue(killedClients > 0);

      assertDisconnected(client);
      assertConnected(client2);
    }
  }

  @Test
  public void clientInfo() {
    String info = client.clientInfo();
    assertNotNull(info);
    assertEquals(1, info.split("\n").length);
    assertTrue(info.contains(clientName));
  }

  @Test
  public void clientListWithClientId() {
    long id = client.clientId();
    String listInfo = jedis.clientList(id);
    assertNotNull(listInfo);
    assertTrue(listInfo.contains(clientName));
  }

  @Test
  public void listWithType() {
    assertTrue(client.clientList(ClientType.NORMAL).split("\\n").length > 1);
    assertEquals(0, client.clientList(ClientType.MASTER).length());
    assertEquals(1, client.clientList(ClientType.SLAVE).split("\\n").length);
    assertEquals(1, client.clientList(ClientType.REPLICA).split("\\n").length);
    assertEquals(1, client.clientList(ClientType.PUBSUB).split("\\n").length);
  }

  @Test
  public void trackingInfo() {
    TrackingInfo trackingInfo = client.clientTrackingInfo();

    assertEquals(1, trackingInfo.getFlags().size());
    assertEquals(-1, trackingInfo.getRedirect());
    assertEquals(0, trackingInfo.getPrefixes().size());
  }

  @Test
  public void trackingInfoResp3() {
    Jedis clientResp3 = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
            .protocol(RedisProtocol.RESP3).build());
    TrackingInfo trackingInfo = clientResp3.clientTrackingInfo();

    assertEquals(1, trackingInfo.getFlags().size());
    assertEquals(-1, trackingInfo.getRedirect());
    assertEquals(0, trackingInfo.getPrefixes().size());
  }

  private void assertDisconnected(Jedis j) {
    try {
      j.ping();
      fail("Jedis connection should be disconnected");
    } catch (JedisConnectionException jce) {
      // should be here
    }
  }

  private void assertConnected(Jedis j) {
    assertEquals("PONG", j.ping());
  }

  private String findInClientList() {
    for (String clientInfo : jedis.clientList().split("\n")) {
      if (pattern.matcher(clientInfo).find()) {
        return clientInfo;
      }
    }
    return null;
  }
}
