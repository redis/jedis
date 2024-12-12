package redis.clients.jedis.authentication;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.entraid.EntraIDTokenAuthConfigBuilder;
import redis.clients.authentication.entraid.ManagedIdentityInfo.UserManagedIdentityType;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisPooled;

public class RedisEntraIDManagedIdentityIntegrationTests {
  private static final Logger log = LoggerFactory.getLogger(RedisEntraIDIntegrationTests.class);

  private static EntraIDTestContext testCtx;
  private static EndpointConfig endpointConfig;
  private static HostAndPort hnp;
  private static Set<String> managedIdentityAudience = Collections
      .singleton("https://redis.azure.com");

  @BeforeClass
  public static void before() {
    try {
      testCtx = EntraIDTestContext.DEFAULT;
      endpointConfig = HostAndPorts.getRedisEndpoint("standalone-entraid-acl");
      hnp = endpointConfig.getHostAndPort();
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      org.junit.Assume.assumeTrue(false);
    }
  }

  // T.1.1
  // Verify authentication using Azure AD with managed identities
  @Test
  public void withUserAssignedId_azureManagedIdentityIntegrationTest() {
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .userAssignedManagedIdentity(UserManagedIdentityType.OBJECT_ID,
          testCtx.getUserAssignedManagedIdentity())
        .scopes(managedIdentityAudience).build();

    DefaultJedisClientConfig jedisConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (JedisPooled jedis = new JedisPooled(hnp, jedisConfig)) {
      String key = UUID.randomUUID().toString();
      jedis.set(key, "value");
      assertEquals("value", jedis.get(key));
      jedis.del(key);
    }
  }

  // T.1.1
  // Verify authentication using Azure AD with managed identities
  @Test
  public void withSystemAssignedId_azureManagedIdentityIntegrationTest() {
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .systemAssignedManagedIdentity().scopes(managedIdentityAudience).build();

    DefaultJedisClientConfig jedisConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (JedisPooled jedis = new JedisPooled(hnp, jedisConfig)) {
      String key = UUID.randomUUID().toString();
      jedis.set(key, "value");
      assertEquals("value", jedis.get(key));
      jedis.del(key);
    }
  }
}
