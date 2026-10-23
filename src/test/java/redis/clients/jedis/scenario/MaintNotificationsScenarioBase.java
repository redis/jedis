package redis.clients.jedis.scenario;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redis.test.fi.Database;
import com.redis.test.fi.Scenario;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.AfterEach;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.util.TlsUtil;

/**
 * Scenario tests that run one standalone {@link RedisClient} against the deployed database. A
 * subclass deploys the database and opens the client through {@link #openClient}; the client config
 * starts from the database's credentials (plus pinned TLS trust for a TLS database) and the pool
 * from its defaults, and {@link #customiseClientConfig} / {@link #customiseClientPoolConfig} add a
 * subclass's settings. The client is owned here and closed on teardown before the database is
 * deleted.
 */
abstract class MaintNotificationsScenarioBase extends FaultInjectorScenarioBase {

  private static final Logger logger = LoggerFactory
      .getLogger(MaintNotificationsScenarioBase.class);

  static final String TRUSTSTORE_PASSWORD = "changeit";

  RedisClient client;
  Path serverTruststore;

  /** Opens the client under test and takes ownership of it. */
  RedisClient openClient(Database database, MaintenanceNotificationsConfig maintenance) {
    DefaultJedisClientConfig.Builder config = database.clientConfig();
    if (database.tls()) {
      serverTruststore = createServerTruststore(database.endpoint());
      config.sslOptions(sslOptionsFor(serverTruststore));
    }
    customiseClientConfig(config);
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    customiseClientPoolConfig(poolConfig);
    client = RedisClient.builder().hostAndPort(database.host(), database.port())
        .clientConfig(config.build()).poolConfig(poolConfig).maintenanceNotifications(maintenance)
        .build();
    return client;
  }

  /**
   * Adds client settings on top of the credentials and TLS trust. The default adds nothing: the
   * client is what an application gets without tuning.
   */
  void customiseClientConfig(DefaultJedisClientConfig.Builder config) {
  }

  /** Adds pool settings on top of the defaults. */
  void customiseClientPoolConfig(GenericObjectPoolConfig<Connection> poolConfig) {
  }

  /**
   * Bootstraps client trust for an endpoint whose CA is not available to the test: downloads the
   * certificate chain the server presents and saves a truststore containing only that chain, so the
   * client trusts exactly this server and nothing else. The store is a temp file under the test
   * work folder, deleted when the test completes.
   */
  private static Path createServerTruststore(URI endpoint) {
    try {
      X509Certificate[] chain = TlsUtil.captureServerChain(endpoint.getHost(), endpoint.getPort());
      logger.info("Pinning server chain of {}: subject={}, SANs={}", endpoint,
        chain[0].getSubjectX500Principal(), chain[0].getSubjectAlternativeNames());
      return TlsUtil.createAndSaveTruststore(chain, TlsUtil.tempTruststorePath("sch-pinned"),
        TRUSTSTORE_PASSWORD);
    } catch (Exception e) {
      throw new IllegalStateException("TLS trust bootstrap failed for " + endpoint, e);
    }
  }

  /**
   * All {@link SslOptions} defaults except the truststore are kept, so the client under test
   * performs full certificate-chain and hostname verification.
   */
  private static SslOptions sslOptionsFor(Path truststore) {
    return SslOptions.builder().truststore(truststore.toFile(), TRUSTSTORE_PASSWORD.toCharArray())
        .build();
  }

  private void deleteServerTruststore() {
    if (serverTruststore == null) {
      return;
    }
    try {
      Files.deleteIfExists(serverTruststore);
    } catch (IOException e) {
      logger.warn("Failed to delete truststore {}", serverTruststore, e);
    }
    serverTruststore = null;
  }

  @AfterEach
  final void tearDownClient() {
    try {
      client = closeIfOpen(client);
    } finally {
      deleteServerTruststore();
    }
  }

  /** Closes the client if one is open; returns null so the field is cleared in one step. */
  static RedisClient closeIfOpen(RedisClient client) {
    if (client != null) {
      client.close();
    }
    return null;
  }
}
