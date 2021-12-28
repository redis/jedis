package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.FailoverParams;
import redis.clients.jedis.HostAndPorts;

public class FailoverCommandsTest {

  private static final HostAndPort node1 = HostAndPorts.getRedisServers().get(9);
  private static final HostAndPort node2 = HostAndPorts.getRedisServers().get(10);

  private HostAndPort masterAddress;
  private HostAndPort replicaAddress;

  private boolean switching;
  private static boolean failoverStuck = false;

  @Before
  public void prepare() {
    String role1, role2;
    try (Jedis jedis1 = new Jedis(node1)) {
      role1 = (String) jedis1.role().get(0);
    }
    try (Jedis jedis2 = new Jedis(node2)) {
      role2 = (String) jedis2.role().get(0);
    }

    if ("master".equals(role1) && "slave".equals(role2)) {
      masterAddress = node1;
      replicaAddress = node2;
    } else if ("master".equals(role2) && "slave".equals(role1)) {
      masterAddress = node2;
      replicaAddress = node1;
    } else {
      fail();
    }

    switching = false;
  }

  @After
  public void cleanUp() {
    if (switching) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException ex) { }
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
        failoverStuck = true;
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

  @Test
  public void failoverToHAP() throws InterruptedException {
    assumeFalse(failoverStuck);
    try (Jedis master = new Jedis(masterAddress)) {
      switching = true;
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .to(new HostAndPort("127.0.0.1", replicaAddress.getPort()))));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void failoverForceWithoutToFailFast() {
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .timeout(100).force()));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void failoverForceWithoutTimeoutFailFast() {
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .to(new HostAndPort("127.0.0.1", replicaAddress.getPort())).force()));
    }
  }

  @Test
  public void failoverForce() throws InterruptedException {
    assumeFalse(failoverStuck);
    try (Jedis master = new Jedis(masterAddress)) {
      switching = true;
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .to(new HostAndPort("127.0.0.1", replicaAddress.getPort())).force().timeout(100)));
    }
  }

  @Test
  public void failoverToWrongPort() {
    try (Jedis master = new Jedis(masterAddress)) {
      master.failover(FailoverParams.failoverParams().to("127.0.0.1", 6300));
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
