package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.MigrateParams;

@RunWith(Parameterized.class)
public class MigrateTest extends JedisCommandsTestBase {

  private static final byte[] bfoo = { 0x01, 0x02, 0x03 };
  private static final byte[] bbar = { 0x04, 0x05, 0x06 };
  private static final byte[] bfoo1 = { 0x07, 0x08, 0x01 };
  private static final byte[] bbar1 = { 0x09, 0x00, 0x01 };
  private static final byte[] bfoo2 = { 0x07, 0x08, 0x02 };
  private static final byte[] bbar2 = { 0x09, 0x00, 0x02 };
  private static final byte[] bfoo3 = { 0x07, 0x08, 0x03 };
  private static final byte[] bbar3 = { 0x09, 0x00, 0x03 };

  private Jedis dest;
  private Jedis destAuth;

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl");

  private static final EndpointConfig destEndpoint = HostAndPorts.getRedisEndpoint(
      "standalone7-with-lfu-policy");

  private static final EndpointConfig destEndpointWithAuth = HostAndPorts.getRedisEndpoint(
      "standalone1");

  private static final String host = destEndpoint.getHost();
  private static final int port = destEndpoint.getPort();
  private static final int portAuth = destEndpointWithAuth.getPort();
  private static final int db = 2;
  private static final int dbAuth = 3;
  private static final int timeout = Protocol.DEFAULT_TIMEOUT;

  public MigrateTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    dest = new Jedis(host, port, 500);
    dest.flushAll();
    dest.select(db);

