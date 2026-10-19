package redis.clients.jedis;

import java.util.Collection;
import java.util.function.Function;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.JedisCommandIterationBase;

/**
 * Iterates the keys carrying a {@code BLESS} flag on every node of the connection provider, with an
 * independent {@code BLESS SCAN} cursor loop per node. In cluster mode this includes replicas, so a
 * key may be returned more than once.
 * @since 8.1
 */
public class BlessScanIteration extends JedisCommandIterationBase<ScanResult<String>, String> {

  private final Function<String, CommandArguments> args;

  public BlessScanIteration(ConnectionProvider connectionProvider, int batchCount, BlessFlag flag) {
    super(connectionProvider, BuilderFactory.SCAN_RESPONSE);
    this.args = (cursor) -> new CommandArguments(Command.BLESS).add(Command.SCAN).add(cursor)
        .add(flag).add(Keyword.COUNT).add(batchCount);
  }

  @Override
  protected boolean isNodeCompleted(ScanResult<String> reply) {
    return reply.isCompleteIteration();
  }

  @Override
  protected CommandArguments initCommandArguments() {
    return args.apply(ScanParams.SCAN_POINTER_START);
  }

  @Override
  protected CommandArguments nextCommandArguments(ScanResult<String> lastReply) {
    return args.apply(lastReply.getCursor());
  }

  @Override
  protected Collection<String> convertBatchToData(ScanResult<String> batch) {
    return batch.getResult();
  }
}
