package redis.clients.jedis.tests.utils;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class ClientKillerUtil {
  public static void killClient(Jedis jedis, String clientName) {
    for (String clientInfo : jedis.clientList().split("\n")) {
      if (clientInfo.contains("name=" + clientName)) {
        // Ugly, but cmon, it's a test.
        String hostAndPortString  = clientInfo.split(" ")[1].split("=")[1];
        String[] hostAndPortParts = HostAndPort.extractParts(hostAndPortString);
        // It would be better if we kill the client by Id as it's safer but jedis doesn't implement
        // the command yet.
        jedis.clientKill(hostAndPortParts[0] + ":" + hostAndPortParts[1]);
      }
    }
  }

  public static void tagClient(Jedis j, String name) {
    j.clientSetname(name);
  }
}
