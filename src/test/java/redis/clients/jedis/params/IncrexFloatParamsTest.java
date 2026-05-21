package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IncrexFloatParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    IncrexFloatParams first = getDefaultValue();
    IncrexFloatParams second = getDefaultValue();
    assertTrue(first.equals(second));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    IncrexFloatParams first = getDefaultValue();
    IncrexFloatParams second = getDefaultValue();
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    IncrexFloatParams first = new IncrexFloatParams().lbound(0.0).ubound(100.5).ex(60);
    IncrexFloatParams second = new IncrexFloatParams().lbound(0.0).ubound(200.5).px(5000);
    assertFalse(first.equals(second));
  }

  @Test
  public void checkHashCodeVariousParams() {
    IncrexFloatParams first = new IncrexFloatParams().lbound(0.0).ubound(100.5).ex(60);
    IncrexFloatParams second = new IncrexFloatParams().lbound(0.0).ubound(200.5).px(5000);
    assertNotEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    IncrexFloatParams first = getDefaultValue();
    assertFalse(first.equals(null));
  }

  @Test
  public void checkEqualsAcrossSubtypes() {
    // An IncrexParams and an IncrexFloatParams with otherwise-equal options must NOT be equal.
    // This guards against the base-class equals comparing only shared state.
    assertNotEquals(new IncrexParams().ex(60), new IncrexFloatParams().ex(60));
  }

  @Test
  public void checkSaturate() {
    assertNotEquals(new IncrexFloatParams().saturate(), new IncrexFloatParams());
  }

  @Test
  public void checkEnx() {
    assertNotEquals(new IncrexFloatParams().ex(60).enx(), new IncrexFloatParams().ex(60));
  }

  @Test
  public void checkPersist() {
    assertNotEquals(new IncrexFloatParams().persist(), new IncrexFloatParams());
  }

  private IncrexFloatParams getDefaultValue() {
    return new IncrexFloatParams();
  }
}
