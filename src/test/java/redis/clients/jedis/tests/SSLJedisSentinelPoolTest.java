package redis.clients.jedis.tests;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisSentinelPool;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class SSLJedisSentinelPoolTest {
  private static final String MASTER_NAME = "mymaster";

  protected Set<String> sentinels = new HashSet<String>();

  @Before
  public void setUp() throws Exception {
    HostAndPort sentinel2 = HostAndPortUtil.getSentinelServers().get(1);
    HostAndPort sentinel4 = HostAndPortUtil.getSentinelServers().get(3);
    sentinels.add(sentinel2.getHost() + ":" + (sentinel2.getPort() + 10000));
    sentinels.add(sentinel4.getHost() + ":" + (sentinel4.getPort() + 10000));
  }

  @BeforeClass
  public static void setupTrustStore() {
    setJvmTrustStore("src/test/resources/truststore.jceks", "jceks");
  }

  private static void setJvmTrustStore(String trustStoreFilePath, String trustStoreType) {
    assertTrue(String.format("Could not find trust store at '%s'.", trustStoreFilePath), new File(
        trustStoreFilePath).exists());
    System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
    System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithoutSsl() {
    boolean redisSsl = false;
    boolean sentinelSsl = true;
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig, 1000, 1000,
        "foobared", 0, "clientName", 1000, 1000, null, "sentinelClientName", redisSsl, sentinelSsl, null, null, null);
    pool.getResource().close();
    pool.destroy();
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithSsl() {
    boolean redisSsl = true;
    boolean sentinelSsl = true;
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

    class PortSwizzlingJedisSentinelPool extends JedisSentinelPool {
      public PortSwizzlingJedisSentinelPool(String masterName, Set<String> sentinels,
          GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password, int database,
          String clientName, int sentinelConnectionTimeout, int sentinelSoTimeout, String sentinelPassword,
          String sentinelClientName, boolean isRedisSslEnabled, boolean isSentinelSslEnabled,
          SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
        super(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, password, database, clientName,
            sentinelConnectionTimeout, sentinelSoTimeout, sentinelPassword, sentinelClientName, isRedisSslEnabled,
            isSentinelSslEnabled, sslSocketFactory, sslParameters, hostnameVerifier);
      }

      @Override
      protected HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
        // Sentinel broadcasts the non-ssl port number of redis, this swizzles it to the ssl port.
        HashMap<Integer, Integer> portMapping = new HashMap<Integer, Integer>();
        portMapping.put(6381, 16381);
        portMapping.put(6382, 16382);
        HostAndPort original = super.toHostAndPort(getMasterAddrByNameResult);
        int swizzled = portMapping.get(original.getPort());
        return new HostAndPort(original.getHost(), swizzled);
      }
    }
    JedisSentinelPool pool = new PortSwizzlingJedisSentinelPool(MASTER_NAME, sentinels, poolConfig, 1000, 1000,
            "foobared", 0, "clientName", 1000, 1000, null, "sentinelClientName", redisSsl, sentinelSsl, null, null, null);
    pool.getResource().close();
    pool.destroy();
  }

}
