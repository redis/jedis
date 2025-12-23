package redis.clients.jedis;

import java.util.HashMap;

public final class HostAndPorts {

  private static final HashMap<String, EndpointConfig> endpointConfigs;

  static {
    String endpointsPath = System.getenv().getOrDefault("REDIS_ENDPOINTS_CONFIG_PATH", "src/test/resources/endpoints.json");
    try {
      endpointConfigs = EndpointConfig.loadFromJSON(endpointsPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static EndpointConfig getRedisEndpoint(String endpointName) {
    if (!endpointConfigs.containsKey(endpointName)) {
      throw new IllegalArgumentException("Unknown Redis endpoint: " + endpointName);
    }

    return endpointConfigs.get(endpointName);
  }

  private HostAndPorts() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
