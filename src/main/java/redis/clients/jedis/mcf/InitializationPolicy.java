package redis.clients.jedis.mcf;

import redis.clients.jedis.annots.Experimental;

/**
 * Interface for initialization policies.
 * <p>
 * An initialization policy determines when a multi-database connection is ready to be returned
 * based on the availability of individual database connections.
 * </p>
 * <p>
 * The policy is evaluated based on the completion status of database connection health checks, and
 * the decision to continue waiting, succeed, or fail is based on the number of available, pending,
 * and failed connections.
 * </p>
 * @author Ali Takavci
 * @since 7.3
 */
@Experimental
public interface InitializationPolicy {

  enum Decision {
    CONTINUE, SUCCESS, FAIL
  }

  Decision evaluate(InitializationContext context);

  interface InitializationContext {

    int getAvailableConnections();

    int getFailedConnections();

    int getPendingConnections();

  }

  /**
   * Built-in initialization policies.
   * <p>
   * The policy is evaluated based on the completion status of database health checks, and the
   * decision to continue waiting, succeed, or fail is based on the number of available, pending,
   * and failed connections.
   * </p>
   * Built-in policies are:
   * <ul>
   * <li>{@link BuiltIn#ALL_AVAILABLE} - All databases need to be available</li>
   * <li>{@link BuiltIn#MAJORITY_AVAILABLE} - Majority of databases need to be available</li>
   * <li>{@link BuiltIn#ONE_AVAILABLE} - At least one database needs to be available</li>
   * </ul>
   */
  class BuiltIn {

    /**
     * Policy that requires all databases to be available before the connection is ready.
     */
    public static final InitializationPolicy ALL_AVAILABLE = new AllAvailablePolicy();

    /**
     * Policy that requires a majority of databases to be available before the connection is ready.
     */
    public static final InitializationPolicy MAJORITY_AVAILABLE = new MajorityAvailablePolicy();

    /**
     * Policy that requires at least one database to be available before the connection is ready.
     */
    public static final InitializationPolicy ONE_AVAILABLE = new OneAvailablePolicy();

    /*
     * All databases need to be available. The connection is ready only when all database health
     * checks have completed successfully. If any connection fails, the initialization fails. If all
     * connections are available, initialization succeeds. Otherwise, continue waiting.
     */
    private static class AllAvailablePolicy implements InitializationPolicy {

      @Override
      public Decision evaluate(InitializationContext ctx) {
        // Any failure means overall failure
        if (ctx.getFailedConnections() > 0) {
          return Decision.FAIL;
        }

        // All connections completed successfully
        if (ctx.getPendingConnections() == 0) {
          return Decision.SUCCESS;
        }

        return Decision.CONTINUE;
      }

    }

    /*
     * A majority of databases need to be available. The connection is ready when more than half of
     * the database connections are available. This means initialization can succeed early once
     * majority is reached, or fail early if majority becomes impossible.
     */
    private static class MajorityAvailablePolicy implements InitializationPolicy {

      @Override
      public Decision evaluate(InitializationContext ctx) {
        int total = ctx.getPendingConnections() + ctx.getAvailableConnections()
            + ctx.getFailedConnections();
        int required = (total / 2) + 1;

        // Early success - majority reached
        if (ctx.getAvailableConnections() >= required) {
          return Decision.SUCCESS;
        }

        // Early failure - impossible to reach majority
        int maxPossibleAvailable = ctx.getAvailableConnections() + ctx.getPendingConnections();
        if (maxPossibleAvailable < required) {
          return Decision.FAIL;
        }

        // Final evaluation - no more pending
        if (ctx.getPendingConnections() == 0) {
          return ctx.getAvailableConnections() >= required ? Decision.SUCCESS : Decision.FAIL;
        }

        return Decision.CONTINUE;
      }

    }

    /*
     * At least one database needs to be available. The connection is ready as soon as any database
     * connection is available. Initialization fails only if all connections fail.
     */
    private static class OneAvailablePolicy implements InitializationPolicy {

      @Override
      public Decision evaluate(InitializationContext ctx) {
        // Any success means overall success
        if (ctx.getAvailableConnections() > 0) {
          return Decision.SUCCESS;
        }

        // All connections completed with failures
        if (ctx.getPendingConnections() == 0) {
          return Decision.FAIL;
        }

        return Decision.CONTINUE;
      }

    }

  }

}
