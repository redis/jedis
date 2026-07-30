package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class XReadGroupParams implements IParams {

  private Integer count = null;
  private Integer maxCount = null;
  private Long maxSize = null;
  private Integer block = null;
  private boolean noack = false;
  private Long claim = null;

  public static XReadGroupParams xReadGroupParams() {
    return new XReadGroupParams();
  }

  public XReadGroupParams count(int count) {
    this.count = count;
    return this;
  }

  /**
   * Limit the total number of entries returned across all streams combined. Unlike {@code COUNT},
   * which is a per-stream limit, {@code MAXCOUNT} caps the whole command reply; once the cap is
   * reached, remaining streams are skipped. Entries not emitted because of the cap are neither
   * delivered nor added to the consumer's pending entries. Defaults to unlimited when unset. Must
   * be a positive integer and, when {@code COUNT} is also set and positive, greater than or equal
   * to it (validated by the server).
   * @param maxCount cumulative entry cap across all streams
   * @return this
   * @since 8.0
   */
  public XReadGroupParams maxCount(int maxCount) {
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
  public XReadGroupParams maxSize(long maxSize) {
    this.maxSize = maxSize;
    return this;
  }

  public XReadGroupParams block(int block) {
    this.block = block;
    return this;
  }

  public XReadGroupParams noAck() {
    this.noack = true;
    return this;
  }

  public XReadGroupParams claim(long minIdleMillis) {
    this.claim = minIdleMillis;
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
    if (noack) {
      args.add(Keyword.NOACK);
    }
    if (claim != null) {
      args.add(Keyword.CLAIM).add(claim);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XReadGroupParams that = (XReadGroupParams) o;
    return noack == that.noack && Objects.equals(count, that.count)
        && Objects.equals(maxCount, that.maxCount) && Objects.equals(maxSize, that.maxSize)
        && Objects.equals(block, that.block) && Objects.equals(claim, that.claim);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, maxCount, maxSize, block, noack, claim);
  }
}
