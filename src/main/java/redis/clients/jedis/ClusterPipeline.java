package redis.clients.jedis;

import redis.clients.jedis.providers.ClusterConnectionProvider;

public class ClusterPipeline extends MultiNodePipelineBase {

  private final ClusterConnectionProvider provider;

  public ClusterPipeline(ClusterConnectionProvider provider) {
    super(new ClusterCommandObjects());
    this.provider = provider;
  }

  @Override
  protected HostAndPort getNodeKey(CommandArguments args) {
    return provider.getNode(((ClusterCommandArguments) args).getCommandHashSlot());
  }

  @Override
  protected Connection getConnection(HostAndPort nodeKey) {
    return provider.getConnection(nodeKey);
  }
}
