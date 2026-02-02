package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XCfgSetParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    XCfgSetParams firstParam = getDefaultValue();
    XCfgSetParams secondParam = getDefaultValue();
    assertTrue(firstParam.equals(secondParam));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    XCfgSetParams firstParam = getDefaultValue();
    XCfgSetParams secondParam = getDefaultValue();
    assertEquals(firstParam.hashCode(), secondParam.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    XCfgSetParams firstParam = getDefaultValue();
    firstParam.idmpDuration(100);
    XCfgSetParams secondParam = getDefaultValue();
    secondParam.idmpDuration(200);
    assertFalse(firstParam.equals(secondParam));
  }

  @Test
  public void checkHashCodeVariousParams() {
    XCfgSetParams firstParam = getDefaultValue();
    firstParam.idmpDuration(100);
    XCfgSetParams secondParam = getDefaultValue();
    secondParam.idmpDuration(200);
    assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    XCfgSetParams firstParam = getDefaultValue();
    XCfgSetParams secondParam = null;
    assertFalse(firstParam.equals(secondParam));
  }

  @Test
  public void testIdmpDurationValidRange() {
    XCfgSetParams params = new XCfgSetParams();

    // Test minimum valid value
    assertDoesNotThrow(() -> params.idmpDuration(1));

    // Test maximum valid value
    assertDoesNotThrow(() -> params.idmpDuration(86400));

    // Test value in range
    assertDoesNotThrow(() -> params.idmpDuration(100));
  }

  @Test
  public void testIdmpDurationBelowMinimum() {
    XCfgSetParams params = new XCfgSetParams();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> params.idmpDuration(0));
    assertEquals("IDMP-DURATION must be between 1 and 86400 seconds", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> params.idmpDuration(-1));
    assertEquals("IDMP-DURATION must be between 1 and 86400 seconds", exception.getMessage());
  }

  @Test
  public void testIdmpDurationAboveMaximum() {
    XCfgSetParams params = new XCfgSetParams();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> params.idmpDuration(86401));
    assertEquals("IDMP-DURATION must be between 1 and 86400 seconds", exception.getMessage());
  }

  @Test
  public void testIdmpMaxsizeValidRange() {
    XCfgSetParams params = new XCfgSetParams();

    // Test minimum valid value
    assertDoesNotThrow(() -> params.idmpMaxsize(1));

    // Test maximum valid value
    assertDoesNotThrow(() -> params.idmpMaxsize(10000));

    // Test value in range
    assertDoesNotThrow(() -> params.idmpMaxsize(100));
  }

  @Test
  public void testIdmpMaxsizeBelowMinimum() {
    XCfgSetParams params = new XCfgSetParams();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> params.idmpMaxsize(0));
    assertEquals("IDMP-MAXSIZE must be between 1 and 10000", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> params.idmpMaxsize(-1));
    assertEquals("IDMP-MAXSIZE must be between 1 and 10000", exception.getMessage());
  }

  @Test
  public void testIdmpMaxsizeAboveMaximum() {
    XCfgSetParams params = new XCfgSetParams();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> params.idmpMaxsize(10001));
    assertEquals("IDMP-MAXSIZE must be between 1 and 10000", exception.getMessage());
  }

  @Test
  public void testBothParametersValid() {
    XCfgSetParams params = new XCfgSetParams();

    assertDoesNotThrow(() -> params.idmpDuration(1000).idmpMaxsize(500));
  }

  private XCfgSetParams getDefaultValue() {
    return new XCfgSetParams();
  }
}
