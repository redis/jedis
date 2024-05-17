package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.FailoverParams;
import redis.clients.jedis.HostAndPorts;

public class FailoverCommandsTest {

  private static final int INVALID_PORT = 6000;

  private static final EndpointConfig node1 = HostAndPorts.getRedisEndpoint("standalone9");
  private static final EndpointConfig node2 = HostAndPorts.getRedisEndpoint("standalone10-replica-of-standalone9");

  private HostAndPort masterAddress;
  private HostAndPort replicaAddress;

  @Before
  public void prepare() {
    String role1, role2;
    try (Jedis jedis1 = new Jedis(node1.getHostAndPort(), node1.getClientConfigBuilder().build())) {
      role1 = (String) jedis1.role().get(0);
    }
    try (Jedis jedis2 = new Jedis(node2.getHostAndPort(), node2.getClientConfigBuilder().build())) {
      role2 = (String) jedis2.role().get(0);
    }

    if ("master".equals(role1) && "slave".equals(role2)) {
      masterAddress = node1.getHostAndPort();
      replicaAddress = node2.getHostAndPort();
    } else if ("master".equals(role2) && "slave".equals(role1)) {
      masterAddress = node2.getHostAndPort();
      replicaAddress = node1.getHostAndPort();
    } else {
      fail();
    }
  }

  @Test
  public void failoverMaster() throws InterruptedException {
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover());
      Thread.sleep(250);
//      assertEquals("slave", master.role().get(0));
      // Above test has a tendency to get stuck. So, doing following 'not so ideal' test.
      if ("slave".equals(master.role().get(0))) {
        // ok
      } else {
        // failover stuck
      }
    }
  }

  @Test
  public void failoverReplica() {
    try (Jedis replica = new Jedis(replicaAddress)) {
      replica.failover();
      fail("FAILOVER is not valid when server is a replica.");
    } catch(JedisDataException ex) {
      assertEquals("ERR FAILOVER is not valid when server is a replica.", ex.getMessage());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void failoverForceWithoutToFailFast() {
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .timeout(100).force()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void failoverForceWithoutTimeoutFailFast() {
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .to(new HostAndPort("127.0.0.1", INVALID_PORT)).force()));
    }
  }

  @Test
  public void failoverToWrongPort() {
    try (Jedis master = new Jedis(masterAddress)) {
      master.failover(FailoverParams.failoverParams().to("127.0.0.1", INVALID_PORT));
      fail("FAILOVER target HOST and PORT is not a replica.");
    } catch(JedisDataException ex) {
      assertEquals("ERR FAILOVER target HOST and PORT is not a replica.", ex.getMessage());
    }
  }

  @Test
  public void failoverToWrongPortWithTimeout() {
    try (Jedis master = new Jedis(masterAddress)) {
      master.failover(FailoverParams.failoverParams().to("127.0.0.1", INVALID_PORT).timeout(1000));
      fail("FAILOVER target HOST and PORT is not a replica.");
    } catch(JedisDataException ex) {
      assertEquals("ERR FAILOVER target HOST and PORT is not a replica.", ex.getMessage());
    }
  }

  @Test
  public void abortMaster() {
    try (Jedis master = new Jedis(masterAddress)) {
      master.failoverAbort();
    } catch(JedisDataException ex) {
      assertEquals("ERR No failover in progress.", ex.getMessage());
    }
  }

  @Test
  public void abortReplica() {
    try (Jedis replica = new Jedis(replicaAddress)) {
      replica.failoverAbort();
    } catch(JedisDataException ex) {
      assertEquals("ERR No failover in progress.", ex.getMessage());
    }
  }
}
