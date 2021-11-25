package redis.clients.jedis.resps;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.util.SafeEncoder;

public class Slowlog {

  private final long id;
  private final long timeStamp;
  private final long executionTime;
  private final List<String> args;
  private HostAndPort clientIpPort;
  private String clientName;

  private static final String COMMA = ",";

  @SuppressWarnings("unchecked")
  private Slowlog(List<Object> properties) {
    super();
    this.id = (Long) properties.get(0);
    this.timeStamp = (Long) properties.get(1);
    this.executionTime = (Long) properties.get(2);

    List<byte[]> bargs = (List<byte[]>) properties.get(3);
    this.args = new ArrayList<>(bargs.size());

    for (byte[] barg : bargs) {
      this.args.add(SafeEncoder.encode(barg));
    }
    if (properties.size() == 4) return;

    this.clientIpPort = HostAndPort.from(SafeEncoder.encode((byte[]) properties.get(4)));
    this.clientName = SafeEncoder.encode((byte[]) properties.get(5));
  }

  @SuppressWarnings("unchecked")
  public static List<Slowlog> from(List<Object> nestedMultiBulkReply) {
    List<Slowlog> logs = new ArrayList<>(nestedMultiBulkReply.size());
    for (Object obj : nestedMultiBulkReply) {
      List<Object> properties = (List<Object>) obj;
      logs.add(new Slowlog(properties));
    }

    return logs;
  }

  public long getId() {
    return id;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public long getExecutionTime() {
    return executionTime;
  }

  public List<String> getArgs() {
    return args;
  }

  public HostAndPort getClientIpPort() {
    return clientIpPort;
  }

  public String getClientName() {
    return clientName;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(id).append(COMMA).append(timeStamp).append(COMMA)
        .append(executionTime).append(COMMA).append(args).toString();
  }
}
