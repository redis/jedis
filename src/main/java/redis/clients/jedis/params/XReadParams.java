package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class XReadParams implements IParams {

  private Integer count = null;
  private Integer maxCount = null;
  private Long maxSize = null;
  private Integer block = null;

  public static XReadParams xReadParams() {
    return new XReadParams();
  }

  public XReadParams count(int count) {
    this.count = count;
    return this;
  }

  /**
   * Limit the total number of entries returned across all streams combined. Unlike {@code COUNT},
   * which is a per-stream limit, {@code MAXCOUNT} caps the whole command reply; once the cap is
   * reached, remaining streams are skipped. Defaults to unlimited when unset. Must be a positive
   * integer and, when {@code COUNT} is also set and positive, greater than or equal to it
   * (validated by the server).
   * @param maxCount cumulative entry cap across all streams
   * @return this
   * @since 8.0
   */
  public XReadParams maxCount(int maxCount) {
    this.maxCount = maxCount;
    return this;
  }

  /**
   * Apply a soft cap to the total server reply size in bytes across all streams combined. The
   * server measures the serialized reply it is building, including protocol overhead, so this is
   * not an exact payload-size guarantee: a single first available entry larger than the cap is
   * still returned, and the reply may be empty if no entries are available. Defaults to unlimited
   * when unset. Must be a positive integer (validated by the server).
   * @param maxSize cumulative soft reply-size cap in bytes
   * @return this
   * @since 8.0
   */
  public XReadParams maxSize(long maxSize) {
    this.maxSize = maxSize;
    return this;
  }

  public XReadParams block(int block) {
    this.block = block;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (count != null) {
      args.add(Keyword.COUNT).add(count);
    }
    if (maxCount != null) {
      args.add(Keyword.MAXCOUNT).add(maxCount);
    }
    if (maxSize != null) {
      args.add(Keyword.MAXSIZE).add(maxSize);
    }
    if (block != null) {
      args.add(Keyword.BLOCK).add(block).blocking();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XReadParams that = (XReadParams) o;
    return Objects.equals(count, that.count) && Objects.equals(maxCount, that.maxCount)
        && Objects.equals(maxSize, that.maxSize) && Objects.equals(block, that.block);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, maxCount, maxSize, block);
  }
}
