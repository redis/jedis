package redis.clients.jedis;

import java.util.function.Function;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.JedisRoundRobinBase;

public class ScanRoundRobin extends JedisRoundRobinBase<ScanResult<String>> {

  private final int count;
  private final Function<String, CommandArguments> args;

  public ScanRoundRobin(ConnectionProvider connectionProvider, int batchCount, String match) {
    super(connectionProvider, BuilderFactory.SCAN_RESPONSE);
    this.count = batchCount;
    this.args = (cursor) -> new CommandArguments(Protocol.Command.SCAN).add(cursor)
        .add(Keyword.MATCH).add(match).add(Keyword.COUNT).add(count);
  }

  public ScanRoundRobin(ConnectionProvider connectionProvider, int batchCount, String match, String type) {
    super(connectionProvider, BuilderFactory.SCAN_RESPONSE);
    this.count = batchCount;
    this.args = (cursor) -> new CommandArguments(Protocol.Command.SCAN).add(cursor)
        .add(Keyword.MATCH).add(match).add(Keyword.COUNT).add(count).add(Keyword.TYPE).add(type);
  }

  @Override
  protected boolean isIterationCompleted(ScanResult<String> reply) {
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
}
