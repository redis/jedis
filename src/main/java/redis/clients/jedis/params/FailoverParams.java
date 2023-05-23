package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.FORCE;
import static redis.clients.jedis.Protocol.Keyword.TIMEOUT;
import static redis.clients.jedis.Protocol.Keyword.TO;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.HostAndPort;

public class FailoverParams implements IParams {

  private HostAndPort to;

  private boolean force;

  private Long timeout;

  public static FailoverParams failoverParams() {
    return new FailoverParams();
  }

  public FailoverParams to(String host, int port) {
    return to(new HostAndPort(host, port));
  }

  public FailoverParams to(HostAndPort to) {
    this.to = to;
    return this;
  }

  /**
   * Both TO ({@link FailoverParams#to(redis.clients.jedis.HostAndPort) or
   * {@link FailoverParams#to(java.lang.String, int)}) and
   * {@link FailoverParams#timeout(long) TIMEOUT} must be set in order for FORCE option.
   */
  public FailoverParams force() {
    this.force = true;
    return this;
  }

  public FailoverParams timeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (to != null) {
      args.add(TO).add(to.getHost()).add(to.getPort());
    }

    if (force) {
      if (to == null || timeout == null) {
        throw new IllegalArgumentException("FAILOVER with force option requires both a timeout and target HOST and IP.");
      }
      args.add(FORCE);
    }

    if (timeout != null) {
      args.add(TIMEOUT).add(timeout);
    }

  }
}
