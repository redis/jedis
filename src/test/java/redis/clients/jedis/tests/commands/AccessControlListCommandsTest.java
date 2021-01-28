package redis.clients.jedis.tests.commands;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.AccessControlUser;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.tests.utils.RedisVersionUtil;
import redis.clients.jedis.util.SafeEncoder;

/**
 * TODO: properly define and test exceptions
 */
public class AccessControlListCommandsTest extends JedisCommandTestBase {

  public static String USER_YYY = "yyy";
  public static String USER_ZZZ = "zzz";
  public static String USER_ZZZ_PASSWORD = "secret";

  @BeforeClass
  public static void prepare() throws Exception {
    // Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis", RedisVersionUtil.checkRedisMajorVersionNumber(6));
  }

  @Test
  public void aclWhoAmI() {
    String string = jedis.aclWhoAmI();
    assertEquals("default", string);

    byte[] binary = jedis.aclWhoAmIBinary();
    assertArrayEquals(SafeEncoder.encode("default"), binary);
  }

  @Test
  public void aclListDefault() {
    assertTrue(jedis.aclList().size() > 0);
    assertTrue(jedis.aclListBinary().size() > 0);
  }

  @Test
  public void addAndRemoveUser() {
    int existingUsers = jedis.aclList().size();

    String status = jedis.aclSetUser(USER_ZZZ);
    assertEquals("OK", status);
    assertEquals(existingUsers + 1, jedis.aclList().size());
    assertEquals(existingUsers + 1, jedis.aclListBinary().size()); // test binary

    jedis.aclDelUser(USER_ZZZ);
    assertEquals(existingUsers, jedis.aclList().size());
    assertEquals(existingUsers, jedis.aclListBinary().size()); // test binary
  }

  @Test
  public void aclUsers() {
    List<String> users = jedis.aclUsers();
    assertEquals(2, users.size());
    assertTrue(users.contains("default"));

    assertEquals(2, jedis.aclUsersBinary().size()); // Test binary
  }

  @Test
  public void aclGetUser() {

    // get default user information
    AccessControlUser userInfo = jedis.aclGetUser("default");

    System.err.println("userInfo.getFlags(): " + userInfo.getFlags());
    
    assertEquals(4, userInfo.getFlags().size());
    assertEquals(1, userInfo.getPassword().size());
    assertEquals("+@all", userInfo.getCommands());
    assertEquals("*", userInfo.getKeys().get(0));

    // create new user
    jedis.aclDelUser(USER_ZZZ);
    jedis.aclSetUser(USER_ZZZ);
    userInfo = jedis.aclGetUser(USER_ZZZ);
    assertEquals(2, userInfo.getFlags().size());
    assertEquals("off", userInfo.getFlags().get(0));
    assertTrue(userInfo.getPassword().isEmpty());
    assertTrue(userInfo.getKeys().isEmpty());

    // reset user
    jedis.aclSetUser(USER_ZZZ, "reset", "+@all", "~*", "-@string", "+incr", "-debug",
      "+debug|digest");
    userInfo = jedis.aclGetUser(USER_ZZZ);
    assertThat(userInfo.getCommands(), containsString("+@all"));
    assertThat(userInfo.getCommands(), containsString("-@string"));
    assertThat(userInfo.getCommands(), containsString("+debug|digest"));

    jedis.aclDelUser(USER_ZZZ);

  }

