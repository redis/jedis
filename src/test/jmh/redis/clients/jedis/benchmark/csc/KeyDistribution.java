package redis.clients.jedis.benchmark.csc;

import java.util.Random;

/**
 * Key-index sampling distributions used by the CSC workload benchmarks.
 * <p>
 * Mirrors the four samplers used by the {@code go-redis} CSC exploration ({@code CSC go-redis.md})
 * so the Jedis suite can produce apples-to-apples numbers per workload:
 * <ul>
 * <li>{@link #UNIFORM} — uniform random over {@code [0, n)}.</li>
 * <li>{@link #zipf(double)} — Zipfian with exponent {@code s} (default {@code s=1.1}).</li>
 * <li>{@link #normal()} — Normal {@code N(n/2, n/6)} clamped to {@code [0, n)}.</li>
 * <li>{@link #hot(int, double)} — fixed hot-set hit fraction for synthetic hot-key tests.</li>
 * </ul>
 * <p>
 * All samplers are stateless across calls except for the {@link Random} instance they receive,
 * which must be per-thread (see {@link Workload.Rng}).
 */
final class KeyDistribution {

  private KeyDistribution() {
  }

  interface IndexSampler {
    int next(Random r, int n);
  }

  static final IndexSampler UNIFORM = new IndexSampler() {
    @Override
    public int next(Random r, int n) {
      return r.nextInt(n);
    }
  };

  /**
   * Zipfian sampler using the {@code rejection-sampling} variant from Devroye's
   * {@code "Non-Uniform Random Variate Generation"} (1986), §10.4 (the same approach the
   * {@code rand/v2.Zipf} type in Go's standard library uses).
   * <p>
   * Returns indices in {@code [0, n)} biased toward {@code 0} — wrap with a permutation if the
   * working-set should be spread across the key space.
   */
  static IndexSampler zipf(final double s) {
    if (s <= 1.0) {
      throw new IllegalArgumentException("Zipf exponent must be > 1.0, got " + s);
    }
    return new IndexSampler() {
      @Override
      public int next(Random r, int n) {
        double sm1 = s - 1.0;
        double b = Math.pow(2.0, sm1);
        while (true) {
          double u = r.nextDouble();
          double v = r.nextDouble();
          double x = Math.floor(Math.pow(u, -1.0 / sm1));
          if (x < 1.0 || x > n) continue;
          double t = Math.pow(1.0 + 1.0 / x, sm1);
          if (v * x * (t - 1.0) / (b - 1.0) <= t / b) {
            return (int) (x - 1.0);
          }
        }
      }
    };
  }

  /**
   * Normal {@code N(n/2, n/6)} clamped to {@code [0, n)}. Produces a contiguous middle-band hot
   * zone — mirrors the {@code NormalDistribution} workload in the go-redis suite.
   */
  static IndexSampler normal() {
    return new IndexSampler() {
      @Override
      public int next(Random r, int n) {
        double mean = n / 2.0;
        double sigma = n / 6.0;
        double g = r.nextGaussian() * sigma + mean;
        int idx = (int) g;
        if (idx < 0) return 0;
        if (idx >= n) return n - 1;
        return idx;
      }
    };
  }

  /**
   * Mixed sampler that picks from the first {@code hotCount} keys with probability {@code hotProb}
   * and from the rest uniformly otherwise.
   */
  static IndexSampler hot(final int hotCount, final double hotProb) {
    return new IndexSampler() {
      @Override
      public int next(Random r, int n) {
        if (r.nextDouble() < hotProb) return r.nextInt(Math.min(hotCount, n));
        return r.nextInt(n);
      }
    };
  }
}
