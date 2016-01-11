package redis.clients.jedis.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.net.URI;

public class SSLJedisTest extends JedisCommandTestBase {

  @BeforeClass
  public static void setupTrustStore() {
    String trustStoreFilePath = "src/test/resources/truststore.jks";
    Assert.assertTrue(String.format("Could not find trust store at '%s'.", trustStoreFilePath),
            new File(trustStoreFilePath).exists());
    System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
  }

  @Test
  public void connectWithShardInfo() {
    final URI uri = URI.create("rediss://localhost:6390");
    final SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
        .getDefault();

    JedisShardInfo shardInfo = new JedisShardInfo(uri, sslSocketFactory, null, null);
    shardInfo.setPassword("foobared");

    Jedis jedis = new Jedis(shardInfo);
    jedis.get("foo");
    jedis.close();
  }

  @Test
  public void connectWithoutShardInfo() {
    Jedis jedis = new Jedis(URI.create("rediss://localhost:6390"));
    jedis.auth("foobared");
    jedis.get("foo");
    jedis.close();
  }
}