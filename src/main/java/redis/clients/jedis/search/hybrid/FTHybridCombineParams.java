package redis.clients.jedis.search.hybrid;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.SearchProtocol;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

/**
 * Arguments for the COMBINE clause in FT.HYBRID command. Specifies how to combine text search and
 * vector similarity scores.
 */
@Experimental
public class FTHybridCombineParams implements IParams {

  private final CombineMethod method;
  private String scoreAlias;

  private FTHybridCombineParams(CombineMethod method) {
    this.method = method;
  }

  /**
   * Create CombineParams with the specified method.
   * @param method the combine method (RRF or Linear)
   * @return a new CombineParams instance
   */
  public static FTHybridCombineParams of(CombineMethod method) {
    if (method == null) {
      throw new IllegalArgumentException("Combine method must not be null");
    }
    return new FTHybridCombineParams(method);
  }

  /**
   * Set an alias for the combined score field using YIELD_SCORE_AS.
   * @param alias the field name to use for the combined score
   * @return this instance
   */
  public FTHybridCombineParams as(String alias) {
    if (alias == null) {
      throw new IllegalArgumentException("Alias must not be null");
    }
    this.scoreAlias = alias;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    method.addParams(args);

    // YIELD_SCORE_AS for COMBINE
    if (scoreAlias != null) {
      args.add(YIELD_SCORE_AS);
      args.add(scoreAlias);
    }
  }

  /**
   * Base interface for combine methods.
   */
  public interface CombineMethod extends IParams {
  }

  /**
   * RRF (Reciprocal Rank Fusion) combine method.
   */
  public static class RRF implements CombineMethod {
    private Integer window;
    private Double constant;

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
    public void addParams(CommandArguments args) {
      args.add(SearchProtocol.SearchKeyword.RRF);

      // Count parameters
      int paramCount = 0;
      if (window != null) paramCount += 2;
      if (constant != null) paramCount += 2;

      args.add(paramCount);

      if (window != null) {
        args.add(WINDOW);
        args.add(window);
      }
      if (constant != null) {
        args.add(CONSTANT);
        args.add(constant);
      }
    }
  }

  /**
   * Linear combination method.
   */
  public static class Linear implements CombineMethod {
    private Double alpha;
    private Double beta;
    private Integer window;

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
     * @return this LINEAR instance
     */
    public Linear window(int window) {
      this.window = window;
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(LINEAR);

      // Count parameters
      int paramCount = 0;
      if (alpha != null) paramCount += 2;
      if (beta != null) paramCount += 2;
      if (window != null) paramCount += 2;

      args.add(paramCount);

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
    }
  }
}
