package redis.clients.jedis.scenario;

import java.io.Closeable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * Shared plumbing for the Redis Enterprise discovery-service scenario tests.
 * <p>
 * Redis Enterprise runs a Sentinel-protocol emulator on port 8001 of every cluster node. It
 * announces each database under two master names - {@code <db>} resolving to the endpoint's
 * external address and {@code <db>@internal} resolving to the internal one - and publishes
 * {@code +switch-master} whenever the database's endpoint (DMC proxy) moves to another node.
 */
final class EnterpriseSentinelSupport {

  private static final Logger log = LoggerFactory.getLogger(EnterpriseSentinelSupport.class);

  /**
   * Endpoint configuration key of the database under test. Overridable because the Redis Enterprise
   * test environments do not agree on a name.
   */
  static final String DB_ENDPOINT_NAME = System.getenv().getOrDefault("RE_SENTINEL_ENDPOINT",
    "m-standard");

  static final String SWITCH_MASTER_CHANNEL = "+switch-master";

  static final String PLUS_MASTER_CHANNEL = "+master";

  static final String MINUS_MASTER_CHANNEL = "-master";

  /**
   * Master name suffix under which Redis Enterprise announces the endpoint's internal address. A
   * client asking for {@code <db>} must not act on these, and does not: the listener filters on
   * exact name equality.
   */
  static final String INTERNAL_SUFFIX = "@internal";

  private static final int PROBE_TIMEOUT_MILLIS = 3000;

  private EnterpriseSentinelSupport() {
  }

  /**
   * Discovery-service addresses, taken verbatim from the {@code discovery_endpoints} field of the
   * endpoint configuration - one entry per cluster node.
   * <p>
   * The addresses carry no credentials anywhere they are used: the discovery service implements no
   * {@code AUTH}, and its {@code HELLO} rejects every argument past the protocol version. Database
   * credentials belong on the data-node config only.
   */
  static List<HostAndPort> sentinelHostsAndPorts(EndpointConfig endpoint) {
    return endpoint.getDiscoveryHostsAndPorts();
  }

  /**
   * Client config for connections to the discovery service itself: no protocol negotiation (so no
   * {@code HELLO}), no credentials, and no {@code CLIENT SETINFO}, none of which it implements.
   */
  static JedisClientConfig sentinelClientConfig() {
    return DefaultJedisClientConfig.builder().serverDefaultProtocol()
        .socketTimeoutMillis(PROBE_TIMEOUT_MILLIS).connectionTimeoutMillis(PROBE_TIMEOUT_MILLIS)
        .build();
  }

  /**
   * First discovery-service address that answers {@code PING}, or {@code null} if none does.
   * <p>
   * Used in preference to simply taking the first configured address: the service runs on every
   * node and answers for every database, so any reachable node will do and one node being down must
   * not fail the test.
   */
  static HostAndPort firstReachableSentinel(List<HostAndPort> sentinels) {
    for (HostAndPort sentinel : sentinels) {
      try (Jedis jedis = new Jedis(sentinel, sentinelClientConfig())) {
        if ("PONG".equals(jedis.ping())) {
          log.info("Discovery service reachable at {}", sentinel);
          return sentinel;
        }
      } catch (RuntimeException e) {
        log.warn("Discovery service not reachable at {}: {}", sentinel, e.toString());
      }
    }
    return null;
  }

