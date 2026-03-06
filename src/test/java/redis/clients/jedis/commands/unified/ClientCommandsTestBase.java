package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ClientAttributeOption;
import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.resps.TrackingInfo;

@Tag("integration")
public abstract class ClientCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public ClientCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  // clientId test
  @Test
  public void clientId() {
    long id = jedis.clientId();
    assertTrue(id > 0, "Client ID should be positive");
  }

  // clientGetname and clientSetname tests
  @Test
  public void clientSetnameAndGetname() {
    String result = jedis.clientSetname("test-client-name");
    assertEquals("OK", result);

    String name = jedis.clientGetname();
    assertEquals("test-client-name", name);

    // Clean up - set to empty to reset
    jedis.clientSetname("");
  }

  // clientList tests
  @Test
  public void clientList() {
    String list = jedis.clientList();
    assertNotNull(list);
    assertTrue(list.length() > 0, "Client list should not be empty");
  }

  @Test
  public void clientListByType() {
    String list = jedis.clientList(ClientType.NORMAL);
    assertNotNull(list);
  }

  @Test
  public void clientListByIds() {
    long clientId = jedis.clientId();
    String list = jedis.clientList(clientId);
    assertNotNull(list);
    assertTrue(list.contains("id=" + clientId));
  }

  // clientInfo test
  @Test
  public void clientInfo() {
    String info = jedis.clientInfo();
    assertNotNull(info);
    assertTrue(info.length() > 0, "Client info should not be empty");
  }

  // clientSetInfo test
  @Test
  public void clientSetInfo() {
    String result = jedis.clientSetInfo(ClientAttributeOption.LIB_NAME, "jedis-test");
    assertEquals("OK", result);
  }

  // clientKill tests
  @Test
  public void clientKillWithParams() {
    // Kill with params targeting a non-existent client (using SKIPME yes to avoid killing
    // ourselves)
    ClientKillParams params = ClientKillParams.clientKillParams()
        .skipMe(ClientKillParams.SkipMe.YES).type(ClientType.NORMAL);

    // Should return 0 or more (may kill other test connections)
    long killed = jedis.clientKill(params);
    assertTrue(killed >= 0);
  }

  // clientPause and clientUnpause tests
  @Test
  public void clientPauseAndUnpause() {
    String pauseResult = jedis.clientPause(100);
    assertEquals("OK", pauseResult);

    String unpauseResult = jedis.clientUnpause();
    assertEquals("OK", unpauseResult);
  }

  @Test
  public void clientPauseWithMode() {
    String pauseResult = jedis.clientPause(100, ClientPauseMode.WRITE);
    assertEquals("OK", pauseResult);

    String unpauseResult = jedis.clientUnpause();
    assertEquals("OK", unpauseResult);
  }

  // clientUnblock test
  @Test
  public void clientUnblock() {
    long clientId = jedis.clientId();
    // Unblocking a client that is not blocked should return 0
    long result = jedis.clientUnblock(clientId);
    assertEquals(0, result);
  }

  @Test
  public void clientUnblockWithType() {
    long clientId = jedis.clientId();
    // Unblocking a client that is not blocked should return 0
    long result = jedis.clientUnblock(clientId, UnblockType.TIMEOUT);
    assertEquals(0, result);
  }

  // clientNoEvict tests
  @Test
  public void clientNoEvictOnOff() {
    String onResult = jedis.clientNoEvictOn();
    assertEquals("OK", onResult);

    String offResult = jedis.clientNoEvictOff();
    assertEquals("OK", offResult);
  }

  // clientNoTouch tests
  @Test
  public void clientNoTouchOnOff() {
    String onResult = jedis.clientNoTouchOn();
    assertEquals("OK", onResult);

    String offResult = jedis.clientNoTouchOff();
    assertEquals("OK", offResult);
  }

  // clientTrackingInfo test
  @Test
  public void clientTrackingInfo() {
    TrackingInfo info = jedis.clientTrackingInfo();
    assertNotNull(info);
  }
}