  @Test
  public void createUserAndPasswords() {
    String status = jedis.aclSetUser(USER_ZZZ, ">" + USER_ZZZ_PASSWORD);
    assertEquals("OK", status);

    // create a new client to try to authenticate
    Jedis jedis2 = new Jedis();
    String authResult = null;

    // the user is just created without any permission the authentication should fail
    try {
      authResult = jedis2.auth(USER_ZZZ, USER_ZZZ_PASSWORD);
      fail("Should throw a WRONGPASS exception");
    } catch (JedisAccessControlException e) {
      assertNull(authResult);
      assertTrue(e.getMessage().startsWith("WRONGPASS "));
    }

    // now activate the user
    authResult = jedis.aclSetUser(USER_ZZZ, "on", "+acl");
    jedis2.auth(USER_ZZZ, USER_ZZZ_PASSWORD);
    String connectedUser = jedis2.aclWhoAmI();
    assertEquals(USER_ZZZ, connectedUser);

    // test invalid password
    jedis2.close();

    try {
      authResult = jedis2.auth(USER_ZZZ, "wrong-password");
      fail("Should throw a WRONGPASS exception");
    } catch (JedisAccessControlException e) {
      assertEquals("OK", authResult);
      assertTrue(e.getMessage().startsWith("WRONGPASS "));
    }

    // remove password, and try to authenticate
    status = jedis.aclSetUser(USER_ZZZ, "<" + USER_ZZZ_PASSWORD);
    try {
      authResult = jedis2.auth(USER_ZZZ, USER_ZZZ_PASSWORD);
      fail("Should throw a WRONGPASS exception");
    } catch (JedisAccessControlException e) {
      assertEquals("OK", authResult);
      assertTrue(e.getMessage().startsWith("WRONGPASS "));
    }

    jedis.aclDelUser(USER_ZZZ); // delete the user
    try {
      authResult = jedis2.auth(USER_ZZZ, "wrong-password");
      fail("Should throw a WRONGPASS exception");
    } catch (JedisAccessControlException e) {
      assertEquals("OK", authResult);
      assertTrue(e.getMessage().startsWith("WRONGPASS "));
    }

    jedis2.close();

  }

  @Test
  public void aclSetUserWithAnyPassword() {
    jedis.aclDelUser(USER_ZZZ);
    String status = jedis.aclSetUser(USER_ZZZ, "nopass");
    assertEquals("OK", status);
    status = jedis.aclSetUser(USER_ZZZ, "on", "+acl");
    assertEquals("OK", status);

    // connect with this new user and try to get/set keys
    Jedis jedis2 = new Jedis();
    String authResult = jedis2.auth(USER_ZZZ, "any password");
    assertEquals("OK", authResult);

    jedis2.close();
    jedis.aclDelUser(USER_ZZZ);

  }

  @Test
  public void aclExcudeSingleCommand() {
    jedis.aclDelUser(USER_ZZZ);
    String status = jedis.aclSetUser(USER_ZZZ, "nopass");
    assertEquals("OK", status);

    status = jedis.aclSetUser(USER_ZZZ, "on", "+acl");
    assertEquals("OK", status);

    status = jedis.aclSetUser(USER_ZZZ, "allcommands", "allkeys");
    assertEquals("OK", status);

    status = jedis.aclSetUser(USER_ZZZ, "-ping");
    assertEquals("OK", status);

    // connect with this new user and try to get/set keys
    Jedis jedis2 = new Jedis();
    String authResult = jedis2.auth(USER_ZZZ, "any password");
    assertEquals("OK", authResult);

    jedis2.incr("mycounter");

    String result = null;
    try {
      result = jedis2.ping();
      fail("Should throw a NOPERM exception");
    } catch (JedisAccessControlException e) {
      assertNull(result);
      assertEquals(
        "NOPERM this user has no permissions to run the 'ping' command or its subcommand",
        e.getMessage());
    }

    jedis2.close();
    jedis.aclDelUser(USER_ZZZ);

  }

  @Test
  public void aclDelUser() {
    String statusSetUser = jedis.aclSetUser(USER_YYY);
    assertEquals("OK", statusSetUser);
    int before = jedis.aclList().size();
    Long statusDelUser = jedis.aclDelUser(USER_YYY);
    assertEquals(1, statusDelUser.longValue());
    int after = jedis.aclList().size();
    assertEquals(before - 1, after);
  }

