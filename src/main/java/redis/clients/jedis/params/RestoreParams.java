package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

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
      args.add(Keyword.REPLACE);
    }

    if (absTtl) {
      args.add(Keyword.ABSTTL);
    }

    if (idleTime != null) {
      args.add(Keyword.IDLETIME).add(idleTime);
    }

    if (frequency != null) {
      args.add(Keyword.FREQ).add(frequency);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RestoreParams that = (RestoreParams) o;
    return replace == that.replace && absTtl == that.absTtl && Objects.equals(idleTime, that.idleTime) && Objects.equals(frequency, that.frequency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(replace, absTtl, idleTime, frequency);
  }
}
