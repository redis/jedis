package redis.clients.jedis.modules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.util.RedisVersionUtil;

@RunWith(Parameterized.class)
public class RedisModulesAccessControlListCommandsTest extends JedisCommandsTestBase {

  public static final String USER_NAME = "newuser";
  public static final String USER_PASSWORD = "secret";

  @BeforeClass
  public static void prepare() throws Exception {
    // Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
        RedisVersionUtil.checkRedisMajorVersionNumber(8, endpoint));
  }

  public RedisModulesAccessControlListCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    try {
      jedis.aclDelUser(USER_NAME);
    } catch (Exception e) { }
    super.tearDown();
  }

  @Test
  public void aclHashesCommandsTest() {
    // create and enable an user with permission to all keys but no commands
    jedis.aclSetUser(USER_NAME, ">" + USER_PASSWORD, "on", "~*");

    // client object with new user
    try (UnifiedJedis client = new UnifiedJedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().user(USER_NAME).password(USER_PASSWORD).build())) {

      // user can't execute hash commands
      JedisAccessControlException ace = assertThrows("Should throw a NOPERM exception",
          JedisAccessControlException.class,
          () -> client.hgetAll("foo"));
      assertThat(ace.getMessage(), startsWith("NOPERM "));
      assertThat(ace.getMessage(), endsWith(" has no permissions to run the 'hgetall' command"));

      // permit user to hash commands
      jedis.aclSetUser(USER_NAME, "+@hash");

      // user can now execute hash commands
      client.hgetAll("foo");
    }
  }

}
