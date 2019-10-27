package redis.clients.jedis.tests.commands;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.HostAndPortUtil;

public abstract class JedisCommandTestBase {
  protected static final HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  protected Jedis jedis;

  public JedisCommandTestBase() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    resetConfigs();
    jedis.disconnect();
  }

  protected Jedis createJedis() {
    Jedis j = new Jedis(hnp);
    j.connect();
    j.auth("foobared");
    j.flushAll();
    return j;
  }

  private Map<String, String> configMap = null;

  protected void backupConfigs(String... configs) {
    configMap = new LinkedHashMap<>(configs.length);
    for (String config : configs) {
      configMap.put(config, jedis.configGet(config).get(1));
    }
  }

  private void resetConfigs() {
    if (configMap == null) return;
    for (Map.Entry<String, String> entry : configMap.entrySet()) {
      String config = entry.getKey();
      String value = entry.getValue();
      jedis.configSet(config, value);
    }
  }
}
