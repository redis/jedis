package redis.clients.jedis.params;

import static org.junit.Assert.*;

import org.junit.Test;

public class HGetFParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    HGetFParams firstParam = new HGetFParams();
    HGetFParams secondParam = new HGetFParams();
    assertTrue(firstParam.equals(secondParam));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    HGetFParams firstParam = new HGetFParams();
    HGetFParams secondParam = new HGetFParams();
    assertEquals(firstParam.hashCode(), secondParam.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    HGetFParams firstParam = new HGetFParams();
    HGetFParams secondParam = new HGetFParams().persist();
    assertFalse(firstParam.equals(secondParam));
  }

  @Test
  public void checkHashCodeVariousParams() {
    HGetFParams firstParam = new HGetFParams();
    HGetFParams secondParam = new HGetFParams().persist();
    assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    HGetFParams firstParam = new HGetFParams();
    HGetFParams secondParam = null;
    assertFalse(firstParam.equals(secondParam));
  }

  // TODO: test for arguments ??
}
