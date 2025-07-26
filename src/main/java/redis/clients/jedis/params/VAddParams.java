package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.Experimental;

/**
 * Parameters for the VADD command.
 */
@Experimental
public class VAddParams implements IParams {

  private boolean cas;
  private QuantizationType quantization;
  private Integer ef;
  private String attributes;
  private Integer m;

  public enum QuantizationType {
    NOQUANT, Q8, BIN
  }

  public VAddParams() {
  }

  /**
   * Performs the operation partially using threads, in a check-and-set style. The neighbor
   * candidates collection, which is slow, is performed in the background, while the command is
   * executed in the main thread.
   * @return VAddParams
   */
  public VAddParams cas() {
    this.cas = true;
    return this;
  }

  /**
   * Forces the vector to be created without int8 quantization.
   * @return VAddParams
   */
  public VAddParams noQuant() {
    this.quantization = QuantizationType.NOQUANT;
    return this;
  }

  /**
   * Forces the vector to use signed 8-bit quantization. This is the default.
   * @return VAddParams
   */
  public VAddParams q8() {
    this.quantization = QuantizationType.Q8;
    return this;
  }

  /**
   * Forces the vector to use binary quantization instead of int8. This is much faster and uses less
   * memory, but impacts the recall quality.
   * @return VAddParams
   */
  public VAddParams bin() {
    this.quantization = QuantizationType.BIN;
    return this;
  }

  /**
   * Plays a role in the effort made to find good candidates when connecting the new node to the
   * existing Hierarchical Navigable Small World (HNSW) graph. The default is 200. Using a larger
   * value may help in achieving a better recall.
   * @param buildExplorationFactor the exploration factor
   * @return VAddParams
   */
  public VAddParams ef(int buildExplorationFactor) {
    this.ef = buildExplorationFactor;
    return this;
  }

  /**
   * Associates attributes in the form of a JavaScript object to the newly created entry or updates
   * the attributes (if they already exist).
   * @param attributes the attributes as a JSON string
   * @return VAddParams
   */
  public VAddParams setAttr(String attributes) {
    this.attributes = attributes;
    return this;
  }

  /**
   * The maximum number of connections that each node of the graph will have with other nodes. The
   * default is 16. More connections means more memory, but provides for more efficient graph
   * exploration.
   * @param numLinks the maximum number of connections
   * @return VAddParams
   */
  public VAddParams m(int numLinks) {
    this.m = numLinks;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (cas) {
      args.add(Protocol.Keyword.CAS);
    }

    if (quantization != null) {
      switch (quantization) {
        case NOQUANT:
          args.add(Protocol.Keyword.NOQUANT);
          break;
        case Q8:
          args.add(Protocol.Keyword.Q8);
          break;
        case BIN:
          args.add(Protocol.Keyword.BIN);
          break;
      }
    }

    if (ef != null) {
      args.add(Protocol.Keyword.EF).add(ef);
    }

    if (attributes != null) {
      args.add(Protocol.Keyword.SETATTR).add(attributes);
    }

    if (m != null) {
      args.add(Protocol.Keyword.M).add(m);
    }
  }
}