    destAuth = new Jedis(destEndpointWithAuth.getHostAndPort(),
        destEndpointWithAuth.getClientConfigBuilder().build());
    destAuth.flushAll();
    destAuth.select(dbAuth);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    dest.close();
    destAuth.close();
    super.tearDown();
  }

  @Test
  public void nokey() {
    assertEquals("NOKEY", jedis.migrate(host, port, "foo", db, timeout));
    assertEquals("NOKEY", jedis.migrate(host, port, bfoo, db, timeout));
    assertEquals("NOKEY",
      jedis.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3"));
    assertEquals("NOKEY",
      jedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3));
  }

  @Test
  public void migrate() {
    jedis.set("foo", "bar");
    assertEquals("OK", jedis.migrate(host, port, "foo", db, timeout));
    assertEquals("bar", dest.get("foo"));
    assertNull(jedis.get("foo"));

    jedis.set(bfoo, bbar);
    assertEquals("OK", jedis.migrate(host, port, bfoo, db, timeout));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertNull(jedis.get(bfoo));
  }

  @Test
  public void migrateEmptyParams() {
    jedis.set("foo", "bar");
    assertEquals("OK", jedis.migrate(host, port, db, timeout, new MigrateParams(), "foo"));
    assertEquals("bar", dest.get("foo"));
    assertNull(jedis.get("foo"));

    jedis.set(bfoo, bbar);
    assertEquals("OK", jedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertNull(jedis.get(bfoo));
  }

  @Test
  public void migrateCopy() {
    jedis.set("foo", "bar");
    assertEquals("OK", jedis.migrate(host, port, db, timeout, new MigrateParams().copy(), "foo"));
    assertEquals("bar", dest.get("foo"));
    assertEquals("bar", jedis.get("foo"));

    jedis.set(bfoo, bbar);
    assertEquals("OK", jedis.migrate(host, port, db, timeout, new MigrateParams().copy(), bfoo));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertArrayEquals(bbar, jedis.get(bfoo));
  }

  @Test
  public void migrateReplace() {
    jedis.set("foo", "bar1");
    dest.set("foo", "bar2");
    assertEquals("OK", jedis.migrate(host, port, db, timeout, new MigrateParams().replace(), "foo"));
    assertEquals("bar1", dest.get("foo"));
    assertNull(jedis.get("foo"));

    jedis.set(bfoo, bbar1);
    dest.set(bfoo, bbar2);
    assertEquals("OK", jedis.migrate(host, port, db, timeout, new MigrateParams().replace(), bfoo));
    assertArrayEquals(bbar1, dest.get(bfoo));
    assertNull(jedis.get(bfoo));
  }

  @Test
  public void migrateCopyReplace() {
    jedis.set("foo", "bar1");
    dest.set("foo", "bar2");
    assertEquals("OK",
      jedis.migrate(host, port, db, timeout, new MigrateParams().copy().replace(), "foo"));
    assertEquals("bar1", dest.get("foo"));
    assertEquals("bar1", jedis.get("foo"));

    jedis.set(bfoo, bbar1);
    dest.set(bfoo, bbar2);
    assertEquals("OK",
      jedis.migrate(host, port, db, timeout, new MigrateParams().copy().replace(), bfoo));
    assertArrayEquals(bbar1, dest.get(bfoo));
    assertArrayEquals(bbar1, jedis.get(bfoo));
  }

  @Test
  public void migrateAuth() {
    jedis.set("foo", "bar");
    assertEquals("OK", jedis.migrate(host, portAuth, dbAuth, timeout,
        new MigrateParams().auth(destEndpointWithAuth.getPassword()), "foo"));
    assertEquals("bar", destAuth.get("foo"));
    assertNull(jedis.get("foo"));

    jedis.set(bfoo, bbar);
    assertEquals("OK", jedis.migrate(host, portAuth, dbAuth, timeout,
        new MigrateParams().auth(destEndpointWithAuth.getPassword()), bfoo));
    assertArrayEquals(bbar, destAuth.get(bfoo));
    assertNull(jedis.get(bfoo));
  }

  @Test
  public void migrateAuth2() {
    destAuth.set("foo", "bar");
    assertEquals("OK", destAuth.migrate(host, endpoint.getPort(), 0, timeout,
        new MigrateParams().auth2(endpoint.getUsername(), endpoint.getPassword()), "foo"));
    assertEquals("bar", jedis.get("foo"));
    assertNull(destAuth.get("foo"));

    // binary
    dest.set(bfoo1, bbar1);
    assertEquals("OK", dest.migrate(host, endpoint.getPort(), 0, timeout,
        new MigrateParams().auth2(endpoint.getUsername(), endpoint.getPassword()), bfoo1));
    assertArrayEquals(bbar1, jedis.get(bfoo1));
    assertNull(dest.get(bfoo1));
  }

  @Test
  public void migrateCopyReplaceAuth() {
    jedis.set("foo", "bar1");
    destAuth.set("foo", "bar2");
    assertEquals("OK", jedis.migrate(host, portAuth, dbAuth, timeout,
        new MigrateParams().copy().replace().auth(destEndpointWithAuth.getPassword()), "foo"));
    assertEquals("bar1", destAuth.get("foo"));
    assertEquals("bar1", jedis.get("foo"));

    jedis.set(bfoo, bbar1);
    destAuth.set(bfoo, bbar2);
    assertEquals(
      "OK",
      jedis.migrate(host, portAuth, dbAuth, timeout,
        new MigrateParams().copy().replace().auth(destEndpointWithAuth.getPassword()), bfoo));
    assertArrayEquals(bbar1, destAuth.get(bfoo));
    assertArrayEquals(bbar1, jedis.get(bfoo));
  }

  @Test
  public void migrateMulti() {
    jedis.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    assertEquals("OK",
      jedis.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3"));
    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar2", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));

    jedis.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    assertEquals("OK",
      jedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3));
    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar2, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

  @Test
  public void migrateConflict() {
    jedis.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    dest.set("foo2", "bar");
    try {
      jedis.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3");
      fail("Should get BUSYKEY error");
    } catch (JedisDataException jde) {
      assertTrue(jde.getMessage().contains("BUSYKEY"));
    }
    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));

    jedis.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    dest.set(bfoo2, bbar);
    try {
      jedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3);
      fail("Should get BUSYKEY error");
    } catch (JedisDataException jde) {
      assertTrue(jde.getMessage().contains("BUSYKEY"));
    }
    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

}
