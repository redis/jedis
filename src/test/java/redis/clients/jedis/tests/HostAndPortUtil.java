package redis.clients.jedis.tests;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.List;

public class HostAndPortUtil {
  private static List<HostAndPort> redisHostAndPortList = new ArrayList<HostAndPort>();
  private static List<HostAndPort> sentinelHostAndPortList = new ArrayList<HostAndPort>();
  private static List<HostAndPort> clusterHostAndPortList = new ArrayList<HostAndPort>();

  static {
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT));
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT + 1));
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT + 2));
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT + 3));
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT + 4));
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT + 5));
    redisHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_PORT + 6));

    sentinelHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_SENTINEL_PORT));
    sentinelHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_SENTINEL_PORT + 1));
    sentinelHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_SENTINEL_PORT + 2));
    sentinelHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, Protocol.DEFAULT_SENTINEL_PORT + 3));

    clusterHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, 7379));
    clusterHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, 7380));
    clusterHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, 7381));
    clusterHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, 7382));
    clusterHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, 7383));
    clusterHostAndPortList.add(new HostAndPort(HostAndPort.LOCALHOST_STR, 7384));

    String envRedisHosts = System.getProperty("redis-hosts");
    String envSentinelHosts = System.getProperty("sentinel-hosts");
    String envClusterHosts = System.getProperty("cluster-hosts");

    redisHostAndPortList = parseHosts(envRedisHosts, redisHostAndPortList);
    sentinelHostAndPortList = parseHosts(envSentinelHosts, sentinelHostAndPortList);
    clusterHostAndPortList = parseHosts(envClusterHosts, clusterHostAndPortList);
  }

  public static List<HostAndPort> parseHosts(String envHosts,
      List<HostAndPort> existingHostsAndPorts) {

    if (null != envHosts && 0 < envHosts.length()) {

      String[] hostDefs = envHosts.split(",");

      if (null != hostDefs && 2 <= hostDefs.length) {

        List<HostAndPort> envHostsAndPorts = new ArrayList<HostAndPort>(hostDefs.length);

        for (String hostDef : hostDefs) {

          String[] hostAndPort = hostDef.split(":");

          if (null != hostAndPort && 2 == hostAndPort.length) {
            String host = hostAndPort[0];
            int port = Protocol.DEFAULT_PORT;

            try {
              port = Integer.parseInt(hostAndPort[1]);
            } catch (final NumberFormatException nfe) {
            }

            envHostsAndPorts.add(new HostAndPort(host, port));
          }
        }

        return envHostsAndPorts;
      }
    }

    return existingHostsAndPorts;
  }

  public static List<HostAndPort> getRedisServers() {
    return redisHostAndPortList;
  }

  public static List<HostAndPort> getSentinelServers() {
    return sentinelHostAndPortList;
  }

  public static List<HostAndPort> getClusterServers() {
    return clusterHostAndPortList;
  }
}