  @Test
  public void basicPermissionsTest() {

    // create a user with login permissions

    jedis.aclDelUser(USER_ZZZ); // delete the user

    // users are not able to access any command
    String status = jedis.aclSetUser(USER_ZZZ, ">" + USER_ZZZ_PASSWORD);
    String authResult = jedis.aclSetUser(USER_ZZZ, "on", "+acl");

    // connect with this new user and try to get/set keys
    Jedis jedis2 = new Jedis();
    jedis2.auth(USER_ZZZ, USER_ZZZ_PASSWORD);

    String result = null;
    try {
      result = jedis2.set("foo", "bar");
      fail("Should throw a NOPERM exception");
    } catch (JedisAccessControlException e) {
      assertNull(result);
      assertEquals(
        "NOPERM this user has no permissions to run the 'set' command or its subcommand",
        e.getMessage());
    }

    // change permissions of the user
    // by default users are not able to access any key
    status = jedis.aclSetUser(USER_ZZZ, "+set");

    jedis2.close();
    jedis2.auth(USER_ZZZ, USER_ZZZ_PASSWORD);

    result = null;
    try {
      result = jedis2.set("foo", "bar");
      fail("Should throw a NOPERM exception");
    } catch (JedisAccessControlException e) {
      assertNull(result);
      assertEquals(
        "NOPERM this user has no permissions to access one of the keys used as arguments",
        e.getMessage());
    }

    // allow user to access a subset of the key
    result = jedis.aclSetUser(USER_ZZZ, "allcommands", "~foo:*", "~bar:*"); // TODO : Define a DSL

    // create key foo, bar and zap
    result = jedis2.set("foo:1", "a");
    assertEquals("OK", status);

    result = jedis2.set("bar:2", "b");
    assertEquals("OK", status);

    result = null;
    try {
      result = jedis2.set("zap:3", "c");
      fail("Should throw a NOPERM exception");
    } catch (JedisAccessControlException e) {
      assertNull(result);
      assertEquals(
        "NOPERM this user has no permissions to access one of the keys used as arguments",
        e.getMessage());
    }

    // remove user
    jedis.aclDelUser(USER_ZZZ); // delete the user

  }

  @Test
  public void aclCatTest() {
    List<String> categories = jedis.aclCat();
    assertTrue( !categories.isEmpty() );

    // test binary
    List<byte[]> categoriesBinary = jedis.aclCatBinary();
    assertTrue( !categories.isEmpty() );
    assertEquals( categories.size() , categoriesBinary.size());

    // test commands in a category
    assertTrue(!jedis.aclCat("scripting").isEmpty());

    try {
      jedis.aclCat("testcategory");
      fail("Should throw a ERR exception");
    } catch (Exception e) {
      assertEquals("ERR Unknown category 'testcategory'", e.getMessage());
    }
  }

