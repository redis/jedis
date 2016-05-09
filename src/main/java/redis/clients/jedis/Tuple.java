package redis.clients.jedis;

import java.util.Arrays;

import redis.clients.util.SafeEncoder;

public class Tuple implements Comparable<Tuple> {
  private byte[] element;
  private Double score;

  public Tuple(String element, Double score) {
    super();
    this.element = SafeEncoder.encode(element);
    this.score = score;
  }

  public Tuple(byte[] element, Double score) {
    super();
    this.element = element;
    this.score = score;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result;
    if (null != element) {
      for (final byte b : element) {
        result = prime * result + b;
      }
    }
    long temp;
    temp = Double.doubleToLongBits(score);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Tuple other = (Tuple) obj;
    if (element == null) {
      if (other.element != null) return false;
    } else if (!Arrays.equals(element, other.element)) return false;
    return true;
  }

  public int compareTo(Tuple other) {
    if (this.score == other.getScore() || Arrays.equals(this.element, other.element)) return 0;
    else return this.score < other.getScore() ? -1 : 1;
  }
  
  public String getElement() {
    if (null != element) {
      return SafeEncoder.encode(element);
    } else {
      return null;
    }
  }

  public byte[] getBinaryElement() {
    return element;
  }

  public double getScore() {
    return score;
  }

  public String toString() {
    return '[' + Arrays.toString(element) + ',' + score + ']';
  }
}
