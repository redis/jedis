package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.util.SafeEncoder;

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
   * WARNING: FORCE option can be used only if both TO and TIMEOUT options are specified.
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
      args.add(Keyword.TO.getRaw());
      args.add(SafeEncoder.encode(to.getHost()));
      args.add(Protocol.toByteArray(to.getPort()));

    }

    if (force) {
      if (to == null || timeout == null) {
        throw new IllegalStateException("ERR FAILOVER with force option requires both a timeout and target HOST and IP.");
      }
      args.add(Keyword.FORCE.getRaw());
    }

    if (timeout != null) {
      args.add(Keyword.TIMEOUT.getRaw());
      args.add(Protocol.toByteArray(timeout));
    }

  }
}
