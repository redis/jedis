package redis.clients.jedis.scenario;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.EndpointConfig;

public class RestEndpointUtil {

  public static Endpoint getRestAPIEndpoint(EndpointConfig config) {
    return new Endpoint() {
      @Override
      public String getHost() {
        // convert this to Redis FQDN by removing the node prefix
        // "dns":"redis-10232.c1.taki-active-active-test-c114170a.cto.redislabs.com"
        String host = config.getHost();
        // trim until the first dot
        return host.substring(host.indexOf('.') + 1);
      }

      @Override
      public int getPort() {
        // default port for REST API
        return 9443;
      }
    };
  }
}
