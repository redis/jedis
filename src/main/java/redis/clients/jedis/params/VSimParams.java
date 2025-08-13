package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.Experimental;

/**
 * Parameters for the VSIM command.
 */
@Experimental
public class VSimParams implements IParams {

  private Integer count;
  private Double epsilon;
  private Integer ef;
  private String filter;
  private Integer filterEf;
  private boolean truth;
  private boolean noThread;

  public VSimParams() {
  }

  /**
   * Limits the number of returned results.
   * @param num the maximum number of results to return
   * @return VSimParams
   */
  public VSimParams count(int num) {
    this.count = num;
    return this;
  }

  /**
   * Sets the epsilon (delta) parameter for distance-based filtering. Only elements with a
   * similarity score of (1 - epsilon) or better are returned. For example, epsilon=0.2 means only
   * elements with similarity >= 0.8 are returned.
   * @param delta a floating point number between 0 and 1
   * @return VSimParams
   */
  public VSimParams epsilon(double delta) {
    this.epsilon = delta;
    return this;
  }

  /**
   * Controls the search effort. Higher values explore more nodes, improving recall at the cost of
   * speed. Typical values range from 50 to 1000.
   * @param searchExplorationFactor the exploration factor
   * @return VSimParams
   */
  public VSimParams ef(int searchExplorationFactor) {
    this.ef = searchExplorationFactor;
    return this;
  }

  /**
   * Applies a filter expression to restrict matching elements.
   * @param expression the filter expression
   * @return VSimParams
   */
  public VSimParams filter(String expression) {
    this.filter = expression;
    return this;
  }

  /**
   * Limits the number of filtering attempts for the FILTER expression.
   * @param maxFilteringEffort the maximum filtering effort
   * @return VSimParams
   */
  public VSimParams filterEf(int maxFilteringEffort) {
    this.filterEf = maxFilteringEffort;
    return this;
  }

  /**
   * Forces an exact linear scan of all elements, bypassing the HNSW graph. Use for benchmarking or
   * to calculate recall. This is significantly slower (O(N)).
   * @return VSimParams
   */
  public VSimParams truth() {
    this.truth = true;
    return this;
  }

  /**
   * Executes the search in the main thread instead of a background thread. Useful for small vector
   * sets or benchmarks. This may block the server during execution.
   * @return VSimParams
   */
  public VSimParams noThread() {
    this.noThread = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (count != null) {
      args.add(Protocol.Keyword.COUNT).add(count);
    }

    if (epsilon != null) {
      args.add(Protocol.Keyword.EPSILON).add(epsilon);
    }

    if (ef != null) {
      args.add(Protocol.Keyword.EF).add(ef);
    }

    if (filter != null) {
      args.add(Protocol.Keyword.FILTER).add(filter);
    }

    if (filterEf != null) {
      args.add(Protocol.Keyword.FILTER_EF).add(filterEf);
    }

    if (truth) {
      args.add(Protocol.Keyword.TRUTH);
    }

    if (noThread) {
      args.add(Protocol.Keyword.NOTHREAD);
    }
  }
}
