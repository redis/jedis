package redis.clients.jedis.resps;

import java.util.Map;

/**
 * This class holds information about a node of the cluster with command {@code CLUSTER SHARDS}.
 * They can be accessed via getters. There is also {@link ClusterShardNodeInfo#getClusterShardNodeInfo()}
 * method that returns a generic {@link Map} in case more info are returned from the server.
 */
public class ClusterShardNodeInfo {

  public static final String ID = "id";
  public static final String ENDPOINT = "endpoint";
  public static final String IP = "ip";
  public static final String HOSTNAME = "hostname";
  public static final String PORT = "port";
  public static final String TLS_PORT = "tls-port";
  public static final String ROLE = "role";
  public static final String REPLICATION_OFFSET = "replication-offset";
  public static final String HEALTH = "health";

  private final String id;
  private final String endpoint;
  private final String ip;
  private final String hostname;
  private final Long port;
  private final Long tlsPort;
  private final String role;
  private final Long replicationOffset;
  private final String health;

  private final Map<String, Object> clusterShardNodeInfo;

  /**
   * @param map contains key-value pairs with node info
   */
  public ClusterShardNodeInfo(Map<String, Object> map) {
    id = (String) map.get(ID);
    endpoint = (String) map.get(ENDPOINT);
    ip = (String) map.get(IP);
    hostname = (String) map.get(HOSTNAME);
    port = (Long) map.get(PORT);
    tlsPort = (Long) map.get(TLS_PORT);
    role = (String) map.get(ROLE);
    replicationOffset = (Long) map.get(REPLICATION_OFFSET);
    health = (String) map.get(HEALTH);

    clusterShardNodeInfo = map;
  }

  public String getId() {
    return id;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getIp() {
    return ip;
  }

  public String getHostname() {
    return hostname;
  }

  public Long getPort() {
    return port;
  }

  public Long getTlsPort() {
    return tlsPort;
  }

  public String getRole() {
    return role;
  }

  public Long getReplicationOffset() {
    return replicationOffset;
  }

  public String getHealth() {
    return health;
  }

  public Map<String, Object> getClusterShardNodeInfo() {
    return clusterShardNodeInfo;
  }

  public boolean isSsl() {
    return tlsPort != null;
  }
}
