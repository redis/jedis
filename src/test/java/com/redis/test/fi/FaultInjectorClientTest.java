package com.redis.test.fi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
    assertEquals("/topology-change-standalone?effect=data_movement_no_conn_drop&cluster_index=0",
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
    Map<String, Object> output = client.createDatabase(dbConfig);

    assertEquals(42L, output.get("bdb_id"));
    assertEquals("redis://db.example:12000", ((List<?>) output.get("endpoints")).get(0));
    Map<String, Object> payload = parse(requests.get(1)[2]);
    assertEquals("create_database", payload.get("type"));
    String createBody = requests.get(1)[2];
    assertTrue(createBody.contains("\"shards_count\":2,"),
      "integral dbconfig fields must round-trip unchanged, got: " + createBody);
    assertTrue(createBody.contains("\"memory_size\":134217728"),
      "integral dbconfig fields must round-trip unchanged, got: " + createBody);
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

    StandaloneTriggerCatalog catalog = StandaloneTriggerCatalog.resolve(client);

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
    assertThrows(IllegalArgumentException.class, () -> maintenance.requirement("no_such_config"));

    assertEquals(6, catalog.effect(StandaloneEffect.CONN_DROP).clusterNodes());
    assertThrows(IllegalArgumentException.class,
      () -> catalog.effect(StandaloneEffect.CONN_DROP).trigger("no_such_trigger"));
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
