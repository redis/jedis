package redis.clients.jedis.params;

import static org.junit.Assert.*;

import org.junit.Test;

public class HSetFParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    HSetFParams firstParam = new HSetFParams();
    HSetFParams secondParam = new HSetFParams();
    assertTrue(firstParam.equals(secondParam));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    HSetFParams firstParam = new HSetFParams();
    HSetFParams secondParam = new HSetFParams();
    assertEquals(firstParam.hashCode(), secondParam.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    HSetFParams firstParam = new HSetFParams();
    HSetFParams secondParam = new HSetFParams().keepTtl();
    assertFalse(firstParam.equals(secondParam));
  }

  @Test
  public void checkHashCodeVariousParams() {
    HSetFParams firstParam = new HSetFParams();
    HSetFParams secondParam = new HSetFParams().keepTtl();
    assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    HSetFParams firstParam = new HSetFParams();
    HSetFParams secondParam = null;
    assertFalse(firstParam.equals(secondParam));
  }

  // TODO: test for arguments ??
}
