package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.ABSTTL;
import static redis.clients.jedis.Protocol.Keyword.FREQ;
import static redis.clients.jedis.Protocol.Keyword.IDLETIME;
import static redis.clients.jedis.Protocol.Keyword.REPLACE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

public class RestoreParams implements IParams {

  private boolean replace;

  private boolean absTtl;

  private Long idleTime;

  private Long frequency;

  public static RestoreParams restoreParams() {
    return new RestoreParams();
  }

  public RestoreParams replace() {
    this.replace = true;
    return this;
  }

  public RestoreParams absTtl() {
    this.absTtl = true;
    return this;
  }

  public RestoreParams idleTime(long idleTime) {
    this.idleTime = idleTime;
    return this;
  }

  public RestoreParams frequency(long frequency) {
    this.frequency = frequency;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (replace) {
      args.addObject(REPLACE.getRaw());
    }

    if (absTtl) {
      args.addObject(ABSTTL.getRaw());
    }

    if (idleTime != null) {
      args.addObject(IDLETIME.getRaw());
      args.addObject(Protocol.toByteArray(idleTime));
    }

    if (frequency != null) {
      args.addObject(FREQ.getRaw());
      args.addObject(Protocol.toByteArray(frequency));
    }
  }
}
