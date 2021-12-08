package redis.clients.jedis.commands.jedis;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.FailoverParams;
import redis.clients.jedis.HostAndPorts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FailoverCommandsTest {

  private static HostAndPort node1;
  private static HostAndPort node2;

  private HostAndPort masterAddress;
  private HostAndPort replicaAddress;

  private boolean switched;

  @BeforeClass
  public static void setUp() {
    node1 = HostAndPorts.getRedisServers().get(9);
    node2 = HostAndPorts.getRedisServers().get(10);
  }

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

    switched = false;
  }

  @After
  public void cleanUp() {
    if (switched) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException ex) { }
    }
  }

  @Test
  public void failoverMaster() throws InterruptedException {
    //
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover());
      Thread.sleep(120); // allow some time to failover;
      // not too much as everything is happening in same machine
      assertEquals("slave", master.role().get(0));
    }
  }

  @Test
  public void failoverReplica() {
    try (Jedis replica = new Jedis(replicaAddress)) {
      replica.failover();
    } catch(JedisDataException ex) {
      assertEquals("ERR FAILOVER is not valid when server is a replica.", ex.getMessage());
    }
  }

  @Test
  public void failoverToHAP() throws InterruptedException {
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .to(new HostAndPort("127.0.0.1", replicaAddress.getPort()))));
      switched = true;
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
    try (Jedis master = new Jedis(masterAddress)) {
      assertEquals("OK", master.failover(FailoverParams.failoverParams()
          .to(new HostAndPort("127.0.0.1", replicaAddress.getPort())).force().timeout(100)));
      switched = true;
    }
  }

  @Test
  public void failoverToWrongPort() {
    try (Jedis master = new Jedis(masterAddress)) {
      master.failover(FailoverParams.failoverParams().to("127.0.0.1", 6300));
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
