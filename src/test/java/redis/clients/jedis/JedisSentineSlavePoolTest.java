package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Created by PENG on 2024/4/3 17:34
 *
 * @author admin
 */
public class JedisSentineSlavePoolTest {
  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel0 = HostAndPorts.getSentinelServers().get(0);
  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(2);

  protected final Set<String> sentinels = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel0.toString());
    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());
  }

  @Test
  public void repeatedSentinelPoolInitialization() {

    for (int i = 0; i < 20; ++i) {
      GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
      JedisSentinelSlavePool slavePool = new JedisSentinelSlavePool(MASTER_NAME, sentinels, config,
              1000, 1000, "hellojedis", 0, null);

      slavePool.getResource().close();
      slavePool.destroy();
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelSlavePool pool = new JedisSentinelSlavePool(MASTER_NAME, sentinels, config, 1000,
            1000, "hellojedis", 0, null);

    Jedis jedis = pool.getResource();
    try {
//      redis.clients.jedis.exceptions.JedisDataException: READONLY You can't write against a read only replica.
//      jedis.set("hello", "jedis");
    } finally {
      jedis.close();
    }

    Jedis jedis2 = pool.getResource();
    try {
      assertEquals(jedis, jedis2);
    } finally {
      jedis2.close();
    }
  }

  @Test
  public void checkReadFromSlave() {

    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    JedisSentinelSlavePool slavePool = new JedisSentinelSlavePool(MASTER_NAME, sentinels, config, 1000,
            1000, "hellojedis", 0, null);
    for (int i = 0; i < 10; i++) {
      Jedis jedis = slavePool.getResource();
      jedis.get("key"); //Random read from slave
    }
    slavePool.getResource().close();
    slavePool.destroy();
  }
}
