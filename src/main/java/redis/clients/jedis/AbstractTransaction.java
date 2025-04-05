package redis.clients.jedis;

import java.io.Closeable;
import java.util.List;

public abstract class AbstractTransaction extends PipeliningBase implements Closeable {

  @Deprecated
  protected AbstractTransaction() {
    super(new CommandObjects());
  }

  protected AbstractTransaction(CommandObjects commandObjects) {
    super(commandObjects);
  }

  public abstract void multi();

  /**
   * Must be called before {@link AbstractTransaction#multi() MULTI}.
   */
  public abstract String watch(final String... keys);

  /**
   * Must be called before {@link AbstractTransaction#multi() MULTI}.
   */
  public abstract String watch(final byte[]... keys);

  public abstract String unwatch();

  @Override public abstract void close();

  public abstract List<Object> exec();

  public abstract String discard();

  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }

  public Response<Long> publish(String channel, String message) {
    return appendCommand(commandObjects.publish(channel, message));
  }

  public Response<Long> publish(byte[] channel, byte[] message) {
    return appendCommand(commandObjects.publish(channel, message));
  }
}
