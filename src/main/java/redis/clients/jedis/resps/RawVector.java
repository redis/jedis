package redis.clients.jedis.resps;

import redis.clients.jedis.annots.Experimental;

/**
 * Result of a VEMB RAW command, containing raw vector data and metadata. For regular VEMB commands
 * (without RAW), use List&lt;Double&gt; directly.
 */
@Experimental
public class RawVector {

  private final String quantizationType;
  private final byte[] rawData;
  private final Double norm;
  private final Double quantizationRange;

  /**
   * Constructor for RAW VEMB results.
   * @param quantizationType the quantization type (fp32, bin, or q8)
   * @param rawData the raw vector data blob
   * @param norm the L2 norm of the vector before normalization
   * @param quantizationRange the quantization range (only for q8, null otherwise)
   */
  public RawVector(String quantizationType, byte[] rawData, Double norm, Double quantizationRange) {
    this.quantizationType = quantizationType;
    this.rawData = rawData;
    this.norm = norm;
    this.quantizationRange = quantizationRange;
  }

  /**
   * Get the quantization type.
   * @return quantization type string (fp32, bin, or q8)
   */
  public String getQuantizationType() {
    return quantizationType;
  }

  /**
   * Get the raw vector data.
   * @return raw data blob
   */
  public byte[] getRawData() {
    return rawData;
  }

  /**
   * Get the L2 norm.
   * @return L2 norm value
   */
  public Double getNorm() {
    return norm;
  }

  /**
   * Get the quantization range (for q8 quantization).
   * @return quantization range, or null if not q8 quantization
   */
  public Double getQuantizationRange() {
    return quantizationRange;
  }

  @Override
  public String toString() {
    return "RawVector{quantizationType='" + quantizationType + "', norm=" + norm
        + ", quantizationRange=" + quantizationRange + ", rawDataLength="
        + (rawData != null ? rawData.length : 0) + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RawVector that = (RawVector) o;

    if (quantizationType != null ? !quantizationType.equals(that.quantizationType)
        : that.quantizationType != null)
      return false;
    if (rawData != null ? !java.util.Arrays.equals(rawData, that.rawData) : that.rawData != null)
      return false;
    if (norm != null ? !norm.equals(that.norm) : that.norm != null) return false;
    return quantizationRange != null ? quantizationRange.equals(that.quantizationRange)
        : that.quantizationRange == null;
  }

  @Override
  public int hashCode() {
    int result = quantizationType != null ? quantizationType.hashCode() : 0;
    result = 31 * result + (rawData != null ? java.util.Arrays.hashCode(rawData) : 0);
    result = 31 * result + (norm != null ? norm.hashCode() : 0);
    result = 31 * result + (quantizationRange != null ? quantizationRange.hashCode() : 0);
    return result;
  }
}
