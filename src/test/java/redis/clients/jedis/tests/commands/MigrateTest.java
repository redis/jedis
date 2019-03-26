package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.MigrateParams;

public class MigrateTest extends JedisCommandTestBase {

  private static final byte[] bfoo = {0x01, 0x02, 0x03};
  private static final byte[] bbar = {0x04, 0x05, 0x06};
  private static final byte[] bfoo1 = {0x07, 0x08, 0x01};
  private static final byte[] bbar1 = {0x09, 0x00, 0x01};
  private static final byte[] bfoo2 = {0x07, 0x08, 0x02};
  private static final byte[] bbar2 = {0x09, 0x00, 0x02};
  private static final byte[] bfoo3 = {0x07, 0x08, 0x03};
  private static final byte[] bbar3 = {0x09, 0x00, 0x03};

  private Jedis dest;
  private String host;
  private int port;
  private final int destDB = 2;
  private final int timeout = Protocol.DEFAULT_TIMEOUT;
  private String pass;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    host = jedis.getClient().getHost();
    port = 6380;

    dest = new Jedis(host, port, 500);
    dest.auth("foobared");
    dest.flushAll();

    dest.select(destDB);
    pass = dest.configGet("requirepass").get(1);
    dest.configSet("requirepass", "");
  }

  @After
  @Override
  public void tearDown() {
    dest.configSet("requirepass", pass);
    dest.close();
    super.tearDown();
  }

  @Test
  public void nokey() {
    assertEquals("NOKEY", jedis.migrate(host, port, "foo", destDB, timeout));
    assertEquals("NOKEY", jedis.migrate(host, port, bfoo, destDB, timeout));
    assertEquals("NOKEY", jedis.migrate(host, port, destDB, timeout, new MigrateParams(), "foo1", "foo2", "foo3"));
    assertEquals("NOKEY", jedis.migrate(host, port, destDB, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3));
  }

  @Test
  public void migrate() {
    jedis.set("foo", "bar");
    assertEquals("OK", jedis.migrate(host, port, "foo", destDB, timeout));
    assertEquals("bar", dest.get("foo"));
    assertNull(jedis.get("foo"));

    jedis.set(bfoo, bbar);
    assertEquals("OK", jedis.migrate(host, port, bfoo, destDB, timeout));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertNull(jedis.get(bfoo));
  }

  @Test
  public void migrateMulti() {
    jedis.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    assertEquals("OK", jedis.migrate(host, port, destDB, timeout, new MigrateParams(), "foo1", "foo2", "foo3"));
    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar2", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));

    jedis.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    assertEquals("OK", jedis.migrate(host, port, destDB, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3));
    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar2, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

  @Test
  public void migrateConflict() {
    jedis.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    dest.set("foo2", "bar");
    try {
      jedis.migrate(host, port, destDB, timeout, new MigrateParams(), "foo1", "foo2", "foo3");
      fail("Should get BUSYKEY error");
    } catch(JedisDataException jde) {
      assertTrue(jde.getMessage().contains("BUSYKEY"));
    }
    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));

    jedis.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    dest.set(bfoo2, bbar);
    try {
      jedis.migrate(host, port, destDB, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3);
      fail("Should get BUSYKEY error");
    } catch(JedisDataException jde) {
      assertTrue(jde.getMessage().contains("BUSYKEY"));
    }
    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

  @Test
  public void migrateCopyReplace() {
    jedis.set("foo", "bar1");
    dest.set("foo", "bar2");
    assertEquals("OK", jedis.migrate(host, port, destDB, timeout, new MigrateParams().copy().replace(), "foo"));
    assertEquals("bar1", jedis.get("foo"));
    assertEquals("bar1", dest.get("foo"));

    jedis.set(bfoo, bbar1);
    dest.set(bfoo, bbar2);
    assertEquals("OK", jedis.migrate(host, port, destDB, timeout, new MigrateParams().copy().replace(), bfoo));
    assertArrayEquals(bbar1, jedis.get(bfoo));
    assertArrayEquals(bbar1, dest.get(bfoo));
  }

}
