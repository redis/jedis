package redis.clients.jedis.params.set;

import static redis.clients.util.Preconditions.checkArgument;
import redis.clients.jedis.params.Params;

public class SetParams extends Params {

  private SetParams() {}
  
  public static SetParams setParams() {
    return new SetParams();
  }
  
  public SetParams ex(int secondsToExpire) {
    checkArgument(!contains("px"), "ex parameter could not be used with px parameter");
    
    addParam("ex", secondsToExpire);
    return this;
  }
  
  public SetParams px(long millisecondsToExpire) {
    checkArgument(!contains("ex"), "px parameter could not be used with ex parameter");
    
    addParam("px", millisecondsToExpire);
    return this;
  }
  
  public SetParams nx() {
    checkArgument(!contains("xx"), "nx parameter could not be used with xx parameter");
    
    addParam("nx");
    return this;
  }
  
  public SetParams xx() {
    checkArgument(!contains("nx"), "xx parameter could not be used with nx parameter");
    
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
