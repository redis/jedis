package redis.clients.jedis.resps;

import java.util.List;
import java.util.Map;

/**
 * Result of a VSIM command, containing similar elements and optionally their similarity scores.
 */
public class VSimResult {

  private final List<String> elements;
  private final Map<String, Double> scores;
  private final boolean withScores;

  /**
   * Constructor for results without scores.
   * @param elements the list of similar elements
   */
  public VSimResult(List<String> elements) {
    this.elements = elements;
    this.scores = null;
    this.withScores = false;
  }

  /**
   * Constructor for results with scores.
   * @param elements the list of similar elements
   * @param scores the map of element names to their similarity scores
   */
  public VSimResult(List<String> elements, Map<String, Double> scores) {
    this.elements = elements;
    this.scores = scores;
    this.withScores = true;
  }

  /**
   * Get the list of similar elements.
   * @return list of element names
   */
  public List<String> getElements() {
    return elements;
  }

  /**
   * Get the similarity scores if available.
   * @return map of element names to similarity scores, or null if scores were not requested
   */
  public Map<String, Double> getScores() {
    return scores;
  }

  /**
   * Check if this result includes similarity scores.
   * @return true if scores are included, false otherwise
   */
  public boolean hasScores() {
    return withScores;
  }

  /**
   * Get the similarity score for a specific element.
   * @param element the element name
   * @return the similarity score, or null if scores are not available or element not found
   */
  public Double getScore(String element) {
    return scores != null ? scores.get(element) : null;
  }

  /**
   * Get the number of elements in the result.
   * @return the number of elements
   */
  public int size() {
    return elements != null ? elements.size() : 0;
  }

  /**
   * Check if the result is empty.
   * @return true if no elements were found, false otherwise
   */
  public boolean isEmpty() {
    return elements == null || elements.isEmpty();
  }

  @Override
  public String toString() {
    if (withScores && scores != null) {
      return "VSimResult{elements=" + elements + ", scores=" + scores + "}";
    } else {
      return "VSimResult{elements=" + elements + "}";
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VSimResult that = (VSimResult) o;

    if (withScores != that.withScores) return false;
    if (elements != null ? !elements.equals(that.elements) : that.elements != null) return false;
    return scores != null ? scores.equals(that.scores) : that.scores == null;
  }

  @Override
  public int hashCode() {
    int result = elements != null ? elements.hashCode() : 0;
    result = 31 * result + (scores != null ? scores.hashCode() : 0);
    result = 31 * result + (withScores ? 1 : 0);
    return result;
  }
}
