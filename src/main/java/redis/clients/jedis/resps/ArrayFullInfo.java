package redis.clients.jedis.resps;

import java.util.Map;

/**
 * This class holds information about an array returned by {@code ARINFO key FULL}. It extends
 * {@link ArrayInfo} with the additional per-slice aggregate fields reported only when the
 * {@code FULL} flag is present. The underlying {@link Map} accessible via {@link #getArrayInfo()}
 * contains every field returned by the server.
 */
public class ArrayFullInfo extends ArrayInfo {

  public static final String DENSE_SLICES = "dense-slices";
  public static final String SPARSE_SLICES = "sparse-slices";
  public static final String AVG_DENSE_SIZE = "avg-dense-size";
  public static final String AVG_DENSE_FILL = "avg-dense-fill";
  public static final String AVG_SPARSE_SIZE = "avg-sparse-size";

  private final Long denseSlices;
  private final Long sparseSlices;
  private final Long avgDenseSize;
  private final Long avgDenseFill;
  private final Long avgSparseSize;

  /**
   * @param map contains key-value pairs with array info (including the additional aggregate fields
   *          reported by {@code ARINFO key FULL})
   */
  public ArrayFullInfo(Map<String, Object> map) {
    super(map);
    denseSlices = (Long) map.get(DENSE_SLICES);
    sparseSlices = (Long) map.get(SPARSE_SLICES);
    avgDenseSize = (Long) map.get(AVG_DENSE_SIZE);
    avgDenseFill = (Long) map.get(AVG_DENSE_FILL);
    avgSparseSize = (Long) map.get(AVG_SPARSE_SIZE);
  }

  /**
   * @return the number of dense slices, or {@code null} if not reported
   */
  public Long getDenseSlices() {
    return denseSlices;
  }

  /**
   * @return the number of sparse slices, or {@code null} if not reported
   */
  public Long getSparseSlices() {
    return sparseSlices;
  }

  /**
   * @return the average size of dense slices, or {@code null} if not reported
   */
  public Long getAvgDenseSize() {
    return avgDenseSize;
  }

  /**
   * @return the average fill rate of dense slices, or {@code null} if not reported
   */
  public Long getAvgDenseFill() {
    return avgDenseFill;
  }

  /**
   * @return the average size of sparse slices, or {@code null} if not reported
   */
  public Long getAvgSparseSize() {
    return avgSparseSize;
  }
}
