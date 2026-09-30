package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ClientSetInfoConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisSentinelClient;
import redis.clients.jedis.builders.SentinelClientBuilder;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * What the Redis Enterprise discovery service (Sentinel-compatible API) offers a Jedis client, and
 * the two configuration mistakes that silently break it.
 * <p>
 * This class is expected to stay green, and it deliberately neither mutates the cluster nor needs
 * the fault injector - only the endpoints configuration and reachable port 8001. When
 * {@link #implementsOnlyASubsetOfTheSentinelProtocol()} goes red, that means the server grew a
 * command it did not implement before and the client-side handling should be revisited.
 * <p>
 * The companion {@link EnterpriseSentinelEndpointMoveIT} drives an actual endpoint move.
 */
@Tags({ @Tag("scenario") })
@DisplayName("Redis Enterprise Sentinel discovery service, as seen by Jedis")
public class EnterpriseSentinelDiscoveryIT {

  private static final Logger log = LoggerFactory.getLogger(EnterpriseSentinelDiscoveryIT.class);

  private static EndpointConfig endpoint;

  private List<HostAndPort> sentinels;

  private HostAndPort sentinel;

  private String masterName;

  @BeforeAll
  public static void setupEndpoint() {
    // Endpoints#getRedisEndpoint throws TestAbortedException when the key is absent, which skips.
    endpoint = Endpoints.getRedisEndpoint(EnterpriseSentinelSupport.DB_ENDPOINT_NAME);
  }

  @BeforeEach
  public void resolveDiscoveryService() {
    sentinels = EnterpriseSentinelSupport.sentinelHostsAndPorts(endpoint);
    assumeTrue(!sentinels.isEmpty(), EnterpriseSentinelSupport.missingDiscoveryEndpoints());

    sentinel = EnterpriseSentinelSupport.firstReachableSentinel(sentinels);
    assumeTrue(sentinel != null, EnterpriseSentinelSupport.unreachableDiscoveryService(sentinels));

    // The Sentinel master name is the Redis Enterprise database name, which is also the endpoints
    // configuration key in every environment seen so far. Asserted rather than assumed so a
    // divergence reports here instead of as "seems to be not monitored" later.
    masterName = EnterpriseSentinelSupport.DB_ENDPOINT_NAME;
  }

  @Test
  @DisplayName("announces the database under both its external and internal master name")
  public void announcesTheDatabaseUnderBothMasterNames() {
    try (Jedis jedis = new Jedis(sentinel, EnterpriseSentinelSupport.sentinelClientConfig())) {

      assertEquals("PONG", jedis.ping());

      Map<String, String> master = jedis.sentinelMaster(masterName);
      log.info("SENTINEL MASTER {} -> {}", masterName, master);

      assertEquals(masterName, master.get("name"));
      assertEquals("master", master.get("flags"));
      assertEquals("0", master.get("num-other-sentinels"));
      assertEquals(String.valueOf(endpoint.getPort()), master.get("port"));
      assertNotNull(master.get("ip"));

      // Every database is announced twice: under its plain name (external address) and under
      // <db>@internal. Filter by the database port, which is unique per database in the cluster.
      List<String> names = new ArrayList<>();
      for (Map<String, String> entry : jedis.sentinelMasters()) {
        if (String.valueOf(endpoint.getPort()).equals(entry.get("port"))) {
          names.add(entry.get("name"));
        }
      }
      log.info("SENTINEL MASTERS entries for port {} -> {}", endpoint.getPort(), names);
      assertEquals(new HashSet<>(java.util.Arrays.asList(masterName,
        masterName + EnterpriseSentinelSupport.INTERNAL_SUFFIX)), new HashSet<>(names));
      assertEquals(2, names.size());

      List<String> address = jedis.sentinelGetMasterAddrByName(masterName);
      assertEquals(master.get("ip"), address.get(0));
      assertEquals(master.get("port"), address.get(1));
    }
  }

  @Test
  @DisplayName("every cluster node answers for the database, so any node is a usable sentinel")
  public void everyClusterNodeAnswersForTheDatabase() {
    HostAndPort expected = EnterpriseSentinelSupport.reportedMaster(sentinel, masterName);

    for (HostAndPort candidate : sentinels) {
      HostAndPort reported = EnterpriseSentinelSupport.reportedMaster(candidate, masterName);
      log.info("{} reports master {} at {}", candidate, masterName, reported);
      assertEquals(expected, reported);
    }
  }

  @Test
  @DisplayName("implements SUBSCRIBE and SENTINEL SLAVES, but not PSUBSCRIBE nor SENTINEL REPLICAS")
  public void implementsOnlyASubsetOfTheSentinelProtocol() {
    try (Jedis jedis = new Jedis(sentinel, EnterpriseSentinelSupport.sentinelClientConfig())) {

      // The pre-5.0 spelling is implemented and returns an empty list: a Redis Enterprise endpoint
      // is served by a proxy, so there are no client-visible replicas. Jedis never calls either of
      // these on its discovery path, so only applications asking directly are affected.
      assertTrue(jedis.sentinelSlaves(masterName).isEmpty());
      assertThrows(JedisDataException.class, () -> jedis.sentinelReplicas(masterName));

      assertThrows(JedisDataException.class, () -> jedis.sentinelMyId());
      assertThrows(JedisDataException.class, () -> jedis.sentinelFailover(masterName));
      assertThrows(JedisDataException.class, () -> jedis.sentinelReset(masterName));

      // Nothing on Jedis's sentinel path uses PSUBSCRIBE - this is what makes Jedis work against
      // Redis Enterprise where a pattern-subscribing client cannot.
      assertThrows(JedisDataException.class,
        () -> jedis.psubscribe(new redis.clients.jedis.JedisPubSub() {
        }, "*"));
    }

    // SUBSCRIBE of the three channels Redis Enterprise publishes does work.
    try (
        EnterpriseSentinelSupport.SwitchMasterCapture capture = new EnterpriseSentinelSupport.SwitchMasterCapture(
            sentinel)) {
      assertTrue(capture.isAlive());
    }
  }

  @Test
  @DisplayName("credentials on the sentinel config break discovery, because there is no AUTH")
  public void sentinelCredentialsBreakDiscovery() {
    assumeTrue(endpoint.getPassword() != null,
      "Skipping: the database has no password, so there is nothing to misplace");

    // The discovery service implements no AUTH. With no HELLO on the link (Jedis's default for
    // sentinel connections) credentials become a standalone AUTH, every sentinel is marked
    // unavailable, and the real cause is swallowed into a generic "All sentinels down".
    SentinelClientBuilder<RedisSentinelClient> builder = RedisSentinelClient.builder()
        .masterName(masterName).sentinels(new HashSet<>(sentinels))
        .sentinelClientConfig(DefaultJedisClientConfig.builder().serverDefaultProtocol()
            .user(endpoint.getUsername()).password(endpoint.getPassword()).build())
        .clientConfig(endpoint.getClientConfigBuilder().build());

    JedisConnectionException e = assertThrows(JedisConnectionException.class, builder::build);
    log.info("Credentials on the sentinel config fail with: {}", e.getMessage());
    assertTrue(e.getMessage().contains("All sentinels down"));
  }

  @ParameterizedTest(name = "clientSetInfoDisabled={0}")
  @ValueSource(booleans = { false, true })
  @DisplayName("discovers and serves the database with and without CLIENT SETINFO")
  public void discoversTheDatabaseWithAndWithoutClientSetInfo(boolean clientSetInfoDisabled) {
    DefaultJedisClientConfig.Builder sentinelConfig = DefaultJedisClientConfig.builder()
        .serverDefaultProtocol();
    if (clientSetInfoDisabled) {
      sentinelConfig.clientSetInfoConfig(ClientSetInfoConfig.DISABLED);
    }

    // Jedis sends CLIENT SETINFO LIB-NAME/LIB-VER on the sentinel link unless disabled, and the
    // discovery service implements no CLIENT. That is tolerated only because Connection#getMany
    // swallows a per-reply error, so assert both configurations actually work.
    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(masterName)
        .sentinels(new HashSet<>(sentinels)).sentinelClientConfig(sentinelConfig.build())
        .clientConfig(endpoint.getClientConfigBuilder().build()).build()) {

      assertEquals(EnterpriseSentinelSupport.reportedMaster(sentinel, masterName),
        client.getCurrentMaster());
      assertEquals(1, client.getPrimaryNodesConnectionMap().size());
      assertTrue(client.getPrimaryNodesConnectionMap().containsKey(client.getCurrentMaster()));

      String key = "jedis-re-sentinel-" + System.currentTimeMillis();
      client.set(key, "discovered");
      assertEquals("discovered", client.get(key));
      assertFalse(client.getCurrentMaster().getHost().isEmpty());
      client.del(key);
    }
  }
}
