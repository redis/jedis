package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.args.StreamDeletionPolicy;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;
import java.util.Objects;

public class XAddParams implements IParams {

  private Rawable id;

  private Long maxLen;

  private boolean approximateTrimming;

  private boolean exactTrimming;

  private boolean nomkstream;

  private String minId;

  private Long limit;

  private StreamDeletionPolicy trimMode;

  private byte[] producerId;

  private byte[] idempotentId;

  private boolean idmpAuto;

  public static XAddParams xAddParams() {
    return new XAddParams();
  }

  public XAddParams noMkStream() {
    this.nomkstream = true;
    return this;
  }

  public XAddParams id(byte[] id) {
    this.id = RawableFactory.from(id);
    return this;
  }

  public XAddParams id(String id) {
    this.id = RawableFactory.from(id);
    return this;
  }

  public XAddParams id(StreamEntryID id) {
    return id(id.toString());
  }

  public XAddParams id(long time, long sequence) {
    return id(time + "-" + sequence);
  }

  public XAddParams id(long time) {
    return id(time + "-*");
  }

  public XAddParams maxLen(long maxLen) {
    this.maxLen = maxLen;
    return this;
  }

  public XAddParams minId(String minId) {
    this.minId = minId;
    return this;
  }

  public XAddParams approximateTrimming() {
    this.approximateTrimming = true;
    return this;
  }

  public XAddParams exactTrimming() {
    this.exactTrimming = true;
    return this;
  }

  public XAddParams limit(long limit) {
    this.limit = limit;
    return this;
  }

  /**
   * When trimming, defines desired behaviour for handling consumer group references.
   * see {@link StreamDeletionPolicy} for details.
   *
   * @return XAddParams
   */
  public XAddParams trimmingMode(StreamDeletionPolicy trimMode) {
    this.trimMode = trimMode;
    return this;
  }

  /**
   * Enable idempotent producer mode with automatic idempotent ID generation.
   * Redis will calculate an idempotent ID based on the message content.
   *
   * @param producerId unique producer identifier (binary)
   * @return XAddParams
   */
  public XAddParams idmpAuto(byte[] producerId) {
    this.producerId = producerId;
    this.idmpAuto = true;
    this.idempotentId = null;
    return this;
  }

  /**
   * Enable idempotent producer mode with automatic idempotent ID generation.
   * Redis will calculate an idempotent ID based on the message content.
   *
   * @param producerId unique producer identifier (string)
   * @return XAddParams
   */
  public XAddParams idmpAuto(String producerId) {
    return idmpAuto(SafeEncoder.encode(producerId));
  }

  /**
   * Enable idempotent producer mode with explicit idempotent ID.
   * The caller provides both producer ID and idempotent ID.
   *
   * @param producerId unique producer identifier (binary)
   * @param idempotentId unique idempotent identifier for this message (binary)
   * @return XAddParams
   */
  public XAddParams idmp(byte[] producerId, byte[] idempotentId) {
    this.producerId = producerId;
    this.idempotentId = idempotentId;
    this.idmpAuto = false;
    return this;
  }

  /**
   * Enable idempotent producer mode with explicit idempotent ID.
   * The caller provides both producer ID and idempotent ID.
   *
   * @param producerId unique producer identifier (string)
   * @param idempotentId unique idempotent identifier for this message (string)
   * @return XAddParams
   */
  public XAddParams idmp(String producerId, String idempotentId) {
    return idmp(SafeEncoder.encode(producerId), SafeEncoder.encode(idempotentId));
  }

  @Override
  public void addParams(CommandArguments args) {

    if (nomkstream) {
      args.add(Keyword.NOMKSTREAM);
    }

    if (trimMode != null) {
      args.add(trimMode);
    }

    if (producerId != null) {
      if (idmpAuto) {
        args.add(Keyword.IDMPAUTO).add(producerId);
      } else if (idempotentId != null) {
        args.add(Keyword.IDMP).add(producerId).add(idempotentId);
      }
    }

    if (maxLen != null) {
      args.add(Keyword.MAXLEN);

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(maxLen);
    } else if (minId != null) {
      args.add(Keyword.MINID);

      if (approximateTrimming) {
        args.add(Protocol.BYTES_TILDE);
      } else if (exactTrimming) {
        args.add(Protocol.BYTES_EQUAL);
      }

      args.add(minId);
    }

    if (limit != null) {
      args.add(Keyword.LIMIT).add(limit);
    }

    args.add(id != null ? id : StreamEntryID.NEW_ENTRY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    XAddParams that = (XAddParams) o;
    return approximateTrimming == that.approximateTrimming && exactTrimming == that.exactTrimming
        && nomkstream == that.nomkstream && idmpAuto == that.idmpAuto
        && Objects.equals(id, that.id) && Objects.equals(maxLen, that.maxLen)
        && Objects.equals(minId, that.minId) && Objects.equals(limit, that.limit)
        && trimMode == that.trimMode && Objects.deepEquals(producerId, that.producerId)
        && Objects.deepEquals(idempotentId, that.idempotentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, maxLen, approximateTrimming, exactTrimming, nomkstream, minId, limit,
        trimMode, Arrays.hashCode(producerId), Arrays.hashCode(idempotentId), idmpAuto);
  }
}
