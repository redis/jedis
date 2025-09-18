package redis.clients.jedis.mcf;

public interface ProbePolicy {

  public enum Decision {
    CONTINUE, SUCCESS, FAIL
  }

  Decision evaluate(ProbeContext data);

  public static interface ProbeContext {

    public int getRemainingProbes();

    public int getSuccesses();

    public int getFails();

  }

  public static class BuiltIn {
    public static final ProbePolicy ALL_SUCCESS = new AllSuccessPolicy();
    public static final ProbePolicy ANY_SUCCESS = new AnySuccessPolicy();
    public static final ProbePolicy MAJORITY_SUCCESS = new MajoritySuccessPolicy();

    /*
     * All probes need to be healthy. If a database doesn’t pass the health check for numProbes
     * times, then the check wasn’t successful. This means you can stop probing after you got the
     * first failed health check (e.g., timeout or unhealthy status)
     */
    private static class AllSuccessPolicy implements ProbePolicy {
      @Override
      public Decision evaluate(ProbeContext ctx) {
        // Any failure means overall failure
        if (ctx.getFails() > 0) {
          return Decision.FAIL;
        }

        // All probes completed successfully
        if (ctx.getRemainingProbes() == 0) {
          return Decision.SUCCESS;
        }

        return Decision.CONTINUE;
      }
    }

    /*
     * A database is healthy if at least one probe returned a healthy status. You can stop probing
     * as soon as you got the first healthy status.
     */
    private static class AnySuccessPolicy implements ProbePolicy {
      @Override
      public Decision evaluate(ProbeContext ctx) {
        // Any success means overall success
        if (ctx.getSuccesses() > 0) {
          return Decision.SUCCESS;
        }

        // All probes completed with failures
        if (ctx.getRemainingProbes() == 0) {
          return Decision.FAIL;
        }

        return Decision.CONTINUE;
      }
    }

    /*
     * A database is healthy if the majority of probes returned ‘healthy’. This means you can stop
     * probing as soon as the majority can’t be guaranteed any more (e.g., you have 4 probes and 2
     * of them failed), or as soon as the majority is reached (e.g., 3 out of 4 were healthy)
     */
    private static class MajoritySuccessPolicy implements ProbePolicy {
      @Override
      public Decision evaluate(ProbeContext ctx) {
        int total = ctx.getRemainingProbes() + ctx.getSuccesses() + ctx.getFails();
        int required = (total / 2) + 1;

        // Early success
        if (ctx.getSuccesses() >= required) {
          return Decision.SUCCESS;
        }

        // Early failure - impossible to reach majority
        int maxPossibleSuccesses = ctx.getSuccesses() + ctx.getRemainingProbes();
        if (maxPossibleSuccesses < required) {
          return Decision.FAIL;
        }

        // Final evaluation
        if (ctx.getRemainingProbes() == 0) {
          return ctx.getSuccesses() >= required ? Decision.SUCCESS : Decision.FAIL;
        }

        return Decision.CONTINUE;
      }
    }
  }
}
