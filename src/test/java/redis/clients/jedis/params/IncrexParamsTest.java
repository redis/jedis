package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IncrexParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    IncrexParams first = getDefaultValue();
    IncrexParams second = getDefaultValue();
    assertTrue(first.equals(second));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    IncrexParams first = getDefaultValue();
    IncrexParams second = getDefaultValue();
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    IncrexParams first = new IncrexParams().lbound(0).ubound(100).ex(60);
    IncrexParams second = new IncrexParams().lbound(0).ubound(200).px(5000);
    assertFalse(first.equals(second));
  }

  @Test
  public void checkHashCodeVariousParams() {
    IncrexParams first = new IncrexParams().lbound(0).ubound(100).ex(60);
    IncrexParams second = new IncrexParams().lbound(0).ubound(200).px(5000);
    assertNotEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    IncrexParams first = getDefaultValue();
    assertFalse(first.equals(null));
  }

  @Test
  public void checkOverflowModes() {
    assertNotEquals(new IncrexParams().overflow(IncrexParams.Overflow.SAT),
      new IncrexParams().overflow(IncrexParams.Overflow.REJECT));
  }

  @Test
  public void checkEnx() {
    assertNotEquals(new IncrexParams().ex(60).enx(), new IncrexParams().ex(60));
  }

  @Test
  public void checkPersist() {
    assertNotEquals(new IncrexParams().persist(), new IncrexParams());
  }

  private IncrexParams getDefaultValue() {
    return new IncrexParams();
  }
}
