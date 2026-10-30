package redis.clients.jedis;

import java.util.Collection;

import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.JedisCommandIterationBase;

/**
 * Full {@code BLESS SCAN} iteration over every node known to the connection provider, each with its
 * own cursor. This is the cluster-safe way to enumerate blessed keys, since {@code BLESS SCAN} only
 * covers the node that receives it.
 * @since 8.1
 */
public class BlessScanIteration extends JedisCommandIterationBase<ScanResult<String>, String> {

  private final BlessFlag flag;
  private final int count;

  public BlessScanIteration(ConnectionProvider connectionProvider, int batchCount, BlessFlag flag) {
    super(connectionProvider, BuilderFactory.SCAN_RESPONSE);
    this.flag = flag;
    this.count = batchCount;
  }

  private CommandArguments args(String cursor) {
    return new CommandArguments(Protocol.Command.BLESS).add(Keyword.SCAN).add(cursor).add(flag)
        .add(Keyword.COUNT).add(count);
  }

  @Override
  protected boolean isNodeCompleted(ScanResult<String> reply) {
    return reply.isCompleteIteration();
  }

  @Override
  protected CommandArguments initCommandArguments() {
    return args(ScanParams.SCAN_POINTER_START);
  }

  @Override
  protected CommandArguments nextCommandArguments(ScanResult<String> lastReply) {
    return args(lastReply.getCursor());
  }

  @Override
  protected Collection<String> convertBatchToData(ScanResult<String> batch) {
    return batch.getResult();
  }
}
