package redis.clients.jedis.scenario;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.fluent.Request;
import com.google.gson.Gson;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaultInjectionClient {

  private static final String BASE_URL;

  static {
    BASE_URL = System.getenv().getOrDefault("FAULT_INJECTION_API_URL", "http://127.0.0.1:20324");
  }

  private static final Logger log = LoggerFactory.getLogger(FaultInjectionClient.class);

  public static class TriggerActionResponse {
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

      if (firstRequestAt != null && Duration.between(firstRequestAt, Instant.now())
          .compareTo(timeout) >= 0) {
        throw new RuntimeException("Timeout");
      }

      if (lastRequestTime == null || Duration.between(lastRequestTime, Instant.now())
          .compareTo(checkInterval) >= 0) {
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
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(5000, TimeUnit.MILLISECONDS)
        .setResponseTimeout(5000, TimeUnit.MILLISECONDS).build();

    return HttpClientBuilder.create()
        .setDefaultRequestConfig(requestConfig).build();
  }

  public TriggerActionResponse triggerAction(String actionType, HashMap<String, Object> parameters)
      throws IOException {
    Gson gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

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

}
