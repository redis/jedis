package com.redis.test.fi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Wire-format and parsing tests for {@link FaultInjectorClient} against a scripted in-process HTTP
 * server standing in for the fault injector.
 */
public class FaultInjectorClientTest {

  private static final Gson GSON = new Gson();

  private HttpServer server;
  private FaultInjectorClient client;

  /** Recorded requests: method, path+query, body. Thread-safe: written on the server thread. */
  private final List<String[]> requests = Collections.synchronizedList(new ArrayList<>());
  /** Scripted responses served in order; the last one repeats. Polled on the server thread. */
  private final Queue<String> responses = new ConcurrentLinkedQueue<>();

  @BeforeEach
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      requests.add(new String[] { exchange.getRequestMethod(), exchange.getRequestURI().toString(),
          readBody(exchange) });
      String body = responses.size() > 1 ? responses.poll() : responses.peek();
      respond(exchange, body == null ? "{}" : body);
    });
    server.start();
    client = new FaultInjectorClient("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  public void tearDown() {
    server.stop(0);
    requests.clear();
    responses.clear();
  }

  @Test
  public void postActionSendsTypeAndParameters() {
    responses.add("{\"action_id\": \"a1\"}");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", 7);
    String actionId = client.postAction("failover", parameters);

    assertEquals("a1", actionId);
    assertEquals("POST", requests.get(0)[0]);
    assertEquals("/action", requests.get(0)[1]);
    Map<String, Object> payload = parse(requests.get(0)[2]);
    assertEquals("failover", payload.get("type"));
    assertEquals(7, ((Number) ((Map<?, ?>) payload.get("parameters")).get("bdb_id")).intValue());
  }

  @Test
  public void postActionWithoutActionIdThrows() {
    responses.add("{}");

    FaultInjectorException e = assertThrows(FaultInjectorException.class,
      () -> client.postAction("failover", new HashMap<>()));
    assertTrue(e.getMessage().contains("No action_id"));
  }

  @Test
  public void awaitActionPollsUntilSuccess() {
    responses.add("{\"status\": \"running\"}");
    responses.add("{\"status\": \"running\"}");
    responses.add("{\"status\": \"success\", \"output\": {\"bdb_id\": 5}}");

    Map<String, Object> response = client.awaitAction("a1", Duration.ofSeconds(5));

    assertEquals("success", response.get("status"));
    assertTrue(requests.size() >= 3, "polled until terminal status");
    assertEquals("/action/a1", requests.get(0)[1]);
  }

  @Test
  public void awaitActionThrowsOnFailed() {
    responses.add("{\"status\": \"failed\", \"error\": \"boom\"}");

    FaultInjectorException e = assertThrows(FaultInjectorException.class,
      () -> client.awaitAction("a1", Duration.ofSeconds(2)));
    assertTrue(e.getMessage().contains("boom"));
  }

  @Test
  public void awaitActionTimesOut() {
    responses.add("{\"status\": \"running\"}");

    assertThrows(FaultInjectorException.class,
      () -> client.awaitAction("a1", Duration.ofMillis(400)));
  }

  @Test
  public void getStandaloneTriggersRequestsEffectAndTypesTheEnvelope() {
    responses.add("{\"triggers\": [{\"name\": \"migrate\","
        + " \"requirements\": [{\"dbconfig\": {\"name\": \"m-standard\"}}]}]}");

    Effect effect = client.getStandaloneTriggers(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP);

    assertEquals("GET", requests.get(0)[0]);
    assertEquals(
      "/topology-change-standalone?effect=data_movement_no_conn_drop&cluster_index=0&include_tls=true",
      requests.get(0)[1]);
    assertEquals("m-standard", effect.trigger("migrate").requirement().dbConfig().get("name"));
  }

  @Test
  public void createDatabasePassesDbconfigVerbatimAndReturnsOutput() {
    responses.add("{\"triggers\": [{\"name\": \"migrate\","
        + " \"requirements\": [{\"dbconfig\": {\"name\": \"m-standard\", \"shards_count\": 2,"
        + " \"memory_size\": 134217728}}]}]}");
    responses.add("{\"action_id\": \"a6\"}");
    responses.add("{\"status\": \"success\", \"output\": {\"bdb_id\": 42,"
        + " \"endpoints\": [\"redis://db.example:12000\"]}}");

    Map<String, Object> dbConfig = client
        .getStandaloneTriggers(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP).trigger("migrate")
        .requirement().dbConfig();
    Database database = client.createDatabase(dbConfig);

    assertEquals(42L, database.id());
    assertEquals(URI.create("redis://db.example:12000"), database.endpoint());
    assertEquals("db.example", database.host());
    assertEquals(12000, database.port());
    assertFalse(database.tls());
    Map<String, Object> payload = parse(requests.get(1)[2]);
    assertEquals("create_database", payload.get("type"));
    String createBody = requests.get(1)[2];
    assertTrue(createBody.contains("\"shards_count\":2,"),
      "integral dbconfig fields must round-trip unchanged, got: " + createBody);
    assertTrue(createBody.contains("\"memory_size\":134217728"),
      "integral dbconfig fields must round-trip unchanged, got: " + createBody);
  }

  @Test
  public void createDatabaseThrowsWhenTheActionReturnsNoOutput() {
    responses.add("{\"action_id\": \"a7\"}");
    responses.add("{\"status\": \"success\"}");

    FaultInjectorException e = assertThrows(FaultInjectorException.class,
      () -> client.createDatabase(Collections.singletonMap("name", "db")));
    assertTrue(e.getMessage().contains("no output"), e.getMessage());
  }

  @Test
  public void databaseCarriesCredentialsAndFoldsTheDefaultUser() {
    Map<String, Object> output = new HashMap<>();
    output.put("bdb_id", 7);
    output.put("endpoints", Collections.singletonList("rediss://db.example:12001"));
    output.put("username", "default");
    output.put("password", "s3cret");
    output.put("tls", true);

    Database database = Database.parse(output);

    assertEquals(7L, database.id());
    assertTrue(database.tls());
    assertNull(database.clientConfig().build().getUser(),
      "the default user must not be sent explicitly");
    assertEquals("s3cret", database.clientConfig().build().getPassword());
    assertFalse(database.toString().contains("s3cret"), "toString must not leak the password");

    output.put("username", "app");
    assertEquals("app", Database.parse(output).clientConfig().build().getUser());
  }

  @Test
  public void deleteDatabaseSendsBdbId() {
    responses.add("{\"action_id\": \"a4\"}");
    responses.add("{\"status\": \"success\"}");

    client.deleteDatabase(42);

    Map<String, Object> payload = parse(requests.get(0)[2]);
    assertEquals("delete_database", payload.get("type"));
    assertEquals(42, ((Number) ((Map<?, ?>) payload.get("parameters")).get("bdb_id")).intValue());
  }

  @Test
  public void triggerEffectSendsParametersAndAwaitsCompletion() {
    responses.add("{\"action_id\": \"a9\"}");
    responses.add("{\"status\": \"running\"}");
    responses.add("{\"status\": \"success\"}");

    Map<String, Object> response = client.triggerEffect(42,
      StandaloneEffect.DATA_MOVEMENT_CONN_DROP, "endpoint_rebind");

    assertEquals("success", response.get("status"));
    Map<String, Object> payload = parse(requests.get(0)[2]);
    assertEquals("topology_change_standalone", payload.get("type"));
    Map<?, ?> parameters = (Map<?, ?>) payload.get("parameters");
    assertEquals(42, ((Number) parameters.get("bdb_id")).intValue());
    assertEquals("data_movement_conn_drop", parameters.get("effect"));
    assertEquals("endpoint_rebind", parameters.get("trigger"));
  }

  @Test
  public void catalogResolvesEffectsTriggersAndRequirements() {
    // one response per effect, in StandaloneEffect declaration order
    responses.add("{\"cluster\": {\"index\": 0, \"nodes\": 6}, \"triggers\": ["
        + " {\"name\": \"maintenance_mode\", \"description\": \"evacuate\","
        + "  \"requirements\": [{\"dbconfig\": {\"name\": \"tcs-mm-1-a\"},"
        + "   \"cluster\": {\"min_nodes\": 3, \"actual_nodes\": 6},"
        + "   \"config\": \"single\", \"description\": \"Standalone config (single)\"},"
        + "  {\"dbconfig\": {\"name\": \"tcs-mm-tls-1-a\"},"
        + "   \"cluster\": {\"min_nodes\": 3, \"actual_nodes\": 6},"
        + "   \"config\": \"single_tls\", \"description\": \"Standalone config (single_tls)\"}]}]}");
    responses.add("{\"cluster\": {\"index\": 0, \"nodes\": 6}, \"triggers\": ["
        + " {\"name\": \"migrate\", \"description\": \"move shards\","
        + "  \"requirements\": [{\"dbconfig\": {\"name\": \"tcs-mig-1-a\"},"
        + "   \"cluster\": {\"min_nodes\": 3, \"actual_nodes\": 6},"
        + "   \"config\": \"single\", \"description\": \"Standalone config (single)\"}]}]}");
    responses.add("{\"cluster\": {\"index\": 0, \"nodes\": 6}, \"triggers\": []}");
    responses.add("{\"cluster\": {\"index\": 0, \"nodes\": 6}, \"triggers\": []}");

    TriggerCatalog catalog = TriggerCatalog.resolve(client, StandaloneEffect.class);

    assertEquals(2, catalog.allTriggers().size());
    Trigger migrate = catalog.effect(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP)
        .trigger("migrate");
    assertEquals("move shards", migrate.description());
    assertEquals("tcs-mig-1-a", migrate.requirement().dbConfig().get("name"));
    assertEquals(3, migrate.requirement().minNodes());

    Trigger maintenance = catalog.effect(StandaloneEffect.DATA_MOVEMENT_CONN_DROP)
        .trigger("maintenance_mode");
    assertEquals(2, maintenance.requirements().size());
    assertEquals("single_tls", maintenance.requirements().get(1).config());
    assertEquals("single", maintenance.requirement().config());
    assertEquals("tcs-mm-tls-1-a", maintenance.requirement("single_tls").dbConfig().get("name"));
    assertEquals("single_tls", maintenance.scenario("single_tls").requirement().config());
    assertTrue(maintenance.offers("single_tls"));
    assertFalse(maintenance.offers("no_such_config"));
    assertThrows(IllegalArgumentException.class, () -> maintenance.requirement("no_such_config"));

    assertEquals(6, catalog.effect(StandaloneEffect.CONN_DROP).clusterNodes());
    assertThrows(IllegalArgumentException.class,
      () -> catalog.effect(StandaloneEffect.CONN_DROP).trigger("no_such_trigger"));
    assertThrows(IllegalArgumentException.class,
      () -> catalog.effect(SlotMigrateEffect.SLOT_SHUFFLE));
  }

  @Test
  public void getSlotMigrateTriggersMergesBothAsmViewsAndNamesVariants() {
    // asm=false: rladmin triggers; requirements carry no "config", only oss_cluster_api (+ tls)
    responses.add("{\"cluster\": {\"index\": 0, \"nodes\": 3}, \"triggers\": ["
        + " {\"name\": \"migrate\", \"description\": \"move one shard\", \"requirements\": ["
        + "   {\"dbconfig\": {\"name\": \"sm-ip\"}, \"cluster\": {\"min_nodes\": 3},"
        + "    \"oss_cluster_api\": {\"ip_type\": \"external\", \"endpoint_type\": \"ip\"}},"
        + "   {\"dbconfig\": {\"name\": \"sm-host\"}, \"cluster\": {\"min_nodes\": 3},"
        + "    \"oss_cluster_api\": {\"ip_type\": \"external\", \"endpoint_type\": \"hostname\"}},"
        + "   {\"dbconfig\": {\"name\": \"sm-host-tls\"}, \"cluster\": {\"min_nodes\": 3}, \"tls\": true,"
        + "    \"oss_cluster_api\": {\"ip_type\": \"external\", \"endpoint_type\": \"hostname\"}}]},"
        + " {\"name\": \"failover\", \"description\": \"swap roles\", \"requirements\": ["
        + "   {\"dbconfig\": {\"name\": \"sm-fo\"}, \"cluster\": {\"min_nodes\": 3},"
        + "    \"oss_cluster_api\": {\"ip_type\": \"external\", \"endpoint_type\": \"ip\"}}]}]}");
    // asm=true: the reshard trigger only
    responses.add("{\"cluster\": {\"index\": 0, \"nodes\": 3}, \"triggers\": ["
        + " {\"name\": \"reshard\", \"description\": \"ASM\", \"requirements\": ["
        + "   {\"dbconfig\": {\"name\": \"sm-asm\"}, \"cluster\": {\"min_nodes\": 3}, \"asm\": true,"
        + "    \"oss_cluster_api\": {\"ip_type\": \"external\", \"endpoint_type\": \"ip\"}}]}]}");

    Effect effect = client.getSlotMigrateTriggers(SlotMigrateEffect.REMOVE);

    assertEquals("/slot-migrate?effect=remove&cluster_index=0&include_tls=true&asm=false",
      requests.get(0)[1]);
    assertEquals("/slot-migrate?effect=remove&cluster_index=0&include_tls=true&asm=true",
      requests.get(1)[1]);
    assertEquals(SlotMigrateEffect.REMOVE, effect.type());
    assertEquals(3, effect.triggers().size());
    Trigger migrate = effect.trigger("migrate");
    assertEquals("external/ip", migrate.requirement().config());
    assertEquals("external/hostname", migrate.requirements().get(1).config());
    assertEquals(1, migrate.requirements().get(1).index());
    Requirement tls = migrate.requirement("external/hostname+tls");
    assertTrue(tls.tls());
    assertEquals("hostname", tls.endpointType());
    assertEquals("sm-host-tls", tls.dbConfig().get("name"));
    Requirement asm = effect.trigger("reshard").requirement("external/ip+asm");
    assertTrue(asm.asm());
    assertFalse(migrate.requirement().asm());
    assertEquals("REMOVE/migrate[external/ip]", migrate.scenario().toString());
  }

  @Test
  public void startEffectPostsTheSlotMigrateQueryRouteWithPinnedNodes() {
    responses.add("{\"action_id\": \"s1\"}");
    Map<String, Object> output = new HashMap<>();
    output.put("bdb_id", 42);
    output.put("endpoints", Collections.singletonList("redis://db.example:13000"));
    Scenario scenario = slotMigrateScenario("remove-add", "migrate", false);

    Map<String, Object> extra = new HashMap<>();
    extra.put("source_node", 2);
    String actionId = client.startEffect(Database.parse(output), scenario, extra);

    assertEquals("s1", actionId);
    assertEquals("POST", requests.get(0)[0]);
    assertEquals(
      "/slot-migrate?effect=remove-add&trigger=migrate&cluster_index=0&bdb_id=42&source_node=2",
      requests.get(0)[1]);
    assertEquals("", requests.get(0)[2], "query route takes no body");
    assertEquals(Duration.ofSeconds(180), client.effectTimeout(scenario));
  }

  @Test
  public void startEffectUsesTheSetupIdOfAnAsmDatabase() {
    responses.add("{\"action_id\": \"s2\"}");
    Map<String, Object> output = new HashMap<>();
    output.put("bdb_id", 43);
    output.put("setup_id", "setup-7");
    output.put("endpoints", Collections.singletonList("redis://db.example:13001"));
    Scenario scenario = slotMigrateScenario("remove", "reshard", true);

    client.startEffect(Database.parse(output), scenario, Collections.emptyMap());

    assertEquals("/slot-migrate?effect=remove&trigger=reshard&cluster_index=0&setup_id=setup-7",
      requests.get(0)[1]);
    assertEquals(Duration.ofSeconds(600), client.effectTimeout(scenario));
  }

  @Test
  public void provisionAndReleaseUseSetupAndTeardownForAsmRequirements() {
    responses.add("{\"setup_id\": \"setup-9\", \"bdb_id\": 44, \"password\": \"pw\","
        + " \"tls\": false, \"endpoints\": [\"redis://db.example:13002\"]}");
    responses.add("{}");
    Scenario scenario = slotMigrateScenario("remove", "reshard", true);

    Database database = client.provision(scenario);
    client.release(database);

    assertEquals("setup-9", database.setupId());
    assertEquals(44L, database.id());
    assertEquals("pw", database.clientConfig().build().getPassword());
    assertEquals("POST", requests.get(0)[0]);
    assertEquals(
      "/slot-migrate/setup?effect=remove&trigger=reshard&cluster_index=0&variant_index=0",
      requests.get(0)[1]);
    assertEquals("/slot-migrate/teardown?setup_id=setup-9", requests.get(1)[1]);
  }

  @Test
  public void provisionAndReleaseUseCreateAndDeleteOtherwise() {
    responses.add("{\"action_id\": \"c1\"}");
    responses.add("{\"status\": \"success\", \"output\": {\"bdb_id\": 45,"
        + " \"endpoints\": [\"redis://db.example:13003\"]}}");
    Scenario scenario = slotMigrateScenario("slot-shuffle", "migrate", false);

    Database database = client.provision(scenario);

    assertNull(database.setupId());
    assertEquals("create_database", parse(requests.get(0)[2]).get("type"));

    requests.clear();
    responses.clear();
    responses.add("{\"action_id\": \"d1\"}");
    responses.add("{\"status\": \"success\"}");
    client.release(database);
    assertEquals("delete_database", parse(requests.get(0)[2]).get("type"));
  }

  @Test
  public void actionStatusReadsTheStatusWithoutPolling() {
    responses.add("{\"status\": \"running\"}");

    assertEquals("running", client.actionStatus("a1"));
    assertEquals(1, requests.size());
    assertEquals("/action/a1", requests.get(0)[1]);
  }

  /** A slot-migrate scenario parsed from a minimal discovery response, no HTTP involved. */
  private static Scenario slotMigrateScenario(String effect, String trigger, boolean asm) {
    Map<String, Object> response = parse("{\"triggers\": [{\"name\": \"" + trigger + "\","
        + " \"requirements\": [{\"dbconfig\": {\"name\": \"db\"}, \"asm\": " + asm + ","
        + "  \"oss_cluster_api\": {\"ip_type\": \"external\", \"endpoint_type\": \"ip\"}}]}]}");
    SlotMigrateEffect type = null;
    for (SlotMigrateEffect candidate : SlotMigrateEffect.values()) {
      if (candidate.wireName().equals(effect)) {
        type = candidate;
      }
    }
    return Effect.parse(type, response).trigger(trigger).scenario();
  }

  @Test
  public void httpErrorSurfacesBody() {
    server.removeContext("/");
    server.createContext("/", exchange -> {
      byte[] body = "{\"detail\": \"Validation Error\"}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(422, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    });

    FaultInjectorException e = assertThrows(FaultInjectorException.class,
      () -> client.get("/action/a1"));
    assertTrue(e.getMessage().contains("422"));
    assertTrue(e.getMessage().contains("Validation Error"));
  }

  private static Map<String, Object> parse(String json) {
    return GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
    }.getType());
  }

  private static String readBody(HttpExchange exchange) throws IOException {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int n;
    while ((n = exchange.getRequestBody().read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  private static void respond(HttpExchange exchange, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
