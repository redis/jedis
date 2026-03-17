package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.exceptions.UnsupportedAggregationException;

import java.util.Objects;

/**
 * Stateful aggregator that keeps aggregating replies of the same type according to the given
 * ResponsePolicy. The proper underlying aggregator is lazily created on the first non-null value.
 * If all added values are null, getResult() returns null.
 */
class ReplyAggregator<T> implements Aggregator<T, T> {

  private final CommandFlagsRegistry.ResponsePolicy policy;
  private Aggregator<T, T> delegate;

  public ReplyAggregator(CommandFlagsRegistry.ResponsePolicy policy) {
    this.policy = Objects.requireNonNull(policy, "policy cannot be null");
  }

  /**
   * Adds a new value to the aggregator. Null values are ignored.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void add(T newReply) {
    if (newReply == null) {
      return; // ignore nulls
    }

    // Lazy initialization of delegate based on policy and first non-null value
    if (delegate == null) {
      switch (policy) {
        case AGG_SUM:
          if (!(newReply instanceof Number)) {
            throw new UnsupportedAggregationException(
                "AGG_SUM policy requires numeric type, but got: " + newReply.getClass().getName());
          }
          delegate = (Aggregator<T, T>) new SumAggregator<>(); // safe cast
          break;
        case AGG_MIN:
          delegate = new MinAggregator<>();
          break;
        case AGG_MAX:
          delegate = new MaxAggregator<>();
          break;
        case AGG_LOGICAL_AND:
          delegate = new LogicalAndAggregator<>();
          break;
        case AGG_LOGICAL_OR:
          delegate = new LogicalOrAggregator<>();
          break;
        case DEFAULT:
          delegate = new DefaultPolicyAggregator<>();
          break;
        case ALL_SUCCEEDED:
          delegate = new FirstNonNullAggregator<>();
          break;
        case ONE_SUCCEEDED:
          delegate = new FirstNonNullAggregator<>();
          break;
        default:
          delegate = new FirstNonNullAggregator<>();
          break;
      }
    }

    // Delegate the addition
    delegate.add(newReply);
  }

  /**
   * Returns the aggregated result so far. Returns null if no meaningful value has been added.
   */
  @Override
  public T getResult() {
    return delegate == null ? null : delegate.getResult();
  }

}