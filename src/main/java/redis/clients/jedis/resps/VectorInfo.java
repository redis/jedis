package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;

import redis.clients.jedis.annots.Experimental;

/**
 * This class holds information about a vector set returned by the {@code VINFO} command. They can
 * be accessed via getters. There is also {@link VectorInfo#getVectorInfo()} method that returns a
 * generic {@link Map} in case where more info are returned from the server.
 */
@Experimental
public class VectorInfo implements Serializable {
  public static final String VECTOR_DIM = "vector-dim";
  public static final String TYPE = "quant-type";
  public static final String SIZE = "size";
  public static final String MAX_NODE_UID = "hnsw-max-node-uid";
  public static final String VSET_UID = "vset-uid";
  public static final String MAX_NODES = "hnsw-m";
  public static final String PROJECTION_INPUT_DIM = "projection-input-dim";
  public static final String ATTRIBUTES_COUNT = "attributes-count";
  public static final String MAX_LEVEL = "max-level";

  private final Long dimensionality;
  private final String type; // Will be converted to QuantizationType if needed
  private final Long size;
  private final Long maxNodeUid;
  private final Long vSetUid;
  private final Long maxNodes;
  private final Long projectionInputDim;
  private final Long attributesCount;
  private final Long maxLevel;
  private final Map<String, Object> vectorInfo;

  /**
   * @param map contains key-value pairs with vector set info
   */
  public VectorInfo(Map<String, Object> map) {
    vectorInfo = map;
    dimensionality = (Long) map.get(VECTOR_DIM);
    type = (String) map.get(TYPE);
    size = (Long) map.get(SIZE);
    maxNodeUid = (Long) map.get(MAX_NODE_UID);
    vSetUid = (Long) map.get(VSET_UID);
    maxNodes = (Long) map.get(MAX_NODES);
    projectionInputDim = (Long) map.get(PROJECTION_INPUT_DIM);
    attributesCount = (Long) map.get(ATTRIBUTES_COUNT);
    maxLevel = (Long) map.get(MAX_LEVEL);
  }

  public Long getDimensionality() {
    return dimensionality;
  }

  public String getType() {
    return type;
  }

  public Long getSize() {
    return size;
  }

  public Long getMaxNodeUid() {
    return maxNodeUid;
  }

  public Long getVSetUid() {
    return vSetUid;
  }

  public Long getMaxNodes() {
    return maxNodes;
  }

  public Long getProjectionInputDim() {
    return projectionInputDim;
  }

  public Long getAttributesCount() {
    return attributesCount;
  }

  public Long getMaxLevel() {
    return maxLevel;
  }

  public Map<String, Object> getVectorInfo() {
    return vectorInfo;
  }
}
