package redis.clients.jedis.params.set;

import redis.clients.jedis.params.Params;

public class SetParams extends Params {

  private SetParams() {}
  
  public static SetParams setParams() {
    return new SetParams();
  }
  
  public SetParams ex(int secondsToExpire) {
    addParam("ex", secondsToExpire);
    return this;
  }
  
  public SetParams px(long millisecondsToExpire) {
    addParam("px", millisecondsToExpire);
    return this;
  }
  
  public SetParams nx() {
    addParam("nx");
    return this;
  }
  
  public SetParams xx() {
    addParam("xx");
    return this;
  }
  
  public String getNxxx() {
    if(contains("nx")) {
      return "nx";
    } else if(contains("px")) {
      return "px";
    } else {
      return null;
    }
  }
  
  public String getExpx() {
    if(contains("ex")) {
      return "ex";
    } else if(contains("px")) {
      return "px";
    } else {
      return null;
    }
  }
  
}
