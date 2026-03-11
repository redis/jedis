package redis.clients.jedis.tls;

import org.junit.jupiter.api.BeforeAll;

import redis.clients.jedis.*;

/**
 * Integration tests for mTLS (mutual TLS) certificate-based authentication with standalone Redis.
 * <p>
 * Extends {@link ClientAuthIT} to provide standalone-specific client creation and command
 * execution.
 */
public class ClientAuthRedisClientIT extends ClientAuthIT {

  @BeforeAll
  public static void setUpStandaloneMtlsStores() {
    endpoint = Endpoints.getRedisEndpoint("standalone-mtls");
    setUpMtlsStoresForEndpoint(endpoint, ClientAuthRedisClientIT.class.getSimpleName());
  }

  @Override
  protected UnifiedJedis createClient(SslOptions sslOptions) {
    return RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build()).build();
  }

  @Override
  protected String executeAclWhoAmI(UnifiedJedis client) {
    RedisClient redisClient = (RedisClient) client;
    return redisClient.executeCommand(new CommandObject<>(
        new CommandArguments(Protocol.Command.ACL).add("WHOAMI"), BuilderFactory.STRING));
  }
}
