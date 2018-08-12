package redis.clients.jedis;

import redis.clients.util.ByteArrayComparator;
import redis.clients.util.SafeEncoder;

import java.util.Arrays;

public class Tuple implements Comparable<Tuple> {
  private byte[] element;
  private Double score;

  public Tuple(String element, Double score) {
    this(SafeEncoder.encode(element), score);
  }

  public Tuple(byte[] element, Double score) {
    super();
    this.element = element;
    this.score = score;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result;
    if (null != element) {
      for (final byte b : element) {
        result = prime * result + b;
      }
    }
    long temp = Double.doubleToLongBits(score);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Tuple other = (Tuple) obj;
    if (!Arrays.equals(element, other.element)) return false;
    return score.equals(other.score);
  }

  @Override
  public int compareTo(Tuple other) {
    return compare(this, other);
  }

  public static int compare(Tuple t1, Tuple t2) {
    int compScore = Double.compare(t1.score, t2.score);
    if(compScore != 0) return compScore;

    return ByteArrayComparator.compare(t1.element, t2.element);
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

  @Override
  public String toString() {
    return '[' + SafeEncoder.encode(element) + ',' + score + ']';
  }
}
