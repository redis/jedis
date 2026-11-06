package com.redis.test.fi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

/**
 * Minimal client for the Redis fault-injector service: two HTTP primitives, one poll helper, and
 * the operations the maintenance-notification scenario tests need for both effect families
 * (standalone {@code topology_change_standalone}, OSS cluster {@code /slot-migrate}). The read-only
 * discovery envelope is typed ({@link TriggerCatalog}); everything round-tripped — dbconfigs and
 * action outputs — stays an opaque map, the FI owns those schemas. Framework-free (JDK HTTP + gson,
 * no jedis imports) and JDK 8 compatible so it can be extracted and reused by other JVM clients.
 */
public final class FaultInjectorClient {

  public static final String BASE_URL_ENV = "FAULT_INJECTION_API_URL";
  /** The name the CI pipeline exports; read when {@link #BASE_URL_ENV} is unset. */
  public static final String CI_BASE_URL_ENV = "RE_FAULT_INJECTOR_URL";
  private static final String DEFAULT_BASE_URL = "http://127.0.0.1:20324";

  private static final Duration POLL_INTERVAL = Duration.ofMillis(300);
  /** Multi-shard OSS-cluster databases take longer to become active than a standalone one. */
  private static final Duration DATABASE_TIMEOUT = Duration.ofSeconds(180);
  private static final Duration EFFECT_TIMEOUT = Duration.ofSeconds(180);
  /**
   * ASM reshards are a cascade of slot moves driven by the decision engine; the FI's own budget.
   */
  private static final Duration ASM_EFFECT_TIMEOUT = Duration.ofSeconds(600);
  private static final int HTTP_TIMEOUT_MILLIS = 10_000;
  /** {@code /slot-migrate/setup} and {@code /teardown} block until the database is ready / gone. */
  private static final int BLOCKING_ROUTE_TIMEOUT_MILLIS = 300_000;

  private final String baseUrl;
  /**
   * Integral numbers must parse as Long, not gson's default Double: discovered dbconfigs are
   * re-serialized verbatim for create_database, and 2.0 for shards_count would corrupt the payload.
   */
  private final Gson gson = new GsonBuilder()
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

  /**
   * Base URL from the {@value #BASE_URL_ENV} env var, then {@value #CI_BASE_URL_ENV}, defaulting to
   * a local fault injector.
   */
  public FaultInjectorClient() {
    this(firstNonNull(System.getenv(BASE_URL_ENV), System.getenv(CI_BASE_URL_ENV),
      DEFAULT_BASE_URL));
  }

  public FaultInjectorClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private static String firstNonNull(String... values) {
    for (String value : values) {
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    throw new IllegalArgumentException("no value");
  }

  // --- HTTP primitives + poll helper ---

  /** {@code GET {base}{path}}; the JSON response as an opaque map. */
  public Map<String, Object> get(String path) {
    return parseObject(request("GET", path, null, HTTP_TIMEOUT_MILLIS));
  }

  /** {@code POST /action {"type", "parameters"}}; the id to poll with {@link #awaitAction}. */
  public String postAction(String type, Map<String, Object> parameters) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", type);
    payload.put("parameters", parameters);
    return actionId(
      parseObject(request("POST", "/action", gson.toJson(payload), HTTP_TIMEOUT_MILLIS)), type);
  }

  /** The {@code status} of an action: pending, running, success, failed or unknown. */
  public String actionStatus(String actionId) {
    return String.valueOf(get("/action/" + actionId).get("status"));
  }

  /**
   * Polls the action every ~300 ms and returns the status response once it succeeds. Throws on
   * {@code failed}, or on {@code timeout} — checked after each poll, so a completion during the
   * final interval is not discarded.
   */
  public Map<String, Object> awaitAction(String actionId, Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);
    while (true) {
      Map<String, Object> response = get("/action/" + actionId);
      Object status = response.get("status");
      if ("success".equals(status)) {
        return response;
      }
      if ("failed".equals(status)) {
        throw new FaultInjectorException("Action " + actionId + " failed: " + response);
      }
      if (!Instant.now().isBefore(deadline)) {
        throw new FaultInjectorException(
            "Timeout after " + timeout + " waiting for action " + actionId);
      }
      try {
        Thread.sleep(POLL_INTERVAL.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new FaultInjectorException("Interrupted waiting for action " + actionId, e);
      }
    }
  }

  // --- discovery ---

  /** The discovery for an effect of either family; see {@link TriggerCatalog}. */
  public Effect discover(EffectType effect) {
    if (effect instanceof StandaloneEffect) {
      return getStandaloneTriggers((StandaloneEffect) effect);
    }
    if (effect instanceof SlotMigrateEffect) {
      return getSlotMigrateTriggers((SlotMigrateEffect) effect);
    }
    throw new IllegalArgumentException("Unknown effect family: " + effect);
  }

  /**
   * Discovers the triggers producing {@code effect}, each with the requirements (dbconfigs) it can
   * run under. The envelope is typed; the dbconfigs inside stay opaque maps.
   */
  public Effect getStandaloneTriggers(StandaloneEffect effect) {
    return Effect.parse(effect, get("/topology-change-standalone?effect=" + effect.wireName()
        + "&cluster_index=0&include_tls=true"));
  }

  /**
   * Discovers the slot-migrate triggers producing {@code effect}. The FI lists the rladmin-driven
   * triggers and the ASM {@code reshard} trigger on disjoint {@code asm} views, so both are fetched
   * and folded into one {@link Effect}; a requirement's {@link Requirement#asm()} tells which path
   * provisions it.
   */
  public Effect getSlotMigrateTriggers(SlotMigrateEffect effect) {
    String query = "/slot-migrate?effect=" + effect.wireName()
        + "&cluster_index=0&include_tls=true&asm=";
    return Effect.merge(Effect.parse(effect, get(query + "false")),
      Effect.parse(effect, get(query + "true")));
  }

