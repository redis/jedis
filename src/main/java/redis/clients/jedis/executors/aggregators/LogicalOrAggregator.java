package redis.clients.jedis.executors.aggregators;

/**
 * Aggregator that performs logical OR on added values. Supports Boolean, Long,
 * ArrayList&lt;Boolean&gt;, ArrayList&lt;Long&gt;.
 */
class LogicalOrAggregator<T> extends LogicalBinaryAggregator<T> {

  LogicalOrAggregator() {
    super("AGG_LOGICAL_OR");
  }

  @Override
  protected boolean applyBooleanOp(boolean a, boolean b) {
    return a || b;
  }
}
