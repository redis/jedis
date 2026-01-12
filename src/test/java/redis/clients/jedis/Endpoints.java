package redis.clients.jedis;

import org.opentest4j.TestAbortedException;
import redis.clients.jedis.util.TestEnvUtil;

import java.util.HashMap;

public final class Endpoints {

  private static final HashMap<String, EndpointConfig> endpointConfigs;

  static {
    try {
      endpointConfigs = EndpointConfig.loadFromJSON(TestEnvUtil.getEndpointsConfigPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static EndpointConfig getRedisEndpoint(String endpointName) {
    if (!endpointConfigs.containsKey(endpointName)) {
      throw new TestAbortedException("Unavailable Redis endpoint: " + endpointName);
    }

    return endpointConfigs.get(endpointName);
  }

  private Endpoints() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