  // --- database lifecycle ---

  /** Creates a database from a discovery-provided dbconfig (passed verbatim). */
  @SuppressWarnings("unchecked")
  public Database createDatabase(Map<String, Object> dbConfig) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("database_config", dbConfig);
    Map<String, Object> response = awaitAction(postAction("create_database", parameters),
      DATABASE_TIMEOUT);
    Object output = response.get("output");
    if (!(output instanceof Map)) {
      throw new FaultInjectorException("create_database returned no output: " + response);
    }
    return Database.parse((Map<String, Object>) output);
  }

  public void deleteDatabase(long bdbId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", bdbId);
    awaitAction(postAction("delete_database", parameters), DATABASE_TIMEOUT);
  }

  /**
   * Provisions the database a scenario needs: {@code create_database} with the requirement's
   * dbconfig, or the blocking {@code /slot-migrate/setup} for an ASM requirement (the FI owns the
   * five ASM flags and the cluster's {@code asm_client_maint_notifications} flag).
   */
  public Database provision(Scenario scenario) {
    Requirement requirement = scenario.requirement();
    if (!requirement.asm()) {
      return createDatabase(requirement.dbConfig());
    }
    String path = "/slot-migrate/setup?effect=" + scenario.effect().wireName() + "&trigger="
        + scenario.trigger().name() + "&cluster_index=0&variant_index=" + requirement.index();
    return Database.parse(parseObject(request("POST", path, null, BLOCKING_ROUTE_TIMEOUT_MILLIS)));
  }

  /** Releases a provisioned database: ASM teardown (restores the cluster flag) or delete. */
  public void release(Database database) {
    if (database.setupId() == null) {
      deleteDatabase(database.id());
    } else {
      request("POST", "/slot-migrate/teardown?setup_id=" + database.setupId(), null,
        BLOCKING_ROUTE_TIMEOUT_MILLIS);
    }
  }

  // --- effects ---

  /** Fires {@code effect} via {@code trigger} on the database and waits for it to complete. */
  public Map<String, Object> triggerEffect(long bdbId, StandaloneEffect effect, String trigger) {
    return awaitAction(startStandaloneEffect(bdbId, effect, trigger), EFFECT_TIMEOUT);
  }

  /** Fires the scenario's effect on the database and waits for it to complete. */
  public Map<String, Object> triggerEffect(Database database, Scenario scenario) {
    return awaitAction(startEffect(database, scenario, Collections.emptyMap()),
      effectTimeout(scenario));
  }

  /**
   * Fires the scenario's effect without waiting; the id is polled with {@link #awaitAction} (use
   * {@link #effectTimeout}) or inspected with {@link #actionStatus}. {@code extra} query parameters
   * (slot-migrate only: {@code source_node}, {@code target_node}, {@code slots}, ...) pin what the
   * FI would otherwise auto-select.
   */
  public String startEffect(Database database, Scenario scenario, Map<String, Object> extra) {
    EffectType effect = scenario.effect();
    String trigger = scenario.trigger().name();
    if (effect instanceof StandaloneEffect) {
      return startStandaloneEffect(database.id(), (StandaloneEffect) effect, trigger);
    }
    StringBuilder path = new StringBuilder("/slot-migrate?effect=").append(effect.wireName())
        .append("&trigger=").append(trigger).append("&cluster_index=0");
    if (database.setupId() != null) {
      path.append("&setup_id=").append(database.setupId());
    } else {
      path.append("&bdb_id=").append(database.id());
    }
    for (Map.Entry<String, Object> param : extra.entrySet()) {
      path.append('&').append(param.getKey()).append('=').append(encode(param.getValue()));
    }
    return actionId(parseObject(request("POST", path.toString(), null, HTTP_TIMEOUT_MILLIS)),
      "slot-migrate " + trigger);
  }

  /** How long the scenario's effect may take end to end. */
  public Duration effectTimeout(Scenario scenario) {
    return scenario.requirement().asm() ? ASM_EFFECT_TIMEOUT : EFFECT_TIMEOUT;
  }

  private String startStandaloneEffect(long bdbId, StandaloneEffect effect, String trigger) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", bdbId);
    parameters.put("cluster_index", 0);
    parameters.put("effect", effect.wireName());
    parameters.put("trigger", trigger);
    return postAction("topology_change_standalone", parameters);
  }

  // --- http/json plumbing ---

  private static String actionId(Map<String, Object> response, String what) {
    Object actionId = response.get("action_id");
    if (actionId == null) {
      throw new FaultInjectorException("No action_id in response for " + what);
    }
    return actionId.toString();
  }

  private static String encode(Object value) {
    try {
      return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private String request(String method, String path, String jsonBody, int readTimeoutMillis) {
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
      conn.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
      conn.setReadTimeout(readTimeoutMillis);
      conn.setRequestMethod(method);
      if (jsonBody != null) {
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream()) {
          out.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
      }

      int status = conn.getResponseCode();
      InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
      String body = stream == null ? "" : readAll(stream);
      if (status >= 400) {
        throw new FaultInjectorException(
            method + " " + path + " failed with HTTP " + status + ": " + body);
      }
      return body;
    } catch (IOException e) {
      throw new FaultInjectorException(method + " " + path + " failed", e);
    }
  }

  private static String readAll(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  private Map<String, Object> parseObject(String json) {
    Map<String, Object> parsed = gson.fromJson(json, new TypeToken<Map<String, Object>>() {
    }.getType());
    return parsed == null ? new HashMap<>() : parsed;
  }
}
