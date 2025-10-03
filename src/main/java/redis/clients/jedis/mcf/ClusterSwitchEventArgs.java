package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.mcf.MultiClusterPooledConnectionProvider.Cluster;

public class ClusterSwitchEventArgs {

  private final SwitchReason reason;
  private final String ClusterName;
  private final Endpoint Endpoint;

  public ClusterSwitchEventArgs(SwitchReason reason, Endpoint endpoint, Cluster cluster) {
    this.reason = reason;
    // TODO: @ggivo do we need cluster name?
    this.ClusterName = cluster.getCircuitBreaker().getName();
    this.Endpoint = endpoint;
  }

  public SwitchReason getReason() {
    return reason;
  }

  public String getClusterName() {
    return ClusterName;
  }

  public Endpoint getEndpoint() {
    return Endpoint;
  }

}