  @Test
  public void aclLogTest() {
    jedis.aclLog("RESET");
    assertTrue(jedis.aclLog().isEmpty());

    // create new user and cconnect
    jedis.aclSetUser("antirez", ">foo", "on", "+set", "~object:1234");
    jedis.aclSetUser("antirez", "+eval", "+multi", "+exec");
    jedis.auth("antirez", "foo");

    // generate an error (antirez user does not have the permission to access foo)
    try {
      jedis.get("foo");
      fail("Should have thrown an JedisAccessControlException: user does not have the permission to get(\"foo\")");
    } catch(JedisAccessControlException e) {}

    // test the ACL Log
    jedis.auth("default", "foobared");

    assertEquals("Number of log messages ", 1, jedis.aclLog().size());
    assertEquals(1, jedis.aclLog().get(0).getCount());
    assertEquals("antirez", jedis.aclLog().get(0).getUsername());
    assertEquals("toplevel", jedis.aclLog().get(0).getContext());
    assertEquals("command", jedis.aclLog().get(0).getReason());
    assertEquals("get", jedis.aclLog().get(0).getObject());

    // Capture similar event
    jedis.aclLog("RESET");
    assertTrue(jedis.aclLog().isEmpty());

    jedis.auth("antirez", "foo");

    for(int i = 0; i < 10 ; i++ ) {
      // generate an error (antirez user does not have the permission to access foo)
      try {
        jedis.get("foo");
        fail("Should have thrown an JedisAccessControlException: user does not have the permission to get(\"foo\")");
      } catch (JedisAccessControlException e) {}
    }

    // test the ACL Log
    jedis.auth("default", "foobared");
    assertEquals("Number of log messages ", 1, jedis.aclLog().size());
    assertEquals(10, jedis.aclLog().get(0).getCount());
    assertEquals("get", jedis.aclLog().get(0).getObject());

    // Generate another type of error
    jedis.auth("antirez", "foo");
    try {
      jedis.set("somekeynotallowed", "1234");
      fail("Should have thrown an JedisAccessControlException: user does not have the permission to set(\"somekeynotallowed\", \"1234\")");
    } catch (JedisAccessControlException e) {}

    // test the ACL Log
    jedis.auth("default", "foobared");
    assertEquals("Number of log messages ", 2, jedis.aclLog().size());
    assertEquals(1, jedis.aclLog().get(0).getCount());
    assertEquals("somekeynotallowed", jedis.aclLog().get(0).getObject());
    assertEquals("key", jedis.aclLog().get(0).getReason());

    jedis.aclLog("RESET");
    assertTrue(jedis.aclLog().isEmpty());

    jedis.auth("antirez", "foo");
    Transaction t = jedis.multi();
    t.incr("foo");
    try{
      t.exec();
      fail("Should have thrown an JedisAccessControlException: user does not have the permission to incr(\"foo\")");
    } catch (Exception e){}
    t.close();

    jedis.auth("default", "foobared");
    assertEquals("Number of log messages ", 1, jedis.aclLog().size());
    assertEquals(1, jedis.aclLog().get(0).getCount());
    assertEquals("multi", jedis.aclLog().get(0).getContext());
    assertEquals("incr", jedis.aclLog().get(0).getObject());

     // ACL LOG can accept a numerical argument to show less entries
    jedis.auth("antirez", "foo");
    for (int i = 0; i < 5; i++) {
      try{
        jedis.incr("foo");
        fail("Should have thrown an JedisAccessControlException: user does not have the permission to incr(\"foo\")");
      } catch (JedisAccessControlException e){}
    }
    try{
      jedis.set("foo-2", "bar");
      fail("Should have thrown an JedisAccessControlException: user does not have the permission to set(\"foo-2\", \"bar\")");
    } catch (JedisAccessControlException e){}

    jedis.auth("default", "foobared");
    assertEquals("Number of log messages ", 3, jedis.aclLog().size());
    assertEquals("Number of log messages ", 2, jedis.aclLog(2).size());

     // Binary tests
    assertEquals("Number of log messages ", 3, jedis.aclLogBinary().size());
    assertEquals("Number of log messages ", 2, jedis.aclLogBinary(2).size());
    byte[] status = jedis.aclLog("RESET".getBytes());
    assertNotNull(status);
    assertTrue(jedis.aclLog().isEmpty());

    jedis.aclDelUser("antirez");
  }

  @Test
  public void aclGenPass() {
    assertNotNull( jedis.aclGenPass() );
  }

  @Test
  public void aclGenPassBinary() {
    assertNotNull( jedis.aclGenPassBinary() );
  }

  @Test
  public void aclBinaryCommandsTest() {
    jedis.aclSetUser(USER_ZZZ.getBytes());
    assertNotNull(jedis.aclGetUser(USER_ZZZ));

    assertEquals(Long.valueOf(1L), jedis.aclDelUser(USER_ZZZ.getBytes()));

    jedis.aclSetUser(USER_ZZZ.getBytes(),
            "reset".getBytes(),
            "+@all".getBytes(),
            "~*".getBytes(),
            "-@string".getBytes(),
            "+incr".getBytes(),
            "-debug".getBytes(),
            "+debug|digest".getBytes());

    AccessControlUser userInfo = jedis.aclGetUser(USER_ZZZ.getBytes());

    assertThat(userInfo.getCommands(), containsString("+@all"));
    assertThat(userInfo.getCommands(), containsString("-@string"));
    assertThat(userInfo.getCommands(), containsString("+debug|digest"));

    jedis.aclDelUser(USER_ZZZ.getBytes());
  }

}
