package redis.clients.jedis;

import redis.clients.jedis.providers.ShardedConnectionProvider;
import java.util.regex.Pattern;

public class ShardedPipeline extends MultiNodePipelineBase {

  private final ShardedConnectionProvider provider;

  public ShardedPipeline(ShardedConnectionProvider provider) {
    super(new ShardedCommandObjects(provider.getHashingAlgo()));
    this.provider = provider;
  }

  public ShardedPipeline(ShardedConnectionProvider provider, Pattern tagPattern) {
    super(new ShardedCommandObjects(provider.getHashingAlgo(), tagPattern));
    this.provider = provider;
  }

  @Override
  protected HostAndPort getNodeKey(CommandArguments args) {
    return provider.getNode(((ShardedCommandArguments) args).getKeyHash());
  }

  @Override
  protected Connection getConnection(HostAndPort nodeKey) {
    return provider.getConnection(nodeKey);
  }
}
