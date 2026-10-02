package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.redis.test.fi.FaultInjectorClient;
import com.redis.test.fi.StandaloneEffect;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointType;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.util.KeyValue;

/**
 * T.3 Configuration &amp; Handshake — verifies the maintenance-notifications handshake against the
 * server via CLIENT MAINT_NOTIFICATIONS_INFO: enablement (T.3.1) and the requested endpoint type
 * (T.3.2). These tests fire no fault-injector effect and mutate nothing, so one database serves the
 * whole class and only the client is rebuilt per configuration.
 */
@Tag("scenario")
public class MaintNotificationsConfigIT {

  // Reuse the base's client: referencing its static field forces the base class (and its DNS-cache
  // TTL static block) to initialize at load time, before the first FI/endpoint name lookup.
  private static final FaultInjectorClient faultInjector = MaintNotificationsScenarioBase.faultInjector;

  private static final String STATUS_ON = "on";
  private static final String STATUS_OFF = "off";

  /** The moving-endpoint-type value the server reports for each requested {@link EndpointType}. */
  private static final Map<EndpointType, String> EXPECTED_ENDPOINT_TYPE = new LinkedHashMap<>();
  static {
    EXPECTED_ENDPOINT_TYPE.put(EndpointType.EXTERNAL_IP, "external-ip");
    EXPECTED_ENDPOINT_TYPE.put(EndpointType.INTERNAL_IP, "internal-ip");
    EXPECTED_ENDPOINT_TYPE.put(EndpointType.EXTERNAL_FQDN, "external-fqdn");
    EXPECTED_ENDPOINT_TYPE.put(EndpointType.INTERNAL_FQDN, "internal-fqdn");
    EXPECTED_ENDPOINT_TYPE.put(EndpointType.NONE, "none");
  }

  private static Map<String, Object> sharedOutput;
  private static long sharedBdbId = -1;

  /**
   * Creates the single shared database. The trigger is arbitrary — no effect fires — but the
   * dbconfig comes from a fresh discovery so the database gets its own endpoint name, as elsewhere.
   */
  @BeforeAll
  static void createSharedDatabase() {
    Map<String, Object> dbConfig = faultInjector.getStandaloneTriggers(StandaloneEffect.CONN_DROP)
        .trigger("endpoint_rebind").requirement("single").dbConfig();
    Map<String, Object> output = faultInjector.createDatabase(dbConfig);
    // record the id before any further step so @AfterAll cleans up even on a later failure
    sharedBdbId = ((Number) output.get("bdb_id")).longValue();
    sharedOutput = output;
    URI endpoint = URI.create((String) ((List<?>) output.get("endpoints")).get(0));
    MaintNotificationsScenarioBase.awaitEndpointConnectable(endpoint);
  }

  @AfterAll
  static void deleteSharedDatabase() {
    if (sharedBdbId >= 0) {
      faultInjector.deleteDatabase(sharedBdbId);
      sharedBdbId = -1;
    }
  }

  /** T.3.1 — the default (AUTO) handshake enables notifications; the server reports them active. */
  @Test
  @Timeout(30)
  void enabledActivatesNotificationsOnServer() {
    MaintenanceNotificationsConfig config = MaintenanceNotificationsConfig.builder().mode(Mode.AUTO)
        .build();
    try (RedisClient client = buildClient(config);
        Connection connection = client.getPool().getResource()) {
      MaintNotificationsInfo info = connection.executeCommand(maintNotificationsInfo());
      assertEquals(STATUS_ON, info.status(),
        "AUTO handshake must enable maintenance notifications on the server");
    }
  }

  /** T.3.1 — DISABLED skips the handshake; the server reports notifications inactive. */
  @Test
  @Timeout(30)
  void disabledDoesNotActivateNotificationsOnServer() {
    try (RedisClient client = buildClient(MaintenanceNotificationsConfig.DISABLED);
        Connection connection = client.getPool().getResource()) {
      MaintNotificationsInfo info = connection.executeCommand(maintNotificationsInfo());
      assertEquals(STATUS_OFF, info.status(),
        "DISABLED must not enable maintenance notifications on the server");
    }
  }

  static Stream<EndpointType> configuredEndpointTypes() {
    return EXPECTED_ENDPOINT_TYPE.keySet().stream();
  }

  /** T.3.2 — the requested endpoint type is the one the server records for the connection. */
  @ParameterizedTest(name = "endpoint-type {0}")
  @MethodSource("configuredEndpointTypes")
  @Timeout(30)
  void endpointTypeAcknowledgedByServer(EndpointType endpointType) {
    MaintenanceNotificationsConfig config = MaintenanceNotificationsConfig.builder()
        .mode(Mode.ENABLED).endpointType(endpointType).build();
    try (RedisClient client = buildClient(config);
        Connection connection = client.getPool().getResource()) {
      MaintNotificationsInfo info = connection.executeCommand(maintNotificationsInfo());
      assertEquals(STATUS_ON, info.status(),
        "requesting an endpoint type must enable notifications");
      assertEquals(EXPECTED_ENDPOINT_TYPE.get(endpointType), info.movingEndpointType(),
        "server must record the requested moving-endpoint-type");
    }
  }

  private static RedisClient buildClient(MaintenanceNotificationsConfig maintenance) {
    return MaintNotificationsScenarioBase.buildClient(sharedOutput, maintenance, null);
  }

  /**
   * CLIENT MAINT_NOTIFICATIONS_INFO as a typed command, mirroring the client's own CommandObjects:
   * the reply is decoded by a dedicated builder into a {@link MaintNotificationsInfo}.
   */
  static CommandObject<MaintNotificationsInfo> maintNotificationsInfo() {
    return new CommandObject<>(
        new CommandArguments(Protocol.Command.CLIENT).add("MAINT_NOTIFICATIONS_INFO"),
        MaintNotificationsInfo.BUILDER);
  }

  /**
   * Parsed CLIENT MAINT_NOTIFICATIONS_INFO reply: the enablement status and the negotiated
   * moving-endpoint-type. The RESP3 reply is a map whose {@code parameters} entry is a nested map.
   */
  static final class MaintNotificationsInfo {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_PARAMETERS = "parameters";
    private static final String PARAM_MOVING_ENDPOINT_TYPE = "moving-endpoint-type";

    static final Builder<MaintNotificationsInfo> BUILDER = new Builder<MaintNotificationsInfo>() {
      @Override
      public MaintNotificationsInfo build(Object data) {
        return new MaintNotificationsInfo(BuilderFactory.ENCODED_OBJECT_MAP.build(data));
      }
    };

    private final String status;
    private final String movingEndpointType;

    private MaintNotificationsInfo(Map<String, Object> reply) {
      this.status = (String) reply.get(FIELD_STATUS);
      this.movingEndpointType = movingEndpointType(reply);
    }

    /** {@code on} or {@code off}. */
    String status() {
      return status;
    }

    /** The negotiated moving-endpoint-type, or null when notifications are disabled. */
    String movingEndpointType() {
      return movingEndpointType;
    }

    private static String movingEndpointType(Map<String, Object> reply) {
      Object parameters = reply.get(FIELD_PARAMETERS);
      if (parameters instanceof List) {
        for (Object entry : (List<?>) parameters) {
          KeyValue<?, ?> kv = (KeyValue<?, ?>) entry;
          if (PARAM_MOVING_ENDPOINT_TYPE.equals(kv.getKey())) {
            return (String) kv.getValue();
          }
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return "status=" + status + ", moving-endpoint-type=" + movingEndpointType;
    }
  }
}