  static boolean databaseReachable(EndpointConfig endpoint) {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().socketTimeoutMillis(PROBE_TIMEOUT_MILLIS)
            .connectionTimeoutMillis(PROBE_TIMEOUT_MILLIS).build())) {
      return "PONG".equals(jedis.ping());
    } catch (RuntimeException e) {
      log.warn("Database not reachable at {}: {}", endpoint.getHostAndPort(), e.toString());
      return false;
    }
  }

  static String missingDiscoveryEndpoints() {
    return "Skipping: endpoint '" + DB_ENDPOINT_NAME
        + "' does not advertise 'discovery_endpoints'. Whatever generates endpoints.json for the "
        + "environment should emit one '<node-addr>:8001' entry per cluster node.";
  }

  static String unreachableDiscoveryService(List<HostAndPort> sentinels) {
    return "Skipping: no Redis Enterprise discovery service answered PING on " + sentinels
        + ". Either port 8001 is not reachable (security-group ingress), or the service is disabled "
        + "('rladmin cluster config services sentinel_service enabled'; check 'rladmin info services_config').";
  }

  static String unreachableDatabase(EndpointConfig endpoint) {
    return "Skipping: database '" + DB_ENDPOINT_NAME + "' did not answer PING at "
        + endpoint.getHostAndPort();
  }

  /**
   * Address the discovery service currently reports for a master name. The authoritative,
   * client-observable answer to "where is the endpoint now", independent of any client state.
   */
  static HostAndPort reportedMaster(HostAndPort sentinel, String masterName) {
    try (Jedis jedis = new Jedis(sentinel, sentinelClientConfig())) {
      List<String> address = jedis.sentinelGetMasterAddrByName(masterName);
      if (address == null || address.size() != 2) {
        throw new IllegalStateException("Discovery service at " + sentinel
            + " does not monitor a master named '" + masterName + "': " + address);
      }
      return new HostAndPort(address.get(0), Integer.parseInt(address.get(1)));
    }
  }

  /**
   * Captures {@code +switch-master}, {@code +master} and {@code -master} straight off the discovery
   * service with {@code SUBSCRIBE}, which is what Redis Enterprise supports.
   * <p>
   * Deliberately independent of the client's own listener, so that "Redis Enterprise did not
   * publish" can be told apart from "the client did not react".
   */
  static final class SwitchMasterCapture implements Closeable {

    private final Jedis jedis;

    private final JedisPubSub pubSub;

    private final Thread thread;

    private final List<String[]> messages = new CopyOnWriteArrayList<>();

    private final AtomicReference<Instant> firstSwitchMasterAt = new AtomicReference<>();

    private volatile boolean recording = true;

    SwitchMasterCapture(HostAndPort sentinel) {
      this.jedis = new Jedis(sentinel, DefaultJedisClientConfig.builder().serverDefaultProtocol()
          .connectionTimeoutMillis(PROBE_TIMEOUT_MILLIS).build());

      this.pubSub = new JedisPubSub() {

        @Override
        public void onMessage(String channel, String message) {
          if (!recording) {
            log.info("Discovery service published (outside capture window, ignored): {} {}",
              channel, message);
            return;
          }
          log.info("Discovery service published: {} {}", channel, message);
          // Recorded here rather than after the await, so the reaction measurement is not inflated
          // by the poll interval.
          if (SWITCH_MASTER_CHANNEL.equals(channel)) {
            firstSwitchMasterAt.compareAndSet(null, Instant.now());
          }
          messages.add(new String[] { channel, message });
        }

      };

      this.thread = new Thread(() -> jedis.subscribe(pubSub, SWITCH_MASTER_CHANNEL,
        PLUS_MASTER_CHANNEL, MINUS_MASTER_CHANNEL), "re-discovery-capture");
      this.thread.setDaemon(true);
      this.thread.start();
    }

    /**
     * Stop recording without closing the connection, so restore steps do not pollute assertions.
     */
    void stopRecording() {
      this.recording = false;
    }

    void clear() {
      messages.clear();
      firstSwitchMasterAt.set(null);
    }

    Instant firstSwitchMasterAt() {
      return firstSwitchMasterAt.get();
    }

    /**
     * Payloads seen on {@code +switch-master} whose master name is exactly {@code masterName}.
     * Matching on the {@code masterName + " "} prefix is what the client's own listener does, and
     * is why {@code <db>@internal ...} does not match {@code <db>}.
     */
    List<String> switchMasterPayloads(String masterName) {
      List<String> payloads = new ArrayList<>();
      for (String[] message : messages) {
        if (SWITCH_MASTER_CHANNEL.equals(message[0]) && message[1].startsWith(masterName + " ")) {
          payloads.add(message[1]);
        }
      }
      return payloads;
    }

    List<String[]> messages() {
      return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * @return whether the capturing subscription is still alive, so that "no event" can be told
     *         apart from "the subscriber was disconnected".
     */
    boolean isAlive() {
      return thread.isAlive() && jedis.isConnected();
    }

    @Override
    public void close() {
      try {
        pubSub.unsubscribe();
      } catch (RuntimeException e) {
        log.debug("Ignoring failure to unsubscribe the discovery-service capture", e);
      }
      try {
        jedis.close();
      } catch (RuntimeException e) {
        log.debug("Ignoring failure to close the discovery-service capture", e);
      }
      thread.interrupt();
    }
  }
}
