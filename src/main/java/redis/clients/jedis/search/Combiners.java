package redis.clients.jedis.search;

import redis.clients.jedis.annots.Experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Factory class for creating {@link Combiner} instances.
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * // RRF with default parameters
 * Combiners.rrf()
 *
 * // RRF with custom window and constant
 * Combiners.rrf().window(10).constant(60)
 *
 * // Linear combination with weights
 * Combiners.linear().alpha(0.7).beta(0.3)
 *
 * // With score alias
 * Combiners.rrf().as("combined_score")
 * </pre>
 *
 * @see Combiner
 */
@Experimental
public final class Combiners {

  private Combiners() {
  }

  /**
   * Create an RRF (Reciprocal Rank Fusion) combiner.
   * @return a new RRF combiner
   */
  public static RRF rrf() {
    return new RRF();
  }

  /**
   * Create a Linear combination combiner.
   * @return a new Linear combiner
   */
  public static Linear linear() {
    return new Linear();
  }

  /**
   * RRF (Reciprocal Rank Fusion) combiner.
   */
  public static class RRF extends Combiner {
    private Integer window;
    private Double constant;

    RRF() {
      super("RRF");
    }

    /**
     * Set the WINDOW parameter for RRF.
     * @param window the window size
     * @return this RRF instance
     */
    public RRF window(int window) {
      this.window = window;
      return this;
    }

    /**
     * Set the CONSTANT parameter for RRF.
     * @param constant the constant value (typically 60)
     * @return this RRF instance
     */
    public RRF constant(double constant) {
      this.constant = constant;
      return this;
    }

    @Override
    protected List<Object> getOwnArgs() {
      if (window == null && constant == null) {
        return Collections.emptyList();
      }
      List<Object> args = new ArrayList<>();
      if (window != null) {
        args.add(WINDOW);
        args.add(window);
      }
      if (constant != null) {
        args.add(CONSTANT);
        args.add(constant);
      }
      return args;
    }
  }

  /**
   * Linear combination combiner.
   */
  public static class Linear extends Combiner {
    private Double alpha;
    private Double beta;
    private Integer window;

    Linear() {
      super("LINEAR");
    }

    /**
     * Set the ALPHA parameter (weight for text search score).
     * @param alpha the alpha value (0.0 to 1.0)
     * @return this Linear instance
     */
    public Linear alpha(double alpha) {
      this.alpha = alpha;
      return this;
    }

    /**
     * Set the BETA parameter (weight for vector similarity score).
     * @param beta the beta value (0.0 to 1.0)
     * @return this Linear instance
     */
    public Linear beta(double beta) {
      this.beta = beta;
      return this;
    }

    /**
     * Set the WINDOW parameter for LINEAR.
     * @param window the window size
     * @return this Linear instance
     */
    public Linear window(int window) {
      this.window = window;
      return this;
    }

    @Override
    protected List<Object> getOwnArgs() {
      if (alpha == null && beta == null && window == null) {
        return Collections.emptyList();
      }
      List<Object> args = new ArrayList<>();
      if (alpha != null) {
        args.add(ALPHA);
        args.add(alpha);
      }
      if (beta != null) {
        args.add(BETA);
        args.add(beta);
      }
      if (window != null) {
        args.add(WINDOW);
        args.add(window);
      }
      return args;
    }
  }
}
