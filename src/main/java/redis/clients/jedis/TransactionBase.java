package redis.clients.jedis;

import java.io.Closeable;
import java.util.List;

public abstract class TransactionBase extends PipeliningBase implements Closeable {

  protected TransactionBase() {
    super(new CommandObjects());
  }

  public abstract void multi();

  /**
   * Must be called before {@link TransactionBase#multi() MULTI}.
   */
  public abstract String watch(final String... keys);

  /**
   * Must be called before {@link TransactionBase#multi() MULTI}.
   */
  public abstract String watch(final byte[]... keys);

  public abstract String unwatch();

  @Override public abstract void close();

  public abstract List<Object> exec();

  public abstract String discard();

  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }
}
