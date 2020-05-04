package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.params.ClientKillParams.Type;
import static redis.clients.jedis.params.ClientKillParams.SkipMe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ClientKillParams;

public class ClientCommandsTest extends JedisCommandTestBase {

  private final String clientName = "fancy_jedis_name";
  private final Pattern pattern = Pattern.compile("\\bname=" + clientName + "\\b");

  private Jedis client;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    client = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    client.auth("foobared");
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
  public void clientId() {
    Long clientId = client.clientId();
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bid=(\\d+)\\b").matcher(info);
    matcher.find();

    Long longId = Long.parseLong(matcher.group(1));

    assertEquals(clientId, longId);
  }

  @Test
  public void clientIdmultipleConnection() {
    Jedis client2 = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    client2.auth("foobared");
    client2.clientSetname("fancy_jedis_another_name");

    long clientId1 = client.clientId();
    long clientId2 = client2.clientId();

    ///client-id is monotonically increasing
    assertTrue(clientId1 < clientId2);

    client2.close();
  }

  @Test
  public void clientIdReconnect() {
    long clientIdInitial = client.clientId();
    client.disconnect();
    client.connect();
    long clientIdAfterReconnect = client.clientId();

    assertTrue(clientIdInitial < clientIdAfterReconnect);
  }

  @Test
  public void killIdString() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bid=(\\d+)\\b").matcher(info);
    matcher.find();
    String id = matcher.group(1);

    long clients = jedis.clientKill(new ClientKillParams().id(id));
    assertEquals(1, clients);

    assertDisconnected(client);
  }

  @Test
  public void killIdBinary() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\bid=(\\d+)\\b").matcher(info);
    matcher.find();
    byte[] id = matcher.group(1).getBytes();

    long clients = jedis.clientKill(new ClientKillParams().id(id));
    assertEquals(1, clients);

    assertDisconnected(client);
  }

  @Test
  public void killTypeNormal() {
    long clients = jedis.clientKill(new ClientKillParams().type(Type.NORMAL));
    assertTrue(clients > 0);
    assertDisconnected(client);
  }

  @Test
  public void killSkipmeNo() {
    jedis.clientKill(new ClientKillParams().type(Type.NORMAL).skipMe(SkipMe.NO));
    assertDisconnected(client);
    assertDisconnected(jedis);
  }

  @Test
  public void killSkipmeYesNo() {
    jedis.clientKill(new ClientKillParams().type(Type.NORMAL).skipMe(SkipMe.YES));
    assertDisconnected(client);
    long clients = jedis.clientKill(new ClientKillParams().type(Type.NORMAL).skipMe(SkipMe.NO));
    assertEquals(1, clients);
    assertDisconnected(jedis);
  }

  @Test
  public void killAddrString() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\baddr=(\\S+)\\b").matcher(info);
    matcher.find();
    String addr = matcher.group(1);

    long clients = jedis.clientKill(new ClientKillParams().addr(addr));
    assertEquals(1, clients);

    assertDisconnected(client);
  }

  @Test
  public void killAddrBinary() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\baddr=(\\S+)\\b").matcher(info);
    matcher.find();
    String addr = matcher.group(1);

    long clients = jedis.clientKill(new ClientKillParams().addr(addr));
    assertEquals(1, clients);

    assertDisconnected(client);
  }

  @Test
  public void killAddrIpPort() {
    String info = findInClientList();
    Matcher matcher = Pattern.compile("\\baddr=(\\S+)\\b").matcher(info);
    matcher.find();
    String addr = matcher.group(1);
    String[] hp = HostAndPort.extractParts(addr);

    long clients = jedis.clientKill(new ClientKillParams().addr(hp[0], Integer.parseInt(hp[1])));
    assertEquals(1, clients);

    assertDisconnected(client);
  }

  private void assertDisconnected(Jedis j) {    
    try {
      j.ping();
      fail("Jedis connection should be disconnected");
    } catch(JedisConnectionException jce) {
      // should be here
    } 
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
