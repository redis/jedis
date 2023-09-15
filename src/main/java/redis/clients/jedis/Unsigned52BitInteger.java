package redis.clients.jedis;

import java.math.BigInteger;

public class Unsigned52BitInteger {
  private BigInteger value;

  public Unsigned52BitInteger(Long value) {
    if (value < 0 || value > 0x1FFFFFFFFFFFFFL) {
      throw new IllegalArgumentException("Value out of range for a 52-bit unsigned integer.");
    }
    this.value = new BigInteger(value.toString());
  }

  public BigInteger getValue() {
    return value;
  }

  @Override
  public String toString() {
    return this.value.toString();
  }

}
