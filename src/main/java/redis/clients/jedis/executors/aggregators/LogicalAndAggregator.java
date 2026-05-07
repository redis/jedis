package redis.clients.jedis.executors.aggregators;

/**
 * Aggregator that performs logical AND on added values. Supports Boolean, Long,
 * ArrayList&lt;Boolean&gt;, ArrayList&lt;Long&gt;.
 */
class LogicalAndAggregator<T> extends LogicalBinaryAggregator<T> {

  LogicalAndAggregator() {
    super("AGG_LOGICAL_AND");
  }

  @Override
  protected boolean applyBooleanOp(boolean a, boolean b) {
    return a && b;
  }
}
