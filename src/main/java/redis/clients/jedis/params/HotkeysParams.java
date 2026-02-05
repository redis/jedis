package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.HotkeysMetric;

import java.util.Arrays;
import java.util.Objects;

/**
 * Parameters for the HOTKEYS START command.
 */
public class HotkeysParams implements IParams {

  private HotkeysMetric[] metrics;
  private Integer count;
  private Integer duration;
  private Integer sample;
  private int[] slots;

  public HotkeysParams() {
  }

  public static HotkeysParams hotkeysParams() {
    return new HotkeysParams();
  }

  /**
   * Specifies which metrics to track. At least one metric is required.
   * @param metrics the metrics to track (CPU, NET)
   * @return this
   */
  public HotkeysParams metrics(HotkeysMetric... metrics) {
    if (metrics == null || metrics.length == 0) {
      throw new IllegalArgumentException("at least one metric is required");
    }
    this.metrics = metrics;
    return this;
  }

  /**
   * Maximum number of hot keys to track.
   * @param count must be between 10 and 64
   * @return this
   * @throws IllegalArgumentException if count is not between 10 and 64
   */
  public HotkeysParams count(int count) {
    if (count < 10 || count > 64) {
      throw new IllegalArgumentException("count must be between 10 and 64");
    }
    this.count = count;
    return this;
  }

  /**
   * Auto-stop tracking after the specified number of seconds.
   * @param duration 0 means no auto-stop
   * @return this
   * @throws IllegalArgumentException if duration is negative
   */
  public HotkeysParams duration(int duration) {
    if (duration < 0) {
      throw new IllegalArgumentException("duration must be >= 0");
    }
    this.duration = duration;
    return this;
  }

  /**
   * Sample 1 in N commands.
   * @param sample 1 means all commands are sampled
   * @return this
   * @throws IllegalArgumentException if sample is less than 1
   */
  public HotkeysParams sample(int sample) {
    if (sample < 1) {
      throw new IllegalArgumentException("sample must be >= 1");
    }
    this.sample = sample;
    return this;
  }

  /**
   * Filter by hash slots (cluster mode only).
   * @param slots the hash slots to filter (0-16383)
   * @return this
   * @throws IllegalArgumentException if any slot is not between 0 and 16383
   */
  public HotkeysParams slots(int... slots) {
    if (slots != null) {
      for (int slot : slots) {
        if (slot < 0 || slot > 16383) {
          throw new IllegalArgumentException("each slot must be between 0 and 16383");
        }
      }
    }
    this.slots = slots;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(Keyword.METRICS);
    args.add(metrics.length);
    for (HotkeysMetric metric : metrics) {
      args.add(metric);
    }

    if (count != null) {
      args.add(Keyword.COUNT);
      args.add(count);
    }

    if (duration != null) {
      args.add(Keyword.DURATION);
      args.add(duration);
    }

    if (sample != null) {
      args.add(Keyword.SAMPLE);
      args.add(sample);
    }

    if (slots != null && slots.length > 0) {
      args.add(Keyword.SLOTS);
      args.add(slots.length);
      for (int slot : slots) {
        args.add(slot);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HotkeysParams that = (HotkeysParams) o;
    return Arrays.equals(metrics, that.metrics) && Objects.equals(count, that.count)
        && Objects.equals(duration, that.duration) && Objects.equals(sample, that.sample)
        && Arrays.equals(slots, that.slots);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(count, duration, sample);
    result = 31 * result + Arrays.hashCode(metrics);
    result = 31 * result + Arrays.hashCode(slots);
    return result;
  }
}
