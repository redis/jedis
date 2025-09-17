package redis.clients.jedis.resps;

import redis.clients.jedis.annots.Experimental;

/**
 * Response object containing both similarity score and attributes for VSIM command when used with
 * WITHSCORES and WITHATTRIBS options.
 */
@Experimental
public class VSimScoreAttribs {

  private final Double score;
  private final String attributes;

  /**
   * Creates a new VSimScoreAttribs instance.
   * @param score the similarity score (0.0 to 1.0)
   * @param attributes the element attributes as JSON string, or null if no attributes
   */
  public VSimScoreAttribs(Double score, String attributes) {
    this.score = score;
    this.attributes = attributes;
  }

  /**
   * Gets the similarity score.
   * @return the similarity score between 0.0 and 1.0
   */
  public Double getScore() {
    return score;
  }

  /**
   * Gets the element attributes.
   * @return the attributes as JSON string, or null if no attributes are set
   */
  public String getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    return "VSimScoreAttribs{" + "score=" + score + ", attributes='" + attributes + '\'' + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VSimScoreAttribs that = (VSimScoreAttribs) o;

    if (score != null ? !score.equals(that.score) : that.score != null) return false;
    return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;
  }

  @Override
  public int hashCode() {
    int result = score != null ? score.hashCode() : 0;
    result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
    return result;
  }
}
