package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.MigrateParams;

@RunWith(Parameterized.class)
public class MigratePipeliningTest extends JedisCommandsTestBase {

  private static final byte[] bfoo = { 0x01, 0x02, 0x03 };
  private static final byte[] bbar = { 0x04, 0x05, 0x06 };
  private static final byte[] bfoo1 = { 0x07, 0x08, 0x01 };
  private static final byte[] bbar1 = { 0x09, 0x00, 0x01 };
  private static final byte[] bfoo2 = { 0x07, 0x08, 0x02 };
  private static final byte[] bbar2 = { 0x09, 0x00, 0x02 };
  private static final byte[] bfoo3 = { 0x07, 0x08, 0x03 };
  private static final byte[] bbar3 = { 0x09, 0x00, 0x03 };

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

  private Jedis dest;
  private Jedis destAuth;

  public MigratePipeliningTest(RedisProtocol protocol) {
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
  public void noKey() {
    Pipeline p = jedis.pipelined();

    p.migrate(host, port, "foo", db, timeout);
    p.migrate(host, port, bfoo, db, timeout);
    p.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3");
    p.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3);

    assertThat(p.syncAndReturnAll(),
        contains("NOKEY", "NOKEY", "NOKEY", "NOKEY"));
  }

  @Test
  public void migrate() {
    assertNull(dest.get("foo"));

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar");
    p.migrate(host, port, "foo", db, timeout);
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertEquals("bar", dest.get("foo"));
  }

  @Test
  public void migrateBinary() {
    assertNull(dest.get(bfoo));

    Pipeline p = jedis.pipelined();

    p.set(bfoo, bbar);
    p.migrate(host, port, bfoo, db, timeout);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertArrayEquals(bbar, dest.get(bfoo));
  }

  @Test
  public void migrateEmptyParams() {
    assertNull(dest.get("foo"));

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar");
    p.migrate(host, port, db, timeout, new MigrateParams(), "foo");
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertEquals("bar", dest.get("foo"));
  }

  @Test
  public void migrateEmptyParamsBinary() {
    assertNull(dest.get(bfoo));

    Pipeline p = jedis.pipelined();

    p.set(bfoo, bbar);
    p.migrate(host, port, db, timeout, new MigrateParams(), bfoo);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertArrayEquals(bbar, dest.get(bfoo));
  }

  @Test
  public void migrateCopy() {
    assertNull(dest.get("foo"));

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar");
    p.migrate(host, port, db, timeout, new MigrateParams().copy(), "foo");
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", "bar"));

    assertEquals("bar", dest.get("foo"));
  }

  @Test
  public void migrateCopyBinary() {
    assertNull(dest.get(bfoo));

    Pipeline p = jedis.pipelined();

    p.set(bfoo, bbar);
    p.migrate(host, port, db, timeout, new MigrateParams().copy(), bfoo);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", bbar));

    assertArrayEquals(bbar, dest.get(bfoo));
  }

  @Test
  public void migrateReplace() {
    dest.set("foo", "bar2");

    assertEquals("bar2", dest.get("foo"));

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar1");
    p.migrate(host, port, db, timeout, new MigrateParams().replace(), "foo");
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertEquals("bar1", dest.get("foo"));
  }

  @Test
  public void migrateReplaceBinary() {
    dest.set(bfoo, bbar2);

    assertArrayEquals(bbar2, dest.get(bfoo));

    Pipeline p = jedis.pipelined();

    p.set(bfoo, bbar1);
    p.migrate(host, port, db, timeout, new MigrateParams().replace(), bfoo);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertArrayEquals(bbar1, dest.get(bfoo));
  }

  @Test
  public void migrateCopyReplace() {
    dest.set("foo", "bar2");

    assertEquals("bar2", dest.get("foo"));

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar1");
    p.migrate(host, port, db, timeout, new MigrateParams().copy().replace(), "foo");
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", "bar1"));

    assertEquals("bar1", dest.get("foo"));
  }

  @Test
  public void migrateCopyReplaceBinary() {
    dest.set(bfoo, bbar2);

    assertArrayEquals(bbar2, dest.get(bfoo));

    Pipeline p = jedis.pipelined();

    p.set(bfoo, bbar1);
    p.migrate(host, port, db, timeout, new MigrateParams().copy().replace(), bfoo);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", bbar1));

    assertArrayEquals(bbar1, dest.get(bfoo));
  }

  @Test
  public void migrateAuth() {
    assertNull(dest.get("foo"));

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar");
    p.migrate(host, portAuth, dbAuth, timeout,
        new MigrateParams().auth(destEndpointWithAuth.getPassword()), "foo");
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertEquals("bar", destAuth.get("foo"));
  }

  @Test
  public void migrateAuthBinary() {
    assertNull(dest.get(bfoo));

    Pipeline p = jedis.pipelined();

    p.set(bfoo, bbar);
    p.migrate(host, portAuth, dbAuth, timeout,
        new MigrateParams().auth(destEndpointWithAuth.getPassword()), bfoo);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertArrayEquals(bbar, destAuth.get(bfoo));
  }

  @Test
  public void migrateAuth2() {
    assertNull(jedis.get("foo"));

    Pipeline p = destAuth.pipelined();

    p.set("foo", "bar");
    p.migrate(endpoint.getHost(), endpoint.getPort(), 0, timeout,
        new MigrateParams().auth2(endpoint.getUsername(), endpoint.getPassword()),
        "foo");
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void migrateAuth2Binary() {
    assertNull(jedis.get(bfoo));

    Pipeline p = dest.pipelined();

    p.set(bfoo, bbar);
    p.migrate(endpoint.getHost(), endpoint.getPort(), 0, timeout,
        new MigrateParams().auth2(endpoint.getUsername(), endpoint.getPassword()),
        bfoo);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK", null));

    assertArrayEquals(bbar, jedis.get(bfoo));
  }

  @Test
  public void migrateMulti() {
    assertNull(dest.get("foo1"));
    assertNull(dest.get("foo2"));
    assertNull(dest.get("foo3"));

    Pipeline p = jedis.pipelined();

    p.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    p.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3");

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK"));

    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar2", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));
  }

  @Test
  public void migrateMultiBinary() {
    assertNull(dest.get(bfoo1));
    assertNull(dest.get(bfoo2));
    assertNull(dest.get(bfoo3));

    Pipeline p = jedis.pipelined();

    p.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    p.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3);

    assertThat(p.syncAndReturnAll(),
        contains("OK", "OK"));

    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar2, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

  @Test
  public void migrateConflict() {
    dest.set("foo2", "bar");

    assertNull(dest.get("foo1"));
    assertEquals("bar", dest.get("foo2"));
    assertNull(dest.get("foo3"));

    Pipeline p = jedis.pipelined();

    p.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    p.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3");

    assertThat(p.syncAndReturnAll(),
        contains(
            equalTo("OK"),
            both(instanceOf(JedisDataException.class)).and(hasToString(containsString("BUSYKEY")))
        ));

    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));
  }

  @Test
  public void migrateConflictBinary() {
    dest.set(bfoo2, bbar);

    assertNull(dest.get(bfoo1));
    assertArrayEquals(bbar, dest.get(bfoo2));
    assertNull(dest.get(bfoo3));

    Pipeline p = jedis.pipelined();

    p.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    p.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3);

    assertThat(p.syncAndReturnAll(),
        contains(
            equalTo("OK"),
            both(instanceOf(JedisDataException.class)).and(hasToString(containsString("BUSYKEY")))
        ));

    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

}
