package redis.clients.jedis.tests.params.set;

import static redis.clients.jedis.params.set.SetParams.setParams;

import org.junit.Test;

public class SetParamsTest {

  @Test(expected = IllegalArgumentException.class)
  public void settingExWithPxShouldNotBeAllowed() {
    setParams().ex(10).px(10000);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void settingPxWithExShouldNotBeAllowed() {
    setParams().px(10000).ex(10);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void settingNxWithXxShouldNotBeAllowed() {
    setParams().nx().xx();
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void settingXxWithNxShouldNotBeAllowed() {
    setParams().xx().nx();
  }
  
}
