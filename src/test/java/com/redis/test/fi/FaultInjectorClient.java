package com.redis.test.fi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

/**
 * Minimal client for the Redis fault-injector service: two HTTP primitives, one poll helper, and
 * the four operations the maintenance-notification scenario tests need. The read-only discovery
 * envelope is typed ({@link StandaloneTriggerCatalog}); everything round-tripped — dbconfigs and
 * action outputs — stays an opaque map, the FI owns those schemas. Framework-free (JDK HTTP + gson,
 * no jedis imports) and JDK 8 compatible so it can be extracted and reused by other JVM clients.
 */
public final class FaultInjectorClient {

  public static final String BASE_URL_ENV = "FAULT_INJECTION_API_URL";
  private static final String DEFAULT_BASE_URL = "http://127.0.0.1:20324";

  private static final Duration POLL_INTERVAL = Duration.ofMillis(300);
  private static final Duration DATABASE_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration EFFECT_TIMEOUT = Duration.ofSeconds(180);
  private static final int HTTP_TIMEOUT_MILLIS = 10_000;

  private final String baseUrl;
  /**
   * Integral numbers must parse as Long, not gson's default Double: discovered dbconfigs are
   * re-serialized verbatim for create_database, and 2.0 for shards_count would corrupt the payload.
   */
  private final Gson gson = new GsonBuilder()
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

  /** Base URL from the {@value #BASE_URL_ENV} env var, defaulting to a local fault injector. */
  public FaultInjectorClient() {
    this(System.getenv().getOrDefault(BASE_URL_ENV, DEFAULT_BASE_URL));
  }

  public FaultInjectorClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  // --- HTTP primitives + poll helper ---

  /** {@code GET {base}{path}}; the JSON response as an opaque map. */
  public Map<String, Object> get(String path) {
    return parseObject(request("GET", path, null));
  }

  /** {@code POST /action {"type", "parameters"}}; the id to poll with {@link #awaitAction}. */
  public String postAction(String type, Map<String, Object> parameters) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", type);
    payload.put("parameters", parameters);

    Object actionId = parseObject(request("POST", "/action", gson.toJson(payload)))
        .get("action_id");
    if (actionId == null) {
      throw new FaultInjectorException("No action_id in response for action " + type);
    }
    return actionId.toString();
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

  // --- operations ---

  /**
   * Discovers the triggers producing {@code effect}, each with the requirements (dbconfigs) it can
   * run under. The envelope is typed; the dbconfigs inside stay opaque maps.
   */
  public Effect getStandaloneTriggers(StandaloneEffect effect) {
    return Effect.parse(effect, get("/topology-change-standalone?effect=" + effect.wireName()
        + "&cluster_index=0&include_tls=true"));
  }

  /**
   * Creates a database from a discovery-provided dbconfig (passed verbatim) and returns the action
   * output: {@code {"bdb_id", "username", "password", "tls", "endpoints": ["redis://host:port"]}}.
   */
  public Map<String, Object> createDatabase(Map<String, Object> dbConfig) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("database_config", dbConfig);
    Map<String, Object> response = awaitAction(postAction("create_database", parameters),
      DATABASE_TIMEOUT);
    return asObject(response.get("output"));
  }

  public void deleteDatabase(long bdbId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", bdbId);
    awaitAction(postAction("delete_database", parameters), DATABASE_TIMEOUT);
  }

  /** Fires {@code effect} via {@code trigger} on the database and waits for it to complete. */
  public Map<String, Object> triggerEffect(long bdbId, StandaloneEffect effect, String trigger) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", bdbId);
    parameters.put("cluster_index", 0);
    parameters.put("effect", effect.wireName());
    parameters.put("trigger", trigger);
    return awaitAction(postAction("topology_change_standalone", parameters), EFFECT_TIMEOUT);
  }

  // --- http/json plumbing ---

  private String request(String method, String path, String jsonBody) {
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
      conn.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
      conn.setReadTimeout(HTTP_TIMEOUT_MILLIS);
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

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asObject(Object value) {
    return value instanceof Map ? (Map<String, Object>) value : new HashMap<>();
  }
}
