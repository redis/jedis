package redis.clients.jedis.scenario;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.annotations.Expose;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.fluent.Request;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaultInjectionClient {

  private static final String BASE_URL;

  static final int CONNECTION_REQUEST_TIMEOUT = 3000;
  static final int RESPONSE_TIMEOUT = 3000;

  /**
   * Response timeout for rladmin calls. `rladmin status` and a long-running
   * `execute_rladmin_command` both take far longer than the 3s default used elsewhere.
   */
  static final int LONG_RESPONSE_TIMEOUT = 60000;

  static {
    BASE_URL = System.getenv().getOrDefault("FAULT_INJECTION_API_URL", "http://127.0.0.1:20324");
  }

  private static final Logger log = LoggerFactory.getLogger(FaultInjectionClient.class);

  public static class TriggerActionResponse {
    @Expose
    private final String actionId;

    private Instant lastRequestTime = null;

    private Instant completedAt = null;

    private Instant firstRequestAt = null;

    public TriggerActionResponse(String actionId) {
      this.actionId = actionId;
    }

    public String getActionId() {
      return actionId;
    }

    public boolean isCompleted(Duration checkInterval, Duration delayAfter, Duration timeout) {
      if (completedAt != null) {
        return Duration.between(completedAt, Instant.now()).compareTo(delayAfter) >= 0;
      }

      if (firstRequestAt != null
          && Duration.between(firstRequestAt, Instant.now()).compareTo(timeout) >= 0) {
        throw new RuntimeException("Timeout");
      }

      if (lastRequestTime == null
          || Duration.between(lastRequestTime, Instant.now()).compareTo(checkInterval) >= 0) {
        lastRequestTime = Instant.now();

        if (firstRequestAt == null) {
          firstRequestAt = lastRequestTime;
        }

        CloseableHttpClient httpClient = getHttpClient();

        Request request = Request.get(BASE_URL + "/action/" + actionId);

        try {
          Response response = request.execute(httpClient);
          String result = response.returnContent().asString();

          log.info("Action status: {}", result);

          if (result.contains("success")) {
            completedAt = Instant.now();
            return Duration.between(completedAt, Instant.now()).compareTo(delayAfter) >= 0;
          }

        } catch (IOException e) {
          throw new RuntimeException("Fault injection proxy error ", e);
        }
      }
      return false;
    }
  }

  private static CloseableHttpClient getHttpClient() {
    return getHttpClient(RESPONSE_TIMEOUT);
  }

  private static CloseableHttpClient getHttpClient(int responseTimeoutMillis) {
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
        .setResponseTimeout(responseTimeoutMillis, TimeUnit.MILLISECONDS).build();

    return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
  }

  public TriggerActionResponse triggerAction(String actionType, HashMap<String, Object> parameters)
      throws IOException {
    Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .excludeFieldsWithoutExposeAnnotation().create();

    HashMap<String, Object> payload = new HashMap<>();
    payload.put("type", actionType);
    payload.put("parameters", parameters);

    String jsonString = gson.toJson(payload);

    CloseableHttpClient httpClient = getHttpClient();
    Request request = Request.post(BASE_URL + "/action");
    request.bodyString(jsonString, ContentType.APPLICATION_JSON);

    try {
      String result = request.execute(httpClient).returnContent().asString();
      return gson.fromJson(result, new TypeToken<TriggerActionResponse>() {
      }.getType());
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * An action's status with the fields {@link TriggerActionResponse#isCompleted} discards.
   * <p>
   * Parsed as a {@link JsonObject} rather than a typed model because {@code output} is sometimes a
   * string, sometimes an object carrying a nested {@code output} member, and sometimes JSON null.
   */
  public static final class ActionStatus {

    private final String status;

    private final String error;

    private final String output;

    ActionStatus(String status, String error, String output) {
      this.status = status;
      this.error = error;
      this.output = output;
    }

    public String getStatus() {
      return status;
    }

    public String getError() {
      return error;
    }

    public String getOutput() {
      return output;
    }

    public boolean isSuccess() {
      return "success".equals(status) || "completed".equals(status);
    }

    public boolean isFailure() {
      return "failed".equals(status) || error != null;
    }

    @Override
    public String toString() {
      return "ActionStatus{status=" + status + ", error=" + error + "}";
    }
  }

  private static String optionalString(JsonObject object, String member) {
    if (!object.has(member)) {
      return null;
    }
    JsonElement value = object.get(member);
    // `execute_rladmin_command` reports its captured text under a nested object rather than as a
    // plain member, so a
    // non-primitive here is not the string being asked for - the caller looks one level deeper.
    return value.isJsonPrimitive() ? value.getAsString() : null;
  }

  public ActionStatus getActionStatus(String actionId) throws IOException {
    CloseableHttpClient httpClient = getHttpClient(LONG_RESPONSE_TIMEOUT);
    Request request = Request.get(BASE_URL + "/action/" + actionId);

    String body = request.execute(httpClient).returnContent().asString();
    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

    String output = optionalString(json, "output");
    if (output == null && json.has("output") && json.get("output").isJsonObject()) {
      output = optionalString(json.getAsJsonObject("output"), "output");
    }

    return new ActionStatus(optionalString(json, "status"), optionalString(json, "error"), output);
  }

  /**
   * Trigger an rladmin command and return immediately, without waiting for it to finish.
   * <p>
   * Required for anything measuring how quickly a client reacts to a Redis Enterprise state-machine
   * event: the {@code endpoint_rebind_propagation_grace_time} wait happens <em>inside</em> the
   * action, so blocking on completion would only start the clock once the window had closed.
   */
  public TriggerActionResponse triggerRladminCommand(String bdbId, String rladminCommand)
      throws IOException {
    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", bdbId);
    parameters.put("rladmin_command", rladminCommand);

    log.info("Starting rladmin {} on bdb {}", rladminCommand, bdbId);
    return triggerAction("execute_rladmin_command", parameters);
  }

  /**
   * Poll an action to completion, distinguishing a reported failure from "not finished yet".
   * @return {@code true} on success, {@code false} when the action reported a failure.
   * @throws RuntimeException on timeout.
   */
  public boolean awaitAction(String actionId, Duration checkInterval, Duration timeout) {
    Instant deadline = Instant.now().plus(timeout);

    while (Instant.now().isBefore(deadline)) {
      ActionStatus status;
      try {
        status = getActionStatus(actionId);
      } catch (IOException e) {
        throw new RuntimeException("Fault injection proxy error", e);
      }

      if (status.isSuccess()) {
        return true;
      }
      if (status.isFailure()) {
        log.warn("Action {} failed: {}", actionId, status);
        return false;
      }

      try {
        Thread.sleep(checkInterval.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for action " + actionId, e);
      }
    }

    throw new RuntimeException("Timeout waiting for action " + actionId);
  }

  /**
   * Run an rladmin command to completion and return its captured output.
   * @throws IllegalStateException when the action reported a failure - note rladmin exits non-zero
   *           for a no-op, so callers doing cleanup should tolerate this.
   */
  public String executeRladminCommandCapturingOutput(String bdbId, String rladminCommand,
      Duration checkInterval, Duration timeout) throws IOException {
    TriggerActionResponse response = triggerRladminCommand(bdbId, rladminCommand);

    if (!awaitAction(response.getActionId(), checkInterval, timeout)) {
      ActionStatus status = getActionStatus(response.getActionId());
      throw new IllegalStateException("rladmin " + rladminCommand + " failed: " + status);
    }

    return getActionStatus(response.getActionId()).getOutput();
  }

  /**
   * Run an rladmin command to completion, discarding its output.
   */
  public boolean executeRladminCommand(String bdbId, String rladminCommand, Duration checkInterval,
      Duration timeout) throws IOException {
    executeRladminCommandCapturingOutput(bdbId, rladminCommand, checkInterval, timeout);
    return true;
  }
}
